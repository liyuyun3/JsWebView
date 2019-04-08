package com.github.lzyzsd.jsbridge;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.os.Looper;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressLint("SetJavaScriptEnabled")
public class BridgeWebView extends WebView implements WebViewJavascriptBridge {

    private final String TAG = "BridgeWebView";
    public BridgeWebViewClient mClient;
    public static final String TO_LOAD_JS = "WebViewJavascriptBridge.js";
    Map<String, CallBackFunction> responseCallbacks = new HashMap<String, CallBackFunction>();

    private List<Message> startupMessage = new ArrayList<Message>();

    public List<Message> getStartupMessage() {
        return startupMessage;
    }

    public void setStartupMessage(List<Message> startupMessage) {
        this.startupMessage = startupMessage;
    }

    private long uniqueId = 0;

    private IJsHandlerNativeListener mjsHandlerNativeListener;

    public BridgeWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public BridgeWebView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    public BridgeWebView(Context context) {
        super(context);
        init();
    }

    private void init() {
        this.setVerticalScrollBarEnabled(false);
        this.setHorizontalScrollBarEnabled(false);
        this.getSettings().setJavaScriptEnabled(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(true);
        }
        setWebViewClient(generateBridgeWebViewClient());
    }

    @Override
    public void setWebViewClient(WebViewClient client) {
        if (client instanceof BridgeWebViewClient) {
            mClient = (BridgeWebViewClient) client;
            super.setWebViewClient(client);
        }
    }

    public BridgeWebViewClient getBridgeWebViewClient() {
        return mClient;
    }

    protected BridgeWebViewClient generateBridgeWebViewClient() {
        return new BridgeWebViewClient(this);
    }

    void handlerReturnData(String url) {
        String functionName = BridgeUtil.getFunctionFromReturnUrl(url);
        CallBackFunction f = responseCallbacks.get(functionName);
        String data = BridgeUtil.getDataFromReturnUrl(url);
        if (f != null) {
            f.onCallBack(data);
            responseCallbacks.remove(functionName);
            return;
        }
    }

    @Override
    public void send(String data) {
        send(data, null);
    }

    @Override
    public void send(String data, CallBackFunction responseCallback) {
        doSend(null, data, responseCallback);
    }

    private void doSend(String handlerName, String data, CallBackFunction responseCallback) {
        Message m = new Message();
        if (!TextUtils.isEmpty(data)) {
            m.setData(data);
        }
        if (responseCallback == null) {
            responseCallback = new CallBackFunction() {
                @Override
                public void onCallBack(String data) {

                }
            };
        }
        String callbackStr = String.format(BridgeUtil.CALLBACK_ID_FORMAT, ++uniqueId + (BridgeUtil.UNDERLINE_STR + SystemClock.currentThreadTimeMillis()));
        responseCallbacks.put(callbackStr, responseCallback);
        m.setCallbackId(callbackStr);
        if (!TextUtils.isEmpty(handlerName)) {
            m.setHandlerName(handlerName);
        }
        queueMessage(m);
    }

    private void queueMessage(Message m) {
        if (startupMessage != null) {
            startupMessage.add(m);
        } else {
            dispatchMessage(m);
        }
    }

    void dispatchMessage(Message m) {
        String messageJson = m.toJson();
        //escape special characters for json string
        messageJson = messageJson.replaceAll("(\\\\)([^utrn])", "\\\\\\\\$1$2");
        messageJson = messageJson.replaceAll("(?<=[^\\\\])(\")", "\\\\\"");
        String javascriptCommand = String.format(BridgeUtil.JS_HANDLE_MESSAGE_FROM_JAVA, messageJson);
        if (Thread.currentThread() == Looper.getMainLooper().getThread()) {
            this.loadUrl(javascriptCommand);
        }
    }

    void flushMessageQueue() {
        if (Thread.currentThread() == Looper.getMainLooper().getThread()) {
            loadUrl(BridgeUtil.JS_FETCH_QUEUE_FROM_JAVA, new CallBackFunction() {

                @Override
                public void onCallBack(String data) {
                    // deserializeMessage
                    List<Message> list = null;
                    try {
                        list = Message.toArrayList(data);
                    } catch (Exception e) {
                        e.printStackTrace();
                        return;
                    }
                    if (list == null || list.size() == 0) {
                        return;
                    }
                    for (int i = 0; i < list.size(); i++) {
                        Message m = list.get(i);
                        String responseId = m.getResponseId();
                        // 是否是response
                        if (!TextUtils.isEmpty(responseId)) {
                            CallBackFunction function = responseCallbacks.get(responseId);
                            String responseData = m.getResponseData();
                            function.onCallBack(responseData);
                            responseCallbacks.remove(responseId);
                        } else {
                            CallBackFunction responseFunction = null;
                            // if had callbackId
                            final String callbackId = m.getCallbackId();
                            if (!TextUtils.isEmpty(callbackId)) {
                                responseFunction = new CallBackFunction() {
                                    @Override
                                    public void onCallBack(String data) {
                                        Message responseMsg = new Message();
                                        responseMsg.setResponseId(callbackId);
                                        responseMsg.setResponseData(data);
                                        queueMessage(responseMsg);
                                    }
                                };
                            } else {
                                responseFunction = new CallBackFunction() {
                                    @Override
                                    public void onCallBack(String data) {
                                        // do nothing
                                    }
                                };
                            }
                            jsHandlerNative(m.getHandlerName(), m.getData(), responseFunction);
                        }
                    }
                }
            });
        }
    }

    public void loadUrl(String jsUrl, CallBackFunction returnCallback) {
        this.loadUrl(jsUrl);
        responseCallbacks.put(BridgeUtil.parseFunctionName(jsUrl), returnCallback);
    }

    /**
     * call javascript registered handler
     */
    public void callHandler(String handlerName, String data, CallBackFunction callBack) {
        doSend(handlerName, data, callBack);
    }

    /**
     * <br> Description: 处理js调用Native
     * <br> Author:      谢文良
     * <br> Date:        2017/10/27 11:24
     *
     * @param key      key
     * @param data     data
     * @param function CallBackFunction
     */
    public void jsHandlerNative(String key, String data, CallBackFunction function) {
        if (mjsHandlerNativeListener != null) {
            mjsHandlerNativeListener.jsHandlerNative(key, data, function);
        }
    }

    /**
     * <br> Description: 清空数据
     * <br> Author:      谢文良
     * <br> Date:        2017/11/3 13:59
     */
    @Override
    public void destroy() {
        if (mClient != null) {
            mClient.destroy();
        }
        if (getParent() != null) {
            ((ViewGroup) getParent()).removeView(this);
        }
        super.destroy();
    }

    /**
     * <br> ClassName:   BridgeWebView
     * <br> Description: 回调监听接口
     * <br>
     * <br> Author:      谢文良
     * <br> Date:        2017/11/22 15:03
     */
    public interface IJsHandlerNativeListener {
        /**
         * <br> Description: 回调监听
         * <br> Author:      谢文良
         * <br> Date:        2017/11/22 15:03
         *
         * @param key      key
         * @param data     data
         * @param function function
         */
        void jsHandlerNative(String key, String data, CallBackFunction function);
    }

    public void setMjsHandlerNativeListener(IJsHandlerNativeListener mjsHandlerNativeListener) {
        this.mjsHandlerNativeListener = mjsHandlerNativeListener;
    }
}
