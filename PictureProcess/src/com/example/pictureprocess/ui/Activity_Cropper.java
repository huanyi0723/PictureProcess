package com.example.pictureprocess.ui;

import java.io.File;
import java.io.FileInputStream;

import com.example.pictureprocess.R;


import uk.co.senab.photoview.PhotoViewAttacher;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.provider.MediaStore.Images.ImageColumns;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.ProgressBar;
import android.widget.TextView;

//裁剪界面
public class Activity_Cropper extends Activity {

	private Uri selectedImage; // 要解析的原图片的URL

	private TextView crop_back; // 取消按钮
	private ImageView crop_img; // 放缩按钮
	private TextView crop_select; // 选取按钮
	private ProgressBar bar;// 等待框

	private int flag = 0;// 图像大小的标志变量 0是大图 1表示图像全部显示

	public static Bitmap itbmp; // 需要传递的Bitmap

	float n; // 根据宽度缩放图像的系数
	
	public static ImageView cropimage;// 显示图片控件
	private PhotoViewAttacher mAttacher; //控制图像属性
	private Bitmap bitmap;//从相片和图库中获取的原始Bitmap
	
	private float minimumScale; //图像最小放缩比例 图像高度与屏幕高度一致

	// Handle机制
	protected Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			if (msg.what == 0x111) {
				bar.setVisibility(View.GONE);
				startActivity(new Intent(Activity_Cropper.this, Activity_Edit.class));
				onBackPressed();
			}
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setContentView(R.layout.zy_crop_main);

		// 第一步 找控件
		crop_back = (TextView) findViewById(R.id.crop_back);
		crop_img = (ImageView) findViewById(R.id.crop_img);
		crop_select = (TextView) findViewById(R.id.crop_select);
		bar = (ProgressBar) findViewById(R.id.bar);
		cropimage = (ImageView) findViewById(R.id.cropimage);
		
		//获取屏幕宽度和高度
		DisplayMetrics dm = getResources().getDisplayMetrics();  
		int w = dm.widthPixels;
		int h = dm.widthPixels;

		// 设置编辑图片View的大小
		LayoutParams rlp = cropimage.getLayoutParams();
		rlp.height = w;
		rlp.width = h;
		cropimage.setLayoutParams(rlp);

		// 第二步 注册监听器
		crop_back.setOnClickListener(new CropListeners());
		crop_img.setOnClickListener(new CropListeners());
		crop_select.setOnClickListener(new CropListeners());

		//显示从上个界面传递过来的图片
		selectedImage = getIntent().getData();
		showImg1(selectedImage);

		mAttacher = new PhotoViewAttacher(cropimage);
		mAttacher.setScaleType(ScaleType.CENTER_CROP);
		
		//设置最小缩放大小
		mAttacher.setMinimumScale(minimumScale);
		
