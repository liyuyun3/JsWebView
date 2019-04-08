package com.github.lzyzsd.jsbridge;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.net.http.SslError;
import android.webkit.SslErrorHandler;
import android.webkit.WebView;
import android.webkit.WebViewClient;


import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;

/**
 * Created by bruce on 10/28/15.
 */
public class BridgeWebViewClient extends WebViewClient {
    private BridgeWebView webView;
    private onUrlListener mOnUrlListener;

    /**
     * url加载状态Map
     * 用于解决Android 4.4版本中，onPageFinished() 被调用2次的问题
     */
    private HashMap<String, Boolean> urlStateMap;

    public BridgeWebViewClient(BridgeWebView webView) {
        this.webView = webView;
        urlStateMap = new HashMap<>();
    }

    @Override
    public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
        //handler.cancel(); 默认的处理方式，WebView变成空白页
        //接受证书
        handler.proceed();
        //handleMessage(Message msg); 其他处理
    }

    public void setOnUrlListener(onUrlListener mUrlListener) {
        this.mOnUrlListener = mUrlListener;
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        try {
            url = URLDecoder.decode(url, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        if (url.startsWith(BridgeUtil.YY_RETURN_DATA)) { // 如果是返回数据
            webView.handlerReturnData(url);
            return true;
        } else if (url.startsWith(BridgeUtil.YY_OVERRIDE_SCHEMA)) { //
            webView.flushMessageQueue();
            return true;
        } else if (url.startsWith(BridgeUtil.YY_TEL_SCHEMA)) {
            if (webView.getContext() instanceof Activity) {
                Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse(url));
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                webView.getContext().startActivity(intent);
            }
            return true;
        } else {
            return super.shouldOverrideUrlLoading(view, url);
        }
    }

    @Override
    public void onPageStarted(WebView view, String url, Bitmap favicon) {
        super.onPageStarted(view, url, favicon);

        if (urlStateMap != null) {
            urlStateMap.put(url, false);
        }
    }

    @Override
    public void onPageFinished(WebView view, String url) {
        super.onPageFinished(view, url);

        if (BridgeWebView.TO_LOAD_JS != null) {
            BridgeUtil.webViewLoadLocalJs(view, BridgeWebView.TO_LOAD_JS);
            if (urlStateMap != null && urlStateMap.containsKey(url) && !urlStateMap.get(url)) {
                urlStateMap.put(url, true);
                if (mOnUrlListener != null) {
                    mOnUrlListener.showLastUrl(url);
                }
            }
        }

        //
        if (webView.getStartupMessage() != null) {
            for (Message m : webView.getStartupMessage()) {
                webView.dispatchMessage(m);
            }
            webView.setStartupMessage(null);
        }
    }

    @Override
    public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
        super.onReceivedError(view, errorCode, description, failingUrl);
    }

    public void destroy() {
        if (urlStateMap != null) {
            urlStateMap.clear();
            urlStateMap = null;
        }
    }

    public interface onUrlListener {
        public void showLastUrl(String url);
    }
}