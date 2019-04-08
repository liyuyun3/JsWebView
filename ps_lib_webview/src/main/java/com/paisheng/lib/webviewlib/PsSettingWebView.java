package com.paisheng.lib.webviewlib;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.TypedArray;
import android.os.Build;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.webkit.CookieManager;
import android.webkit.WebSettings;


/**
 * <br> ClassName:   PsSettingWebView
 * <br> Description: 通用配置PSWebView
 * <br>
 * <br> @author:      谢文良
 * <br> Date:        2017/10/27 10:02
 */

public class PsSettingWebView extends PsJsBridgeWebView {
    private static final String USER_AGENT_TYPE = "tuandaiapp_android";
    private static final String CHACHE_PATH = "/webcache";
    /*** 浏览器自带标识 ***/
    private String mWebViewDefaultAgentType;
    private String mUserAgentType;

    public PsSettingWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initAttributes(context, attrs);
        initSetting();
    }

    public PsSettingWebView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initAttributes(context, attrs);
        initSetting();
    }

    public PsSettingWebView(Context context) {
        super(context);
        initSetting();
    }

    private void initAttributes(Context context, AttributeSet attrs) {
        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.PsSettingWebView);
        mUserAgentType = ta.getString(R.styleable.PsSettingWebView_userAgentType);
        ta.recycle();
    }

    /**
     * <br> Description: 初始化配置
     * <br> Author:      谢文良
     * <br> Date:        2017/10/27 10:19
     */
    @SuppressLint("SetJavaScriptEnabled")
    private void initSetting() {
        getSettings().setJavaScriptEnabled(true);
        getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
        getSettings().setDefaultTextEncodingName("utf-8");
        getSettings().setTextZoom(100);
        getSettings().setUseWideViewPort(true);
        getSettings().setLoadWithOverviewMode(true);
        getSettings().setBuiltInZoomControls(true);
        getSettings().setDisplayZoomControls(false);
        getSettings().setSupportZoom(true);
        getSettings().setDomStorageEnabled(true);
        getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
        getSettings().setDatabaseEnabled(true);
        getSettings().setDatabasePath(getChacheDir(getContext()));
        getSettings().setAppCachePath(getChacheDir(getContext()));
        getSettings().setAppCacheEnabled(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true);
        }
        mWebViewDefaultAgentType = getSettings().getUserAgentString();
        setUserAgentType();
    }

    /**
     * <br> Description: 获取版本号
     * <br> Author:      谢文良
     * <br> Date:        2017/10/27 10:19
     */
    public String getCurrentVersion() {
        String version = "";
        try {
            PackageManager pm = getContext().getApplicationContext().getPackageManager();
            PackageInfo packInfo = pm.getPackageInfo(getContext().getApplicationContext().getPackageName(), 0);
            version = packInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return version;
    }

    /**
     * <br> Description: 截取版本号（前三位，如 5.1.9.1 -> 5.1.9）
     * <br> Author:      liaoshengjian
     * <br> Date:        2017/11/29 14:42
     */
    public String getTDVersionName(String version) {
        String[] versionArray = version.split("\\.");
        if (versionArray.length > 3) {
            String newVersion = "";
            for (int i = 0; i < 3; i++) {
                newVersion += i < 2 ? versionArray[i] + "." : versionArray[i];
            }
            return newVersion;
        }
        return version;
    }

    /**
     * <br> Description: 返回用户标识
     * <br> Author:      谢文良
     * <br> Date:        2017/10/27 10:19
     *
     * @return 用户标识
     */
    protected String getUserAgentTyPe() {
        return TextUtils.isEmpty(mUserAgentType) ? USER_AGENT_TYPE : mUserAgentType;
    }

    /**
     * <br> Description: 修改浏览器标识
     * <br> Author:      谢文良
     * <br> Date:        2018/1/17 14:29
     *
     * @param userAgentType 浏览器标识
     */
    public void setUserAgentType(String userAgentType) {
        mUserAgentType = userAgentType;
        setUserAgentType();
    }

    private void setUserAgentType() {
        getSettings()
                .setUserAgentString(mWebViewDefaultAgentType
                        + "[" + getUserAgentTyPe() + "_" + getTDVersionName(getCurrentVersion())
                        + "][" + android.os.Build.BRAND + "]");
    }

    @Override
    public void destroy() {
        clearCache(true);
        clearFormData();
        clearMatches();
        clearSslPreferences();
        clearDisappearingChildren();
        clearHistory();
        clearAnimation();
        loadUrl("about:blank");
        removeAllViews();
        super.destroy();
    }

    /**
     * <br> Description: 获取webview缓存路径
     * <br> Author:      xwl
     * <br> Date:        2018/3/21 15:08
     *
     * @param context Context
     * @return webview缓存路径
     */
    public static String getChacheDir(Context context) {
        if (context != null) {
            return context.getFilesDir().getAbsolutePath() + CHACHE_PATH;
        }
        return null;
    }
}
