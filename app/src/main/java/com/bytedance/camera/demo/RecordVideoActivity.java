package com.bytedance.camera.demo;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.VideoView;

public class RecordVideoActivity extends AppCompatActivity {

    private VideoView videoView;
    private static final int REQUEST_VIDEO_CAPTURE = 1;

    private static final int REQUEST_EXTERNAL_CAMERA = 101;
    private ImageView iv_pause;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record_video);

        videoView = findViewById(R.id.img);
        iv_pause = findViewById(R.id.iv_pause);
        findViewById(R.id.btn_picture).setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(RecordVideoActivity.this,
                    Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                //todo 在这里申请相机、存储的权限
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.CAMERA,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        REQUEST_EXTERNAL_CAMERA);
            } else {
                //todo 打开相机拍摄
                Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
                if(intent.resolveActivity(getPackageManager()) != null)
                    startActivityForResult(intent,REQUEST_VIDEO_CAPTURE);
            }
        });

        //点击暂停与继续播放
        videoView.setOnTouchListener((v, event) -> {
            if(v instanceof VideoView){
                VideoView player = (VideoView) v;
                //如果正在播放则暂停,否则开始播放
                if(player.isPlaying() && player.canPause()){
                    player.pause();
                    iv_pause.setVisibility(View.VISIBLE);
                }else{
                    player.start();
                    iv_pause.setVisibility(View.INVISIBLE);
                }
            }
            return false;
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == REQUEST_VIDEO_CAPTURE && resultCode == RESULT_OK) {
            //todo 播放刚才录制的视频
            Uri videoUri = intent.getData();
            videoView.setVideoURI(videoUri);
            videoView.start();
            iv_pause.setVisibility(View.GONE);

            //扫描使得视频在相册中可见
            Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            mediaScanIntent.setData(videoUri);
            this.sendBroadcast(mediaScanIntent);

        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_EXTERNAL_CAMERA: {
                //todo 判断权限是否已经授予
                //如果用户同意了相机和外部存储权限
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
