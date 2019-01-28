package com.bytedance.camera.demo;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.widget.ImageView;
import android.widget.Toast;

import com.bytedance.camera.demo.utils.Utils;

import java.io.File;

public class TakePictureActivity extends AppCompatActivity {

    private ImageView imageView;
    private static final int REQUEST_IMAGE_CAPTURE = 1;

    private static final int REQUEST_EXTERNAL_STORAGE = 101;
    private File imgFile;

    @Override

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_take_picture);
        imageView = findViewById(R.id.img);
        findViewById(R.id.btn_picture).setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(TakePictureActivity.this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                    || ContextCompat.checkSelfPermission(TakePictureActivity.this,
                    Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                //todo 在这里申请相机、存储的权限
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.CAMERA,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        REQUEST_EXTERNAL_STORAGE);
            } else {
                takePicture();
            }
        });

    }

    private void takePicture() {
        //todo 打开相机
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        //初始化存储位置
        imgFile = Utils.getOutputMediaFile(Utils.MEDIA_TYPE_IMAGE);
        if(imgFile !=null){
            Uri imgUri;
            if(Build.VERSION.SDK_INT >=24){
                imgUri = FileProvider.getUriForFile(this, "com.bytedance.camera.demo", imgFile);

            }else{
                imgUri = Uri.fromFile(imgFile);
            }

            //指定拍照完成后存储位置
            intent.putExtra(MediaStore.EXTRA_OUTPUT,imgUri);
            startActivityForResult(intent,REQUEST_IMAGE_CAPTURE);

        }




    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            setPic();
        }
    }

    private void setPic() {

        //todo 根据imageView裁剪
        int target_w = imageView.getWidth();
        int target_h = imageView.getHeight();



        //todo 根据缩放比例读取文件，生成Bitmap
        //先只解析尺寸计算缩放比例
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds= true;
        BitmapFactory.decodeFile(imgFile.getAbsolutePath(), options);

        int img_height = options.outHeight;
        int img_width = options.outWidth;

        int scale = Math.min(img_width/target_w,img_height/target_h);
        //根据缩放比例生成bitmap
        options.inJustDecodeBounds = false;
        options.inSampleSize = scale;
        Bitmap bitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath(), options);

        //todo 如果存在预览方向改变，进行图片旋转
        Utils.rotateImage(bitmap,imgFile.getAbsolutePath());

        imageView.setImageBitmap(bitmap);

        //扫描相册
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        mediaScanIntent.setData(Uri.fromFile(imgFile));
        this.sendBroadcast(mediaScanIntent);

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_EXTERNAL_STORAGE: {
                //todo 判断权限是否已经授予
                if(grantResults[0] == PackageManager.PERMISSION_GRANTED
                        && grantResults[1] == PackageManager.PERMISSION_GRANTED){
                    Toast.makeText(this,"相机和存储权限已获取！",Toast.LENGTH_SHORT).show();
                }else{
                    Toast.makeText(this,"未成功获取权限!",Toast.LENGTH_SHORT).show();
                }
                break;
            }
        }
    }
}
