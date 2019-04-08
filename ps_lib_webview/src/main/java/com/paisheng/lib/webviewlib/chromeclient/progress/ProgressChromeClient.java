package com.paisheng.lib.webviewlib.chromeclient.progress;

import android.app.Activity;
import android.webkit.WebChromeClient;
import android.webkit.WebView;


import java.lang.ref.WeakReference;

/**
 * <br> ClassName:   ${className}
 * <br> Description:
 * <br>
 * <br> @author:      谢文良
 * <br> Date:        2017/10/27 17:44
 */

public class ProgressChromeClient extends WebChromeClient {
    private int progress = -1;
    private static final int LOADING_FULL = 100;
    /*** 加载开始 ***/
    public static final int LOADING_START = 0;
    /*** 加载中 ***/
    public static final int LOADING_ING = 1;
    /*** 加载结束 ***/
    public static final int LOADING_END = 2;
    /*** 对Activity的弱引用 ***/
    protected WeakReference<Activity> mWeakReferenceActivity;

    public ProgressChromeClient(Activity mActivity) {
        mWeakReferenceActivity = new WeakReference<>(mActivity);
    }

    @Override
    public final void onProgressChanged(WebView view, int newProgress) {
        super.onProgressChanged(view, newProgress);
        if (progress == -1) {
            onPageLoading(view, LOADING_START, newProgress);
            progress = newProgress;
        } else if (newProgress == LOADING_FULL) {
            onPageLoading(view, LOADING_END, newProgress);
            progress = -1;
        } else {
            onPageLoading(view, LOADING_ING, newProgress);
            progress = newProgress;
        }
    }

    /**
     * <br> Description: 加载中
     * <br> Author:      谢文良
     * <br> Date:        2017/10/27 17:50
     *
     * @param view        WebView
     * @param loadingType loadingType
     * @param newProgress newProgress
     */
    public void onPageLoading(WebView view, int loadingType, int newProgress) {

    }
}
