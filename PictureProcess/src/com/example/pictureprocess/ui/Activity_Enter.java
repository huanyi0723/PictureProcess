package com.example.pictureprocess.ui;

import java.io.File;

import com.example.pictureprocess.R;

import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.ViewGroup.LayoutParams;
import android.widget.RelativeLayout;
import android.app.Activity;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;

public class Activity_Enter extends Activity {
	
	private Uri mImageCaptureUri;// 图像URL地址

	private static final int PICK_FROM_CAMERA = 1; // 从相机中取得图片
	private static final int PICK_FROM_FILE = 2; // 从图库中取得图片
	
	private RelativeLayout album_layout;// 相册
	private RelativeLayout camera_layout; // 拍照
	
	private boolean isCanceledFromUser = true;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setContentView(R.layout.zy_main_test);
				
		isCanceledFromUser = true;
		useDialog();
	}
	
	private void useDialog() {
		LayoutInflater mInflater = LayoutInflater.from(this);
		ViewGroup rootView = (ViewGroup) mInflater.inflate(R.layout.zy_dialog_layout, null);

		rootView.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
		final Dialog dialog = new Dialog(this);
		WindowManager.LayoutParams params = dialog.getWindow().getAttributes();
		params.width = WindowManager.LayoutParams.WRAP_CONTENT;
		params.height = WindowManager.LayoutParams.WRAP_CONTENT;

		// 去掉默认的背景,下面两个都可以
		dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.R.color.transparent));

		// 去掉标题，否则会影响高度计算，一定要在setContentView之前调用，终于明白有一个设置theme的构造函数的目的了
		dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		dialog.setContentView(rootView);
		dialog.setOnDismissListener(new OnDismissListener() {
			@Override
			public void onDismiss(DialogInterface dialog) {
				if(isCanceledFromUser)
				onBackPressed();
			}
		});

		// 相册的点击事件
		album_layout = (RelativeLayout) rootView.findViewById(R.id.album_layout);
		album_layout.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				isCanceledFromUser = false;
				dialog.dismiss();
//				Intent intent = new Intent();
//				intent.setType("image/*");
//				intent.setAction(Intent.ACTION_GET_CONTENT);
//				startActivityForResult(Intent.createChooser(intent,"选择图片"), PICK_FROM_FILE);
				
				Intent picture = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
				startActivityForResult(picture, PICK_FROM_FILE);
			}
		});
		
		//拍照的点击事件
		camera_layout = (RelativeLayout) rootView.findViewById(R.id.camera_layout);
		camera_layout.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				isCanceledFromUser = false;
				dialog.dismiss();
				Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
				mImageCaptureUri = Uri.fromFile(new File(Environment.getExternalStorageDirectory(), "honey_"+ String.valueOf(System.currentTimeMillis()) + ".jpg"));
				intent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, mImageCaptureUri);

				try {
					intent.putExtra("return-data", true);
					startActivityForResult(intent, PICK_FROM_CAMERA);
				} catch (ActivityNotFoundException e) {
					e.printStackTrace();
				}
			}
		});
		
		// 设置默认中间
		dialog.getWindow().setGravity(Gravity.CENTER);

		dialog.show();
	}

	//选择完图片后的回调方法 统一进入裁剪界面进行裁剪处理 传递图片Uri
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode == RESULT_OK){
			switch (requestCode) {
			case PICK_FROM_CAMERA:
				Intent it1= new Intent(Activity_Enter.this, Activity_Cropper.class);
				it1.setData(mImageCaptureUri);
				startActivity(it1);
				break;

			case PICK_FROM_FILE:
				mImageCaptureUri = data.getData();
				Intent it2= new Intent(Activity_Enter.this, Activity_Cropper.class);
				it2.setData(mImageCaptureUri);
				startActivity(it2);
				break;
			}
		}
		onBackPressed();
	}
	
	@Override
	public void onBackPressed(){
		super.onBackPressed();
	}
}


