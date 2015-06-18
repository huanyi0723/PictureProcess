package com.example.pictureprocess.ui;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

import com.example.pictureprocess.R;
import com.example.pictureprocess.adapter.Effect_EditAdapter;
import com.example.pictureprocess.adapter.Sticker_EditAdapter;
import com.example.pictureprocess.common.Effect;
import com.example.pictureprocess.common.LocalPhotoInfo;
import com.example.pictureprocess.common.Sticker;
import com.example.pictureprocess.util.GPUImageFilterTools;
import com.example.pictureprocess.util.GPUImageFilterTools.OnGpuImageFilterChosenListener;
import com.example.pictureprocess.util.ResourceManager;
import com.example.pictureprocess.util.Util;
import com.example.pictureprocess.view.HSuperImageView;
import com.example.pictureprocess.view.HorizontalListView;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;
import com.nostra13.universalimageloader.core.assist.FailReason;

import jp.co.cyberagent.android.gpuimage.GPUImage.OnPictureSavedListener;
import jp.co.cyberagent.android.gpuimage.GPUImageFilter;
import jp.co.cyberagent.android.gpuimage.GPUImageView;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Bitmap.Config;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup.LayoutParams;
import android.view.MotionEvent;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

public class Activity_Edit extends Activity implements OnClickListener {

	public int RequestCode = 111;

	private HorizontalListView effect_listview, sticker_listview;
	private GPUImageView effect_main; // 需要修改的图像
	private RelativeLayout tab_effect, tab_sticker, tab_sticker_library;
	private ProgressBar edit_bar;// 等待框

	private RelativeLayout pictureUrl_layout;
	private LinearLayout content_layout;
	private ImageView pictureUrl_img;
	private TextView txt_loading;
	private Button btn_ok;

	public static RelativeLayout edit_img; // 包含着编辑图片的ViewGroup
	public static HSuperImageView imageView; // 需要添加的贴纸

	private GPUImageFilter mFilter;
	// private FilterAdjuster mFilterAdjuster;

	public static ArrayList<HSuperImageView> sticklist; // 保存贴纸图片的集合
	private int sticknum = -1;// 贴纸添加的序号

	public static Bitmap editbmp; // 编辑界面的贴纸图片

	private Effect_EditAdapter eAdapter;
	private Sticker_EditAdapter sAdapter;

	private ArrayList<Sticker> stickerList = new ArrayList<Sticker>();
	private ArrayList<Sticker> stickerTempList = new ArrayList<Sticker>(); // 贴纸列表

	private int color_selected = 0xffffd83a;
	private int color_unselected = 0xffededed;

	// private AnimationController mController;

	public static Sticker mSticker = null; // 从贴纸库过来的贴纸

	protected final int GETSTICKER_SUCC = 0;
	protected final int DOWNLOADSTICKER_SUCC = 1;
	protected Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			switch (msg.what) {
			case GETSTICKER_SUCC:
				stickerList.clear();
				stickerList.addAll(stickerTempList);
				sAdapter.notifyDataSetChanged();
				break;

			case DOWNLOADSTICKER_SUCC:
				showImg(msg.arg1, (HSuperImageView) msg.obj);
				break;
			}
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		/*requestWindowFeature(Window.FEATURE_ACTION_BAR);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);*/
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setContentView(R.layout.zy_edit_main);

		/*
		 * initMainView();
		 * getSupportActionBar().setDisplayShowHomeEnabled(true);
		 * getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		 * getSupportActionBar().setIcon(R.drawable.null_icon_small);
		 * getSupportActionBar().setLogo(R.drawable.null_icon_small);
		 * getSupportActionBar().setTitle("编辑图片");
		 */

		sticklist = new ArrayList<HSuperImageView>();

		initView();
		initControlView();
		initPictureView();

