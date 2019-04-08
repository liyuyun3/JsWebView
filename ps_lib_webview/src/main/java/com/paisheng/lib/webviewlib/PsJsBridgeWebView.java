package com.paisheng.lib.webviewlib;

import android.content.Context;
import android.util.AttributeSet;
import android.webkit.WebViewClient;

import com.github.lzyzsd.jsbridge.BridgeWebView;
import com.github.lzyzsd.jsbridge.BridgeWebViewClient;
import com.github.lzyzsd.jsbridge.CallBackFunction;

/**
 * <br> ClassName:   PsJsBridgeWebView
 * <br> Description: jsBridge处理
 * <br>
 * <br> @author:      谢文良
 * <br> Date:        2017/10/27 10:06
 */

public class PsJsBridgeWebView extends BridgeWebView {
    private static final String LOADING_PAGE_FINISH = "WebonResume";
    private static final String PAGE_ON_RESUME = "WebonResumeHome";
    private static final String PAGE_ON_PAUSE = "WebonPause";
    private static final String PAGE_ON_DESTROY = "WebonDestroy";
    private boolean mIsLoadFinished;

    public PsJsBridgeWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initJSBridge();
    }

    public PsJsBridgeWebView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initJSBridge();
    }

    public PsJsBridgeWebView(Context context) {
        super(context);
        initJSBridge();
    }

    @Override
    public void setWebViewClient(WebViewClient client) {
        super.setWebViewClient(client);
        initJSBridge();
    }

    private void initJSBridge() {
        if (getBridgeWebViewClient() != null) {
            getBridgeWebViewClient().setOnUrlListener(new BridgeWebViewClient.onUrlListener() {
                @Override
                public void showLastUrl(String url) {
                    callHandler(LOADING_PAGE_FINISH, "", null);
                    mIsLoadFinished = true;
                }
            });
        }
    }

    @Override
    public void onResume() {
        // super.onResume()必须先于callHandler执行
        super.onResume();
        if (mIsLoadFinished) {
            callHandler(PAGE_ON_RESUME, "", null);
        }
    }

    @Override
    public void onPause() {
        callHandler(PAGE_ON_PAUSE, "", new CallBackFunction() {
            @Override
            public void onCallBack(String data) {

            }
        });
        //super.onPause()必须后于callHandler执行
        super.onPause();
    }

    @Override
    public void destroy() {
        callHandler(PAGE_ON_DESTROY, "", null);
        super.destroy();
    }
}