		//由于初始设置无效的问题 图像显示代码先放到这里
//		new Handler().postDelayed(new Runnable(	) {
//			
//			@Override
//			public void run() {
//				
//				cropimage.setScaleType(ImageView.ScaleType.CENTER_CROP);
//				cropimage.setImageBitmap(bitmap);
//			}
//		}, 200);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	private class CropListeners implements OnClickListener {
		@Override
		public void onClick(View v) {
			switch (v.getId()) {
			// 点击取消按钮 返回上一界面
			case R.id.crop_back:
				onBackPressed();
				break;
				
			// 点击放缩按钮
			case R.id.crop_img:
				// 图片全屏按钮 图片不可放缩
				show();
				break;
				
			// 选取按钮 点击后 截取图片 并跳转到编辑界面
			case R.id.crop_select:
				bar.setVisibility(View.VISIBLE);
				// 启动线程来执行任务
				new Thread() {
					public void run() {
						getViewBitmap();
						Message m = new Message();
						m.what = 0x111;
						mHandler.sendMessage(m);
					}
				}.start();
				break;
			}
		}
	}
	
	private void show(){
		if (flag == 0) {
			crop_img.setImageResource(R.drawable.crop_img_sma);
			flag = 1;
			mAttacher.setScaleType(ScaleType.FIT_CENTER);
			mAttacher.setZoomable(false);
//			Toast.makeText(Activity_Cropper.this, "屏幕已锁定,请按全屏键解锁",
//					Toast.LENGTH_SHORT).show();
			// 再次点击 恢复之前可以放缩状态
		} 
		else if (flag == 1) {
			crop_img.setImageResource(R.drawable.crop_img_big);
			flag = 0;
			
			//cropimage.setImageBitmap(bitmap);
			mAttacher.setScaleType(ScaleType.CENTER_CROP);
			
			//图像恢复放大缩小
			mAttacher.setZoomable(true);
		}
	}
	
	// 第一次进来时显示的方法 默认图像全部显示出来
	private void showImg1(Uri selectedImage) {
		//1 先获取图片的路径
		String bmpPath = getRealFilePath(Activity_Cropper.this,selectedImage);
		
		//android4.4上获取有问题时 用这个
		if (bmpPath == null) {
			 //4.4
			Cursor cursor = null;
			String[] column = { MediaStore.Images.Media.DATA };
			
			 String wholeID = DocumentsContract.getDocumentId(selectedImage);
			 String id = wholeID.split(":")[1];
			 String sel = MediaStore.Images.Media._ID + "=?";
			 try{
			 cursor = this.getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, column, sel, new String[] {id}, null);
			 int columnIndex = cursor.getColumnIndex(column[0]);
			 if (cursor.moveToFirst()) {
				 bmpPath = cursor.getString(columnIndex);
			  }
			 }catch(Exception e){
				 //Log.e(e.toString());
			 }finally{
				 if(cursor!=null){
					 cursor.close();
				 }
			 }
		}
		
		//2 要显示的Bitmap
		bitmap = null;
		
		try {
			int angle = 0;
			ExifInterface exif = new ExifInterface(bmpPath);
			switch (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION,ExifInterface.ORIENTATION_UNDEFINED)) {
			case ExifInterface.ORIENTATION_ROTATE_90:
				angle = 90;
				break;

			case ExifInterface.ORIENTATION_ROTATE_180:
				angle = 180;
				break;

			case ExifInterface.ORIENTATION_ROTATE_270:
				angle = 270;
				break;

			default:
				angle = ExifInterface.ORIENTATION_UNDEFINED;
				break;
			}
			bitmap = decodeFile(new File(bmpPath));//根据Path读取资源图片
			if (angle != 0) {
				// 下面的方法主要作用是把图片转一个角度，也可以放大缩小等
				Matrix m = new Matrix();
				int width = bitmap.getWidth();
				int height = bitmap.getHeight();
				m.setRotate(angle); // 旋转angle度
				bitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height, m,true);// 从新生成图片
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		Log.i("TAG", "bitmap.getWidth()" + bitmap.getWidth()); //图片宽度
		Log.i("TAG", "bitmap.getHeight()" + bitmap.getHeight()); //图片高度
		
		if (bitmap.getHeight() >= bitmap.getWidth()) {
			minimumScale = (float)bitmap.getWidth()/ (float)bitmap.getHeight();
			Log.i("TAG", "minimumScale" + minimumScale); 
		} 
		else {
			minimumScale = (float)bitmap.getHeight()/(float)bitmap.getWidth();
		}
		cropimage.setImageBitmap(bitmap);
	}
	
	
	//将Uri转换为File路径 对于4.4代码有问题 改方法废弃
	public String getRealFilePath(Context context, Uri uri) {
	    String scheme = uri.getScheme();
	    Log.i("TAG", "scheme---" + scheme);
	    String data = null;
	    if ( scheme == null )
	        data = uri.getPath();
	    else if ( ContentResolver.SCHEME_FILE.equals( scheme ) ) {
	        data = uri.getPath();
	    } else if ( ContentResolver.SCHEME_CONTENT.equals( scheme ) ) {
	        Cursor cursor = context.getContentResolver().query( uri, new String[] { ImageColumns.DATA }, null, null, null );
	        if ( null != cursor ) {
	            if ( cursor.moveToFirst() ) {
	                int index = cursor.getColumnIndex( ImageColumns.DATA );
	                if ( index > -1 ) {
	                    data = cursor.getString( index );
	                }
	            }
	            cursor.close();
	        }
	    }
	    return data;
	}
	
	public static String Uri2FileName(Uri uri, Activity act) {
		String imgPath = null;
		Cursor cursor = null;
		String[] column = { MediaStore.Images.Media.DATA };
		if(Build.VERSION.SDK_INT>=19&&DocumentsContract.isDocumentUri(act, uri)){
			 //4.4
			 String wholeID = DocumentsContract.getDocumentId(uri);
			 String id = wholeID.split(":")[1];
			 String sel = MediaStore.Images.Media._ID + "=?";
			 try{
			 cursor = act.getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, column,
			            sel, new String[] {id}, null);
			 int columnIndex = cursor.getColumnIndex(column[0]);
			 if (cursor.moveToFirst()) {
			    	imgPath = cursor.getString(columnIndex);
			  }
			 }catch(Exception e){
				 //Log.e(e.toString());
			 }finally{
				 if(cursor!=null){
					 cursor.close();
				 }
			 }
		}else{
			//4.4一下
			try {
				cursor = act.managedQuery(uri, column, null, null, null);
				int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
				cursor.moveToFirst();
				imgPath = cursor.getString(columnIndex);
			} catch (Exception e) {
				
				//Log.e(e.toString());
			} finally {
				// 4.0以上的版本会自动关闭 (4.0--14;; 4.0.3--15)
				if (Build.VERSION.SDK_INT < 14&&cursor!=null) {
					cursor.close();
				}
			}
		}
		return imgPath;
	}

	//解析出Bitmap 缩小2倍
	public Bitmap decodeFile(File file) {
		try {
			BitmapFactory.Options o = new BitmapFactory.Options();
			o.inJustDecodeBounds = true;
			BitmapFactory.decodeStream(new FileInputStream(file), null, o);

			int width_tmp = o.outWidth, height_tmp = o.outHeight;
			int scale = 2;
			
			
			BitmapFactory.Options o2 = new BitmapFactory.Options();
			o2.inSampleSize = scale;
			try {
				FileInputStream is = new FileInputStream(file);
				return BitmapFactory.decodeStream(is, null, o2);
            } catch (OutOfMemoryError err) {
				err.printStackTrace();
			}
		} 
		catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	private void getViewBitmap() {
		cropimage.setDrawingCacheEnabled(true);
		Bitmap bitmap = Bitmap.createBitmap(cropimage.getDrawingCache());

		// 清缓存
		cropimage.destroyDrawingCache();

		int w = cropimage.getWidth();
		int h = cropimage.getHeight();

		Log.i("TAG", "w" + w);
		Log.i("TAG", "h" + h);

		itbmp = Bitmap.createBitmap(bitmap, 0, 0, w, w);
	}

}