		getStickerList();
	}

	private void initView() {
		tab_effect = (RelativeLayout) findViewById(R.id.tab_effect);
		tab_sticker = (RelativeLayout) findViewById(R.id.tab_sticker);
		tab_sticker_library = (RelativeLayout) findViewById(R.id.tab_sticker_library);
		tab_effect.setOnClickListener(this);
		tab_sticker.setOnClickListener(this);
		tab_sticker_library.setOnClickListener(this);

		edit_img = (RelativeLayout) findViewById(R.id.edit_img);
		edit_bar = (ProgressBar) findViewById(R.id.edit_bar);

		// 获取屏幕宽度和高度
		DisplayMetrics dm = getResources().getDisplayMetrics();
		int screenWidth = dm.widthPixels;
		int screenHeight = dm.widthPixels;

		RelativeLayout.LayoutParams rlp = (RelativeLayout.LayoutParams) edit_img.getLayoutParams();
		rlp.height = screenWidth;
		rlp.width = screenWidth;
		edit_img.setLayoutParams(rlp);

		// 点击屏幕背景 将所有贴纸的外框全部去掉
		edit_img.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				HSuperImageView.stickflag = false;
				// 贴纸对象全部存在 没有回收 后期对这个问题再进行优化
				for (int i = 0; i < sticklist.size(); i++) {
					System.out.println(sticklist.get(i));
					sticklist.get(i).invalidate();
				}
				return false;
			}
		});

		imageView = new HSuperImageView(this);
		effect_main = (GPUImageView) findViewById(R.id.effect_main);
		effect_main.setImage(Activity_Cropper.itbmp);
	}

	private void initControlView() {
		effect_listview = (HorizontalListView) findViewById(R.id.effect_listview);
		sticker_listview = (HorizontalListView) findViewById(R.id.sticker_listview);

		eAdapter = new Effect_EditAdapter(this, getEffects());
		sAdapter = new Sticker_EditAdapter(this, stickerList);

		effect_listview.setAdapter(eAdapter);
		sticker_listview.setAdapter(sAdapter);

		effect_listview.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				eAdapter.setSelectItem(arg2);

				final int tmpint = arg2;
				final int tmpitem = arg1.getWidth();
				new Handler().postDelayed(new Runnable() {
					@Override
					public void run() {
						effect_listview.scrollTo(tmpitem * (tmpint - 1) - tmpitem / 4);
					}
				}, 200);

				if (arg2 > 0) {
					// 里面与滤镜相关的图片一定要放到指定文件夹drawable-nodpi 否则效果会有区别 这是一个坑
					GPUImageFilterTools.showDialog(Activity_Edit.this, new OnGpuImageFilterChosenListener() {
						@Override
						public void onGpuImageFilterChosenListener(GPUImageFilter filter) {
							switchFilterTo(filter);
							effect_main.requestRender();
						}
					}, arg2 - 1);
				} else {
				}
			}
		});

		sticker_listview.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				showPicture(arg2);
			}
		});
	}

	private void initPictureView() {
		// mController = new AnimationController();

		pictureUrl_layout = (RelativeLayout) findViewById(R.id.pictureUrl_layout);
		content_layout = (LinearLayout) findViewById(R.id.content_layout);
		pictureUrl_img = (ImageView) findViewById(R.id.pictureUrl_img);
		txt_loading = (TextView) findViewById(R.id.txt_loading);
		btn_ok = (Button) findViewById(R.id.btn_ok);

		int w = Util.getscreenwidth(getApplicationContext()) * 3 / 4;
		RelativeLayout.LayoutParams rlp = (android.widget.RelativeLayout.LayoutParams) content_layout.getLayoutParams();
		rlp.width = w;
		content_layout.setLayoutParams(rlp);

		LayoutParams lp = pictureUrl_img.getLayoutParams();
		lp.height = w;
		lp.width = w;
		pictureUrl_img.setLayoutParams(lp);

		pictureUrl_layout.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// mController.upOut(pictureUrl_layout, 300, 0);
			}
		});
	}

	private void showPicture(final int position) {
		// mController.upIn(pictureUrl_layout, 300, 0);
		/*DisplayImageOptions options = new DisplayImageOptions.Builder().resetViewBeforeLoading(true)
				.cacheInMemory(true).cacheOnDisk(true).imageScaleType(ImageScaleType.EXACTLY)
				.bitmapConfig(Bitmap.Config.RGB_565).build();
		ImageLoader.getInstance().displayImage(stickerList.get(position).pictureUrl, pictureUrl_img, options,
				new SimpleImageLoadingListener() {
					@Override
					public void onLoadingStarted(String imageUri, View view) {
						pictureUrl_img.setBackgroundColor(0x55535353);
						txt_loading.setVisibility(View.VISIBLE);
						txt_loading.setText("加载中...");
					}

					@Override
					public void onLoadingFailed(String imageUri, View view, FailReason failReason) {
						pictureUrl_img.setBackgroundColor(0x55535353);
						txt_loading.setText("加载失败!");
					}

					@Override
					public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
						txt_loading.setVisibility(View.GONE);
					}

					@Override
					public void onLoadingCancelled(String imageUri, View view) {
						pictureUrl_img.setBackgroundColor(0x55535353);
						txt_loading.setText("已取消加载");
					}
				});

		btn_ok.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				setSticker(position);
				// mController.upOut(pictureUrl_layout, 300, 0);
			}
		});*/
		
		setSticker(position);
	}

	// 添加贴纸图片到编辑图片中
	private void setSticker(int postion) {
		HSuperImageView.stickflag = true;
		sticknum++;
		HSuperImageView imageView = new HSuperImageView(Activity_Edit.this, sticknum);
		setImg(postion, imageView);
		sticklist.add(imageView);
		edit_img.addView(imageView, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
	}

	// 滤镜的列表项赋值
	/*
	 * 滤镜排序 1. Amaro 罗马夏日 2. Hudson富士山下 3. Valencia塞纳河畔 4. TonCurve 光辉岁月 5.
	 * Sierra 自由 6. Hefe回忆 7. Rise离人醉 8. Earlybird日落大道 9. Brannan秋意浓 10.
	 * Inkwell黑白故事 11. Walden罗曼蒂克 12. Xproll不羁 13. Lookup暗里着迷
	 */
	private List<Effect> getEffects() {
		List<Effect> list = new ArrayList<Effect>();

		list.add(new Effect("原图", R.drawable.effect_original));
		list.add(new Effect("罗马夏日", R.drawable.effect_amaro));
		list.add(new Effect("富士山下", R.drawable.effect_hudson));
		list.add(new Effect("塞纳河畔", R.drawable.effect_valencia));
		list.add(new Effect("光辉岁月", R.drawable.effect_toncurve));
		list.add(new Effect("自由", R.drawable.effect_sierra));
		list.add(new Effect("回忆", R.drawable.effect_hefe));
		list.add(new Effect("离人醉", R.drawable.effect_rise));
		list.add(new Effect("日落大道", R.drawable.effect_earlybird));
		list.add(new Effect("秋意浓", R.drawable.effect_brannan));
		list.add(new Effect("黑白故事", R.drawable.effect_inkwell));
		list.add(new Effect("罗曼蒂克", R.drawable.effect_walden));
		list.add(new Effect("不羁", R.drawable.effect_xproll));
		list.add(new Effect("暗里着迷", R.drawable.effect_lookup));

		return list;
	}

	// 贴纸的列表项赋值
	private void getStickerList() {
		/*new Thread() {
			public void run() {
				stickerTempList = new ArrayList<Sticker>();
				int random = 1 + (int) (Math.random() * 3);
				//boolean flag = QueryUtils.getPastersTop(random, 8, stickerTempList);
				if (false) {
					for (int i = 0; i < stickerTempList.size(); i++) {
						//Utils.downLoadFile(stickerTempList.get(i), stickerTempList.get(i).pasterUrl);
					}
					handler.sendEmptyMessage(GETSTICKER_SUCC);
				}
			}
		}.start();*/
		
		//本地贴纸
		stickerList.add(new Sticker(R.drawable.sticker1));
		stickerList.add(new Sticker(R.drawable.sticker2));
		stickerList.add(new Sticker(R.drawable.sticker3));
		stickerList.add(new Sticker(R.drawable.sticker4));
		stickerList.add(new Sticker(R.drawable.sticker5));
		stickerList.add(new Sticker(R.drawable.sticker6));
		stickerList.add(new Sticker(R.drawable.sticker7));
	}

	@Override
	public void onStop() {
		super.onStop();
	}

	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.tab_effect:
			effect_listview.setVisibility(View.VISIBLE);
			sticker_listview.setVisibility(View.INVISIBLE);
			tab_effect.setBackgroundColor(color_selected);
			tab_sticker.setBackgroundColor(color_unselected);
			break;

		case R.id.tab_sticker:
			effect_listview.setVisibility(View.INVISIBLE);
			sticker_listview.setVisibility(View.VISIBLE);
			tab_effect.setBackgroundColor(color_unselected);
			tab_sticker.setBackgroundColor(color_selected);
			break;

		case R.id.tab_sticker_library:
			/*
			 * Intent intent1 = new Intent(Activity_Edit.this,
			 * Activity_StickerLib.class); startActivityForResult(intent1,
			 * RequestCode);
			 */
			next();
			
			break;
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		if (mSticker != null) {
			// addSticker(mSticker);
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// super.onActivityResult(requestCode, resultCode, data);
		if (resultCode == Activity.RESULT_OK) {
			if (requestCode == RequestCode) {
				// String url = data.getStringExtra("STICKER");
				// addSticker(url);
			}
		}
	}

	// 从贴纸库界面跳转过来 增加贴纸
	/*
	 * private void addSticker(final Sticker mSticker) {
	 * Utils.downLoadFile(mSticker, mSticker.pasterUrl); File file = new
	 * File(mSticker.pasterLocalpath); Bitmap bmp = Util.decodeFile(file,
	 * Util.getscreenwidth(getApplicationContext())); if (bmp != null) {
	 * imageView.init(bmp);// 设置控件图片 sticknum++; //贴纸个数要增加
	 * sticklist.add(imageView); edit_img.addView(imageView, new LayoutParams(
	 * LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)); } else
	 * Toast.makeText(this, "加载贴纸失败", Toast.LENGTH_SHORT).show(); }
	 */

	/*
	 * @Override public boolean onCreateOptionsMenu(Menu menu) { menu.clear();
	 * menu.add(0, MenuInfo.TOPMENU_NEXT, 0, "下一步").setShowAsAction(
	 * MenuItem.SHOW_AS_ACTION_ALWAYS); return true; }
	 */

	/*
	 * @Override public boolean onOptionsItemSelected(final MenuItem item) {
	 * switch (item.getItemId()) { case MenuInfo.TOPMENU_NEXT: next(); break;
	 * 
	 * case android.R.id.home: onBackPressed(); break; } return true; }
	 */

	//点击下一步后进入的界面
	private void next() {
		HSuperImageView.stickflag = false;
		edit_bar.setVisibility(View.VISIBLE);
		getViewBitmap();

		// 隐蔽所有贴纸
		for (int i = 0; i < sticklist.size(); i++) {
			System.out.println(sticklist.get(i));
			sticklist.get(i).setVisibility(View.INVISIBLE);
		}
	}

	private void setImg(final int position, final HSuperImageView imageView) {
		File file = new File(stickerList.get(position).pasterLocalpath);
		if (file.exists()) {
			showImg(position, imageView);
		} else {
			new Thread() {
				public void run() {
					//Utils.downLoadFile(stickerList.get(position), stickerList.get(position).pasterUrl);
					Message msg = new Message();
					msg.what = DOWNLOADSTICKER_SUCC;
					msg.arg1 = position;
					msg.obj = imageView;
					handler.sendMessage(msg);
				}
			}.start();
		}
	}

	private void showImg(int position, HSuperImageView imageView) {
		//File file = new File(stickerList.get(position).pasterLocalpath);
		//Bitmap bmp = Util.decodeFile(file, Util.getscreenwidth(getApplicationContext()));
		Bitmap bmp = BitmapFactory.decodeResource(getResources(), stickerList.get(position).drawableId);
		
		if (bmp != null)
			imageView.init(bmp);// 设置控件图片
		else
			Toast.makeText(this, "加载贴纸失败", Toast.LENGTH_SHORT).show();
	}

	// 设置Filter啥玩意啊 应该是给图像上滤镜吧 里面的原理也不太清楚
	private void switchFilterTo(final GPUImageFilter filter) {
		if (mFilter == null || (filter != null && !mFilter.getClass().equals(filter.getClass()))) {
			mFilter = filter;
			effect_main.setFilter(mFilter);
			// mFilterAdjuster = new FilterAdjuster(mFilter);
		}
	}

	// 由于GPUImageView是继承自GLSurfaceView 普通的截取无效 只能分别截取 再进行合成才行
	private void getViewBitmap() {

		// edit_bar.setVisibility(View.INVISIBLE);

		// 得到上面的贴纸图片
		edit_img.setDrawingCacheEnabled(true);
		Bitmap bitmap = Bitmap.createBitmap(edit_img.getDrawingCache());

		// 清缓存
		edit_img.destroyDrawingCache();

		int w = edit_img.getWidth();

		editbmp = Bitmap.createBitmap(bitmap, 0, 0, w, w);

		// 保存底部效果图
		String fileName = System.currentTimeMillis() + ".jpg";
		effect_main.saveToPictures("逗萌", fileName, new MyListeners());
	}

	public class MyListeners implements OnPictureSavedListener {
		@Override
		public void onPictureSaved(String path, Uri uri) {
			edit_bar.setVisibility(View.INVISIBLE);
			saveBitmap(path, uri);
		}
	}

	private void saveBitmap(String path, Uri uri) {
		Bitmap bmp = null;
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = false;
		try {
			bmp = BitmapFactory.decodeStream(getContentResolver().openInputStream(uri), null, options);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		// 图像的合成 将第2个Bitmap画到第一个Bitmap上面
		Bitmap bitmap = Bitmap.createBitmap(bmp.getWidth(), bmp.getHeight(), Config.ARGB_8888);
		Canvas cv = new Canvas(bitmap);
		cv.drawBitmap(bmp, 0, 0, null);
		cv.drawBitmap(editbmp, 0, 0, null);

		saveImage(bitmap);

		bmp.recycle();
		new File(path).delete();
		MediaScannerConnection.scanFile(Activity_Edit.this, new String[] { new File(path).toString() }, null, null);
	}

	private void saveImage(final Bitmap image) {
		String fileName = System.currentTimeMillis() + ".jpg";
		File parentpath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
		File file = new File(parentpath, "逗萌/" + fileName);
		file.getParentFile().mkdirs();
		try {
			image.compress(CompressFormat.JPEG, 80, new FileOutputStream(file));
			MediaScannerConnection.scanFile(Activity_Edit.this, new String[] { file.toString() }, null, null);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		if (file.exists()) {
			LocalPhotoInfo mLocalPhotoInfo = new LocalPhotoInfo(file.getAbsolutePath(), "file://"
					+ file.getAbsolutePath());
			//ResourceManager.bitmapsList.add(mLocalPhotoInfo);
		} else {
			Toast.makeText(Activity_Edit.this, "获取图片失败", Toast.LENGTH_SHORT).show();
		}
		image.recycle();
		/*
		 * startActivity(new Intent(Activity_Edit.this,
		 * Activity_Dynamic_create.class)); Activity_Edit.this.onBackPressed();
		 */
		Intent intent = new Intent(Activity_Edit.this, Activity_Show.class);
		intent.putExtra("file_path", file.getAbsolutePath());
		startActivity(intent);
	}

	@Override
	public void onBackPressed() {
		super.onBackPressed();
		overridePendingTransition(android.R.anim.fade_in, android.R.anim.slide_out_right);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (Activity_Cropper.itbmp != null)
			Activity_Cropper.itbmp.recycle();
	}

}
