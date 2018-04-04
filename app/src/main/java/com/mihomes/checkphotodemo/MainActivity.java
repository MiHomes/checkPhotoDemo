package com.mihomes.checkphotodemo;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.bigkoo.alertview.AlertView;
import com.bigkoo.alertview.OnItemClickListener;
import com.jph.takephoto.app.TakePhoto;
import com.jph.takephoto.app.TakePhotoImpl;
import com.jph.takephoto.compress.CompressConfig;
import com.jph.takephoto.model.InvokeParam;
import com.jph.takephoto.model.TContextWrap;
import com.jph.takephoto.model.TImage;
import com.jph.takephoto.model.TResult;
import com.jph.takephoto.model.TakePhotoOptions;
import com.jph.takephoto.permission.InvokeListener;
import com.jph.takephoto.permission.PermissionManager;
import com.jph.takephoto.permission.TakePhotoInvocationHandler;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static android.util.Log.d;

public class MainActivity extends AppCompatActivity implements TakePhoto.TakeResultListener,InvokeListener {

    private static final String TAG = "MainActivity";
    private Button btnSingle;
    private Button btnMulti;
    private RecyclerView recShow;
    private PhotoAdapter photoAdapter;
    private TakePhoto takePhoto;
    private InvokeParam invokeParam;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        initData();
    }

    private void initData() {
        //设置展示框中单行展示的图片个数
        recShow.setLayoutManager(new GridLayoutManager(this, 3));
        //初始化自定义Adapter，onAddPicListener是添加图片的点击监听器，onPicClickListener是添加图片成功以后，点击放大的监听器。
        photoAdapter = new PhotoAdapter(this, onAddPicListener, onPicClickListener);
        //设置多选时最多选择的图片张数
        photoAdapter.setSelectMax(3);
        recShow.setAdapter(photoAdapter);
    }

    //图片点击事件
    private PhotoAdapter.onPicClickListener onPicClickListener = new PhotoAdapter.onPicClickListener() {
        @Override
        public void onPicClick(View view, int position) {
            imageUrls.clear();
            for (int i = 0; i < selectMedia.size(); i++) {
                String compressPath = selectMedia.get(i).getCompressPath();
                imageUrls.add(compressPath);
            }
            imageBrower(position, imageUrls);
        }
    };
    private void imageBrower(int position, ArrayList<String> imageUrls) {
        Intent intent = new Intent(this, ImagePagerActivity.class);
        intent.putExtra(ImagePagerActivity.EXTRA_IMAGE_URLS, imageUrls);
        intent.putExtra(ImagePagerActivity.EXTRA_IMAGE_INDEX, position);
        startActivity(intent);
    }

    /**
     * 获取TakePhoto实例
     * 没有继承TakePhotoActivity 所写
     */
    public TakePhoto getTakePhoto() {
        if (takePhoto == null) {
            takePhoto = (TakePhoto) TakePhotoInvocationHandler.of(this).bind(new TakePhotoImpl(this, this));
        }
        return takePhoto;
    }


    private List<TImage> selectMedia = new ArrayList<>();
    private List<TImage> updateMedia = new ArrayList<>();
    ArrayList<String> imageUrls = new ArrayList<>();
    private PhotoAdapter.onAddPicListener onAddPicListener = new PhotoAdapter.onAddPicListener() {
        @Override
        public void onAddPicClick(int type, int position) {
            switch (type) {
                case 0: //点击图片
                    new AlertView("上传图片", null, "取消", null,
                            new String[]{"拍照", "从相册中选择"},
                            MainActivity.this, AlertView.Style.ActionSheet, new OnItemClickListener() {
                        @Override
                        public void onItemClick(Object o, int position) {
                            TakePhoto takePhoto = getTakePhoto();
                            //获取TakePhoto图片路径
                            if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                                 d(TAG, "SD卡可用");

                                File file = new File(Environment.getExternalStorageDirectory(), "/photo/" + System.currentTimeMillis() + ".jpg");
                                if (!file.getParentFile().exists()) file.getParentFile().mkdirs();
                                Uri imageUri = Uri.fromFile(file);
                                d(TAG, "文件创建成功并获取URL == " + imageUri);
                                //设置takephoto的照片使用压缩
                                configCompress(takePhoto);
                                //设置Takephoto 使用TakePhoto自带的相册   照片旋转角度纠正
                                configTakePhotoOption(takePhoto);
                                switch (position) {
                                    case 0:  //拍照
                                        takePhoto.onPickFromCapture(imageUri);
                                        break;
                                    case 1:  //相册选择
                                        //设置最多几张
                                      /*  if (selectMedia.size() == 0){
                                            takePhoto.onPickMultiple(3);
                                        }else if (selectMedia.size() == 1){
                                            takePhoto.onPickMultiple(2);
                                        }else {
                                            takePhoto.onPickMultiple(1);
                                        }*/
                                        if (selectMedia.size() == 0) {
                                            takePhoto.onPickMultiple(3);
                                        } else if (selectMedia.size() == 1) {
                                            takePhoto.onPickMultiple(2);
                                        } else if (selectMedia.size() == 2) {
                                            takePhoto.onPickMultiple(1);
                                        }/* else if (selectMedia.size() == 3) {
                                            takePhoto.onPickMultiple(2);
                                        } else if (selectMedia.size() == 4) {
                                            takePhoto.onPickMultiple(1);
                                        }*/
                                        break;
                                }
                            } else {
                                Log.d(TAG, "SD卡bu可用");
                            }

                        }
                    }).show();
                    break;
                case 1:  //点击删除按钮

                        selectMedia.remove(position);
                        photoAdapter.notifyItemRemoved(position);

                    break;
            }
        }
    };
    //设置Takephoto 使用TakePhoto自带的相册   照片旋转角度纠正
    private void configTakePhotoOption(TakePhoto takePhoto) {
        TakePhotoOptions.Builder builder = new TakePhotoOptions.Builder();
        builder.setWithOwnGallery(true);
        builder.setCorrectImage(true);
        takePhoto.setTakePhotoOptions(builder.create());
    }

    //设置takephoto的照片使用压缩
    private void configCompress(TakePhoto takePhoto) {
        CompressConfig config;
        config = new CompressConfig.Builder()
                .setMaxSize(102400)
                .setMaxPixel(800)
                .enableReserveRaw(true)
                .create();
        takePhoto.onEnableCompress(config, false);
    }
    private void initView() {

        recShow = (RecyclerView)findViewById(R.id.recycleview_show);
    }


    @Override
    public void takeCancel() {
        Log.d(TAG, "takeCancel");
    }

    @Override
    public void takeFail(TResult result, String msg) {
        ArrayList<TImage> images = result.getImages();
        showImg(result.getImages());
        Log.d(TAG, "takeFail" + images.get(0).toString());
    }

    @Override
    public void takeSuccess(TResult result) {
        showImg(result.getImages());
    }

    //没有继承TakePhotoActivity 所写
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        //将拍照返回的结果交给takePhoto处理
        getTakePhoto().onActivityResult(requestCode, resultCode, data);
        super.onActivityResult(requestCode, resultCode, data);
    }

    //没有继承TakePhotoActivity 所写
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        getTakePhoto().onSaveInstanceState(outState);
        super.onSaveInstanceState(outState);
    }

    //没有继承TakePhotoActivity 所写
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        PermissionManager.TPermissionType type = PermissionManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
        PermissionManager.handlePermissionsResult(this, type, invokeParam, this);
    }

    //没有继承TakePhotoActivity 所写
    @Override
    public PermissionManager.TPermissionType invoke(InvokeParam invokeParam) {
        PermissionManager.TPermissionType type = PermissionManager.checkPermission(TContextWrap.of(this), invokeParam.getMethod());
        if (PermissionManager.TPermissionType.WAIT.equals(type)) {
            this.invokeParam = invokeParam;
        }
        return type;
    }


    //图片成功后返回执行的方法
    private void showImg(ArrayList<TImage> images) {
        Log.d(TAG, images.toString());
        //目的是防止上传重复的图片
        updateMedia.clear();

        for (int i = 0; i < images.size(); i++) {
            if (images.get(i).getCompressPath() != null) {
                selectMedia.add(images.get(i));
                updateMedia.add(images.get(i));
            }
        }
        if (selectMedia != null) {
            photoAdapter.setList(selectMedia);
            photoAdapter.notifyDataSetChanged();
        }
        //需上传图片，请使用updateMedia数组。
    }

}
