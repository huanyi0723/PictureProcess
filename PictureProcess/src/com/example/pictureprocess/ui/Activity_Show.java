package com.example.pictureprocess.ui;

import com.example.pictureprocess.R;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;

public class Activity_Show extends Activity{

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setContentView(R.layout.zy_show_main);
		
		ImageView show = (ImageView)findViewById(R.id.show);
		
		String file_path = getIntent().getStringExtra("file_path");
		Bitmap bitmap = BitmapFactory.decodeFile(file_path);
		show.setImageBitmap(bitmap);
		
	}

	
}
