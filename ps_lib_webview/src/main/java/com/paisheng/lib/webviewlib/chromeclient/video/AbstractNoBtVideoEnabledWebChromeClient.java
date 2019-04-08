package com.paisheng.lib.webviewlib.chromeclient.video;

import android.app.Activity;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.view.SurfaceView;
import android.view.View;
import android.widget.FrameLayout;

import com.paisheng.lib.webviewlib.PsVideoEnabledWebView;
import com.paisheng.lib.webviewlib.chromeclient.label.AbstractInputLabelChromeClient;


/**
 * This class serves as a WebChromeClient to be set to a WebView, allowing it to play video.
 * Video will play differently depending on target API level (in-line, fullscreen, or both).
 * <p>
 * It has been tested with the following video classes:
 * - android.widget.VideoView (typically API level <11)
 * - android.webkit.HTML5VideoFullScreen$VideoSurfaceView/VideoTextureView (typically API level 11-18)
 * - com.android.org.chromium.content.browser.ContentVideoView$VideoSurfaceView (typically API level 19+)
 * <p>
 * Important notes:
 * - For API level 11+, android:hardwareAccelerated="true" must be set in the application manifest.
 * - The invoking activity must call VideoEnabledWebChromeClient's onBackPressed() inside of its own onBackPressed().
 * - Tested in Android API levels 8-19. Only tested on http://m.youtube.com.
 *
 * @author Cristian Perez (http://cpr.name)
 */
public abstract class AbstractNoBtVideoEnabledWebChromeClient extends AbstractInputLabelChromeClient {

    private PsVideoEnabledWebView webView;

    /**
     * Builds a video enabled WebChromeClient.
     *
     * @param mActivity Activity
     * @param webView   The owner VideoEnabledWebView. Passing it will enable the VideoEnabledWebChromeClient to detect the HTML5 video ended event and exit full-screen.
     *                  Note: The web page must only contain one video tag in order for the HTML5 video ended event to work. This could be improved if needed (see Javascript code).
     */
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @SuppressWarnings("unused")
    public AbstractNoBtVideoEnabledWebChromeClient(Activity mActivity, PsVideoEnabledWebView webView) {
        super(mActivity);
        this.webView = webView;
    }


    @Override
    public final void onShowCustomView(View view, CustomViewCallback callback) {
        if (view instanceof FrameLayout) {
            // A video wants to be shown
            FrameLayout frameLayout = (FrameLayout) view;
            View focusedChild = frameLayout.getFocusedChild();
            if (webView != null && webView.getSettings().getJavaScriptEnabled() && focusedChild instanceof SurfaceView) {
                // Run javascript code that detects the video end and notifies the Javascript interface
                String js = "javascript:";
                js += "var _ytrp_html5_video_last;";
                js += "var _ytrp_html5_video = document.getElementsByTagName('video')[0];";
                js += "if (_ytrp_html5_video != undefined && _ytrp_html5_video != _ytrp_html5_video_last) {";
                {
                    js += "_ytrp_html5_video_last = _ytrp_html5_video;";
                    js += "function _ytrp_html5_video_ended() {";
                    {
                        // Must match Javascript interface name and method of VideoEnableWebView
                        js += "_VideoEnabledWebView.notifyVideoEnd();";
                    }
                    js += "}";
                    js += "_ytrp_html5_video.addEventListener('ended', _ytrp_html5_video_ended);";
                }
                js += "}";
                webView.loadUrl(js);
            }
        }
    }
}
