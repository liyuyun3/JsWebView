package com.paisheng.lib.webviewlib.chromeclient.label;


import android.Manifest;
import android.app.Activity;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.webkit.ValueCallback;
import android.webkit.WebView;

import com.paisheng.lib.webviewlib.chromeclient.progress.ProgressChromeClient;
import com.yancy.gallerypick.config.GalleryConfig;
import com.yancy.gallerypick.config.GalleryPick;
import com.yancy.gallerypick.inter.IHandlerCallBack;
import com.yancy.gallerypick.inter.ImageLoader;

import java.util.ArrayList;
import java.util.List;


/**
 * <br> ClassName:   TdInputLabelChromeClient
 * <br> Description: 对input标签的支持
 * <br>
 * <br> @author:      谢文良
 * <br> Date:        2017/10/27 15:56
 */

@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
public abstract class AbstractInputLabelChromeClient extends ProgressChromeClient implements ImageLoader{
    private ValueCallback<Uri[]> filePathCallback;
    private ValueCallback uploadMsg;
    private IHandlerCallBack iHandlerCallBack;
    private GalleryConfig galleryConfig;
    private String[] permissions = {Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE};

    public AbstractInputLabelChromeClient(Activity mActivity) {
        super(mActivity);
        iHandlerCallBack = new SimpleHandlerCallBack() {
            @Override
            public void onSuccess(List<String> photoList) {
                if (photoList != null && photoList.size() > 0) {
                    Uri uri = Uri.parse("file:///" + photoList.get(0));
                    if (filePathCallback != null) {
                        filePathCallback.onReceiveValue(new Uri[]{uri});
                        filePathCallback = null;
                    }
                    if (uploadMsg != null) {
                        uploadMsg.onReceiveValue(uri);
                        uploadMsg = null;
                    }
                }
            }

            @Override
            public void onFinish() {
                if (filePathCallback != null) {
                    filePathCallback.onReceiveValue(null);
                    filePathCallback = null;
                }
                if (uploadMsg != null) {
                    uploadMsg.onReceiveValue(null);
                    uploadMsg = null;
                }
            }
        };
    }

    //android 5.0 支持input标签
    @Override
    public final boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback,
                                           FileChooserParams fileChooserParams) {
        fileChoosers(filePathCallback);
        return true;
    }

    /**
     * <br> Description: android 3.0+支持input标签
     * <br> Author:      谢文良
     * <br> Date:        2017/10/27 16:37
     *
     * @param uploadMsg uploadMsg
     */
    public final void openFileChooser(ValueCallback<Uri> uploadMsg) {
        fileChooser(uploadMsg);
    }

    /**
     * <br> Description: android 3.0+支持input标签
     * <br> Author:      谢文良
     * <br> Date:        2017/10/27 16:37
     *
     * @param uploadMsg  uploadMsg
     * @param acceptType acceptType
     */
    public final void openFileChooser(ValueCallback uploadMsg, String acceptType) {
        fileChooser(uploadMsg);
    }

    /**
     * <br> Description: android 4.1支持input标签
     * <br> Author:      谢文良
     * <br> Date:        2017/10/27 16:38
     */
    public final void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType, String capture) {
        fileChooser(uploadMsg);
    }

    /**
     * <br> Description: 打开文件选择器
     * <br> Author:      谢文良
     * <br> Date:        2017/10/27 17:40
     *
     * @param filePathCallback 回调函数
     */
    private void fileChoosers(final ValueCallback<Uri[]> filePathCallback) {
        this.filePathCallback = filePathCallback;
        this.uploadMsg = null;
        openImageChoose();
    }

    /**
     * <br> Description: 打开文件选择器
     * <br> Author:      谢文良
     * <br> Date:        2017/10/27 17:40
     *
     * @param uploadMsg 回调函数
     */
    private void fileChooser(final ValueCallback uploadMsg) {
        this.filePathCallback = null;
        this.uploadMsg = uploadMsg;
        openImageChoose();
    }

    /**
     * <br> Description: 打开文件选择器
     * <br> Author:      谢文良
     * <br> Date:        2017/10/30 14:39
     */
    private void openImageChoose() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            applyPermissions(permissions, new ApplyPermissionsListener() {
                @Override
                public void applyResult(boolean isAgreed) {
                    open(isAgreed);
                }
            });
        } else {
            open(true);
        }
    }

    /**
     * <br> Description: 申请权限
     * <br> Author:      谢文良
     * <br> Date:        2017/11/1 11:29
     *
     * @param permissions 权限组合
     * @param listener    回掉接口
     */
    public abstract void applyPermissions(String[] permissions, ApplyPermissionsListener listener);

    /**
     * <br> Description: 打开相册选择
     * <br> Author:      谢文良
     * <br> Date:        2017/11/1 11:30
     *
     * @param isAgreed 是否同意
     */
    private void open(boolean isAgreed) {
        if (!isAgreed) {
            return;
        }
        Activity mActivity = mWeakReferenceActivity.get();
        if (mActivity != null) {
            if (galleryConfig == null) {
                galleryConfig = new GalleryConfig.Builder(mActivity)
                        // ImageLoader 加载框架（必填）
                        .imageLoader(this)
                        // 监听接口（必填）
                        .iHandlerCallBack(iHandlerCallBack)
                        // 记录已选的图片
                        .pathList(new ArrayList<String>())
                        // 配置是否多选的同时 配置多选数量   默认：false ， 9
                        // 配置裁剪功能的参数，   默认裁剪比例 1:1
                        .multiSelect(false)
                        .crop(false, 1, 1, 500, 500)
                        // 是否现实相机按钮  默认：false
                        .isShowCamera(true)
                        // 图片存放路径
                        .filePath("/Pictures")
                        .build();
            }
            GalleryPick.getInstance().setGalleryConfig(galleryConfig).open(mActivity);
        }
    }

    /**
     * <br> ClassName:   AbstractInputLabelChromeClient
     * <br> Description: 权限申请接口回调
     * <br>
     * <br> Author:      谢文良
     * <br> Date:        2017/11/1 11:13
     */
    public interface ApplyPermissionsListener {
        /**
         * <br> Description: 申请权限结果回掉
         * <br> Author:      谢文良
         * <br> Date:        2017/11/1 11:12
         *
         * @param isAgreed 是否同意权限申请
         */
        void applyResult(boolean isAgreed);
    }
}
