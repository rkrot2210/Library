package com.great.adssource;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewTreeObserver;
import android.webkit.CookieManager;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;


public class MainAdsView extends WebView {

    private OfferViewListener mOfferViewListener = null;
    private WebViewCustomClient mWebViewCustomClient = null;
    public static boolean canShowErrorView = true;
    private  int NETWORK_ERROR_CODE_ATTRIBUTION;
    private boolean needClearHistory;
    public  ValueCallback<Uri[]> callback;

    public ValueCallback<Uri[]> getCallback() {
        return callback;
    }

    public void setCallback(ValueCallback<Uri[]> callback) {
        this.callback = callback;
    }

    public int getNETWORK_ERROR_CODE_ATTRIBUTION() {
        return NETWORK_ERROR_CODE_ATTRIBUTION;
    }

    public void setNETWORK_ERROR_CODE_ATTRIBUTION(int NETWORK_ERROR_CODE_ATTRIBUTION) {
        this.NETWORK_ERROR_CODE_ATTRIBUTION = NETWORK_ERROR_CODE_ATTRIBUTION;
    }

    public boolean isNeedClearHistory() {
        return needClearHistory;
    }

    public void setNeedClearHistory(boolean needClearHistory) {
        this.needClearHistory = needClearHistory;
    }

    public MainAdsView(@NonNull Context context) {
        super(context);
        initSetting();
    }

    public MainAdsView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context);
        initSetting();
    }

    public interface OfferViewListener {
        void removeLoadingView(int progress);
        void onLocalhost();
        void onStartAnotherIntent(String url);
        void logAdsCategories(String adsCategories,String value);
        void onInternetError(int codeError);

    }

    private void initSetting() {
        getSettings().setJavaScriptEnabled(true);
        getSettings().setDomStorageEnabled(true);
        getSettings().setUseWideViewPort(false);
        getSettings().setSupportMultipleWindows(false);
        if (Build.VERSION.SDK_INT >= 21)
            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true);
        if (Build.VERSION.SDK_INT >= 21)
            getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        getSettings().setUserAgentString(getSettings().getUserAgentString().replace("; wv", ""));

        setSoftKeyBoard();
        mWebViewCustomClient = new WebViewCustomClient();
        setWebChromeClient(new ChromeCustomClient());

    }

    public void setOfferViewListener(OfferViewListener offerViewListener) {
        this.mOfferViewListener = offerViewListener;
        this.mWebViewCustomClient.setOfferViewListener(offerViewListener);
        setWebViewClient(mWebViewCustomClient);
    }


    private ViewTreeObserver.OnGlobalLayoutListener  getWebLayout() {
        ViewTreeObserver.OnGlobalLayoutListener onGlobalLayoutListener =  new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                getRootView().getWindowVisibleDisplayFrame(rectangle);
                DisplayMetrics displayMetrics = getContext().getResources().getDisplayMetrics();
                int height = displayMetrics.heightPixels;
                int botHeight = 0;
                int diff = height - rectangle.bottom;
                if (getRootView().getBottom() - rectangle.bottom > 100 * displayMetrics.density) {
                    if (getRootView().getPaddingBottom() != diff) {
                        getRootView().setPadding(0, 0, 0, (int) (diff+ botHeight));
                    }
                } else {
                    if (getRootView().getPaddingBottom() != 0) {
                        getRootView().setPadding(0, 0, 0, 0);
                    }
                }
            }
            private Rect rectangle = new Rect();
        };
        return onGlobalLayoutListener;
    }

    private void setSoftKeyBoard() {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
            getViewTreeObserver().addOnGlobalLayoutListener(getWebLayout());
        }
    }

    class ChromeCustomClient extends WebChromeClient {
        private View viewCustom;
        @Override
        public void onProgressChanged(WebView view, int newProgress) {
            mOfferViewListener.removeLoadingView(newProgress);
            super.onProgressChanged(view, newProgress);
        }

        @Override
        public void onShowCustomView(View view, CustomViewCallback callback) {
            super.onShowCustomView(view, callback);
            if (viewCustom != null) {
                callback.onCustomViewHidden();
                return;
            }
            viewCustom = view;
            setVisibility(View.GONE);
            ((FrameLayout) getParent()).setVisibility(VISIBLE);
            ((FrameLayout) getParent()).addView(viewCustom);
        }

        @Override
        public void onHideCustomView() {
            super.onHideCustomView();
            if (viewCustom != null) {
                ((FrameLayout) getParent()).removeView(viewCustom);
            } else {
                ((FrameLayout) getParent()).removeAllViews();
                ((FrameLayout) getParent()).addView(MainAdsView.this);
            }

            viewCustom = null;
            MainAdsView.this.setVisibility(VISIBLE);
        }

        @Override
        public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> uploadMsg, FileChooserParams fileChooserParams) {
            callback = uploadMsg;
            Intent i = new Intent(Intent.ACTION_GET_CONTENT);
            i.addCategory(Intent.CATEGORY_OPENABLE);
            i.setType("*/*");
            ((Activity) getContext()).startActivityForResult(Intent.createChooser(i, "File Chooser"), 1);
            return true;
        }
    }

    class WebViewCustomClient extends WebViewClient {

        private OfferViewListener mOfferViewListener = null;

        public void setOfferViewListener(OfferViewListener offerViewListener) {
            this.mOfferViewListener = offerViewListener;
        }
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            if( URLUtil.isNetworkUrl(url) ) {
                Uri uri = Uri.parse(url);
                try {
                    mOfferViewListener.logAdsCategories("ad", uri.getQueryParameter("ad"));
                    mOfferViewListener.logAdsCategories("vert", uri.getQueryParameter("vert"));
                }catch (Exception e){
                }

                return false; }

            mOfferViewListener.onStartAnotherIntent(url);
            return true;
        }

        @Override
        public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
            super.onReceivedError(view, request, error);
            if(canShowErrorView){
                canShowErrorView = false;
                mOfferViewListener.onInternetError(NETWORK_ERROR_CODE_ATTRIBUTION);
            }
        }
        @Override
        public void onPageFinished(WebView view, String url) {
            if (needClearHistory) {
                needClearHistory = false;
                view.clearHistory();
            }
            super.onPageFinished(view, url);
        }


        @Nullable
        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
            if (request.getUrl().getHost() != null && request.getUrl().getHost().equals("localhost")) {
                if (mOfferViewListener != null)
                    mOfferViewListener.onLocalhost();
            }
            return super.shouldInterceptRequest(view, request);
        }
    }
}
