package com.paisheng.lib.webviewlib.chromeclient.video;

import android.app.Activity;
import android.content.Context;
import android.media.MediaPlayer;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.view.LayoutInflater;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.WebChromeClient;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.paisheng.lib.webviewlib.PsVideoEnabledWebView;
import com.paisheng.lib.webviewlib.R;
import com.paisheng.lib.webviewlib.chromeclient.label.AbstractInputLabelChromeClient;

import java.lang.ref.WeakReference;


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
public abstract class AbstractVideoEnabledWebChromeClient extends AbstractInputLabelChromeClient implements MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener {

    /**
     * A View in the activity's layout that contains every other view that should be hidden when the video goes full-screen.
     */
    private View activityNonVideoView;
    /**
     * A ViewGroup in the activity's layout that will display the video. Typically you would like this to fill the whole layout.
     */
    private ViewGroup activityVideoView;
    /**
     * A View to be shown while the video is loading (typically only used in API level <11). Must be already inflated and not attached to a parent view.
     */
    private View loadingView;
    private PsVideoEnabledWebView webView;
    /**
     * Indicates if the video is being displayed using a custom view (typically full-screen)
     */
    private boolean isVideoFullscreen;
    private FrameLayout videoViewContainer;
    private WebChromeClient.CustomViewCallback videoViewCallback;

    /**
     * Builds a video enabled WebChromeClient.
     *
     * @param mActivity Activity
     * @param webView   The owner VideoEnabledWebView. Passing it will enable the VideoEnabledWebChromeClient to detect the HTML5 video ended event and exit full-screen.
     *                  Note: The web page must only contain one video tag in order for the HTML5 video ended event to work. This could be improved if needed (see Javascript code).
     */
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @SuppressWarnings("unused")
    public AbstractVideoEnabledWebChromeClient(Activity mActivity, PsVideoEnabledWebView webView) {
        super(mActivity);
        this.mWeakReferenceActivity = new WeakReference<>(mActivity);
        this.webView = webView;
        this.isVideoFullscreen = false;
        initWebView(webView);
    }

    private void initWebView(PsVideoEnabledWebView webView) {
        if (webView == null || webView.getParent() == null) {
            throw new IllegalArgumentException("webView or webView.Parent is null");
        }
        ViewGroup mParent = (ViewGroup) webView.getParent();
        Context mContext = webView.getContext();
        mParent.removeView(webView);

        LinearLayout linearLayout = new LinearLayout(mContext);
        linearLayout.setLayoutParams(webView.getLayoutParams());
        linearLayout.setOrientation(LinearLayout.VERTICAL);

        RelativeLayout nonVideoLayout = new RelativeLayout(mContext);
        nonVideoLayout.setLayoutParams(new LinearLayout.LayoutParams(-1, -1));
        webView.setLayoutParams(new RelativeLayout.LayoutParams(-1, -1));
        nonVideoLayout.addView(webView);
        linearLayout.addView(nonVideoLayout);
        activityNonVideoView = nonVideoLayout;

        RelativeLayout videoLayout = new RelativeLayout(mContext);
        videoLayout.setLayoutParams(new LinearLayout.LayoutParams(-1, -1));
        linearLayout.addView(videoLayout);
        activityVideoView = videoLayout;

        mParent.addView(linearLayout);

        loadingView = LayoutInflater.from(mContext).inflate(R.layout.ps_video_webview_loading, null);
    }

    /**
     * Indicates if the video is being displayed using a custom view (typically full-screen)
     *
     * @return true it the video is being displayed using a custom view (typically full-screen)
     */
    public boolean isVideoFullscreen() {
        return isVideoFullscreen;
    }

    @Override
    public final void onShowCustomView(View view, WebChromeClient.CustomViewCallback callback) {
        if (view instanceof FrameLayout) {
            // A video wants to be shown
            FrameLayout frameLayout = (FrameLayout) view;
            View focusedChild = frameLayout.getFocusedChild();

            // Save video related variables
            this.isVideoFullscreen = true;
            this.videoViewContainer = frameLayout;
            this.videoViewCallback = callback;

            // Hide the non-video view, add the video view, and show it
            activityNonVideoView.setVisibility(View.GONE);
            activityVideoView.addView(videoViewContainer, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            activityVideoView.setVisibility(View.VISIBLE);

            if (focusedChild instanceof android.widget.VideoView) {
                // android.widget.VideoView (typically API level <11)
                android.widget.VideoView videoView = (android.widget.VideoView) focusedChild;

                // Handle all the required events
                videoView.setOnPreparedListener(this);
                videoView.setOnCompletionListener(this);
                videoView.setOnErrorListener(this);
            } else {
                // Other classes, including:
                // - android.webkit.HTML5VideoFullScreen$VideoSurfaceView, which inherits from android.view.SurfaceView (typically API level 11-18)
                // - android.webkit.HTML5VideoFullScreen$VideoTextureView, which inherits from android.view.TextureView (typically API level 11-18)
                // - com.android.org.chromium.content.browser.ContentVideoView$VideoSurfaceView, which inherits from android.view.SurfaceView (typically API level 19+)

                // Handle HTML5 video ended event only if the class is a SurfaceView
                // Test case: TextureView of Sony Xperia T API level 16 doesn't work fullscreen when loading the javascript below
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

            // Notify full-screen change
            toggledFullscreen(true);
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public final void onShowCustomView(View view, int requestedOrientation, WebChromeClient.CustomViewCallback callback) // Available in API level 14+, deprecated in API level 18+
    {
        onShowCustomView(view, callback);
    }

    @Override
    public final void onHideCustomView() {
        // This method should be manually called on video end in all cases because it's not always called automatically.
        // This method must be manually called on back key press (from this class' onBackPressed() method).

        if (isVideoFullscreen) {
            // Hide the video view, remove it, and show the non-video view
            activityVideoView.setVisibility(View.GONE);
            activityVideoView.removeView(videoViewContainer);
            activityNonVideoView.setVisibility(View.VISIBLE);

            // Call back (only in API level <19, because in API level 19+ with chromium webview it crashes)
//            if (videoViewCallback != null && !videoViewCallback.getClass().getName().contains(".chromium."))
            if (videoViewCallback != null) {
                videoViewCallback.onCustomViewHidden();
            }

            // Reset video related variables
            isVideoFullscreen = false;
            videoViewContainer = null;
            videoViewCallback = null;

            // Notify full-screen change
            toggledFullscreen(false);
        }
    }

    @Override
    public final View getVideoLoadingProgressView() // Video will start loading
    {
        if (loadingView != null) {
            loadingView.setVisibility(View.VISIBLE);
            return loadingView;
        } else {
            return super.getVideoLoadingProgressView();
        }
    }

    @Override
    public final void onPrepared(MediaPlayer mp) // Video will start playing, only called in the case of android.widget.VideoView (typically API level <11)
    {
        if (loadingView != null) {
            loadingView.setVisibility(View.GONE);
        }
    }

    @Override
    public final void onCompletion(MediaPlayer mp) // Video finished playing, only called in the case of android.widget.VideoView (typically API level <11)
    {
        onHideCustomView();
    }

    @Override
    public final boolean onError(MediaPlayer mp, int what, int extra) // Error while playing video, only called in the case of android.widget.VideoView (typically API level <11)
    {
        // By returning false, onCompletion() will be called
        return false;
    }

    /**
     * Notifies the class that the back key has been pressed by the user.
     * This must be called from the Activity's onBackPressed(), and if it returns false, the activity itself should handle it. Otherwise don't do anything.
     *
     * @return Returns true if the event was handled, and false if was not (video view is not visible)
     */
    @SuppressWarnings("unused")
    public final boolean onBackPressed() {
        if (isVideoFullscreen) {
            onHideCustomView();
            return true;
        } else {
            return false;
        }
    }

    /**
     * <br> Description: 设定Activity是否全屏
     * <br> Author:      谢文良
     * <br> Date:        2017/10/30 11:22
     *
     * @param isFullscreen 是否全屏
     */
    private void toggledFullscreen(boolean isFullscreen) {
        Activity mActivity = mWeakReferenceActivity.get();
        if (mActivity == null) {
            return;
        }
        if (isFullscreen) {
            WindowManager.LayoutParams attrs = mActivity.getWindow().getAttributes();
            attrs.flags |= WindowManager.LayoutParams.FLAG_FULLSCREEN;
            attrs.flags |= WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
            mActivity.getWindow().setAttributes(attrs);
            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                //noinspection all
                mActivity.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE);
            }
        } else {
            WindowManager.LayoutParams attrs = mActivity.getWindow().getAttributes();
            attrs.flags &= ~WindowManager.LayoutParams.FLAG_FULLSCREEN;
            attrs.flags &= ~WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
            mActivity.getWindow().setAttributes(attrs);
            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                //noinspection all
                mActivity.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
            }
        }
    }
}
