package com.example.pictureprocess.adapter;

import java.io.File;
import java.util.ArrayList;

import com.example.pictureprocess.R;
import com.example.pictureprocess.common.Sticker;
import com.example.pictureprocess.util.Util;
import com.nostra13.universalimageloader.core.ImageLoader;

import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class Sticker_EditAdapter extends BaseAdapter {

	private LayoutInflater mInflater;
	//贴纸的个数
	private ArrayList<Sticker> mData;
	private Context mContext;

	public Sticker_EditAdapter(Context context, ArrayList<Sticker> mData) {
		this.mContext = context;
		this.mInflater = LayoutInflater.from(mContext);
		this.mData = mData;
	}

	@Override
	public int getCount() {
		return mData.size();
	}

	@Override
	public Object getItem(int position) {
		return position;
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder holder = new ViewHolder();
			if(convertView == null){
				convertView = mInflater.inflate(R.layout.zy_sticker_item, null);
				holder.img = (ImageView) convertView.findViewById(R.id.sticker_img);
				holder.stickerlib_img = (ImageView) convertView.findViewById(R.id.stickerlib_img);
				holder.sticker_layout = (RelativeLayout)convertView.findViewById(R.id.sticker_layout);
				holder.pro_bar = (ProgressBar)convertView.findViewById(R.id.pro_bar);
				
				convertView.setTag(holder);
			}
			else{
				holder = (ViewHolder) convertView.getTag();
				//ImageLoader.getInstance().cancelDisplayTask(holder.img);
			}
			
			Sticker mSticker = mData.get(position);

			holder.img.setVisibility(View.VISIBLE);
			holder.stickerlib_img.setVisibility(View.GONE);
			holder.sticker_layout.setBackgroundColor(Color.BLACK);
			holder.pro_bar.setVisibility(View.GONE);
			
			/*File file = new File(mSticker.pasterLocalpath);
			if(file.exists())
				holder.img.setImageBitmap(Util.decodeFile(file, Util.getscreenwidth(mContext)));
			else
				downLoadFile(mSticker,holder);*/
			holder.img.setImageResource(mSticker.drawableId);
			
			return convertView;
	}
	
	private void downLoadFile(final Sticker mSticker,final ViewHolder holder){
		holder.pro_bar.setVisibility(View.VISIBLE);
		new Thread(){
			public void run(){
				//boolean flag = Utils.downLoadFile(mSticker,mSticker.pasterUrl);
				if(true){
					Message msg = new Message();
					msg.what = GETSTICKER_SUCC;
					msg.obj = new HandlerBody(mSticker,holder);
					handler.sendMessage(msg);
				}
			}
		}.start();
	}
	
	protected final int GETSTICKER_SUCC = 0;
	protected Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			switch (msg.what) {				
			case GETSTICKER_SUCC:
				HandlerBody mBody = (HandlerBody) msg.obj;
				File file = new File(mBody.mSticker.pasterLocalpath);
				mBody.holder.pro_bar.setVisibility(View.GONE);
				mBody.holder.img.setImageBitmap(Util.decodeFile(file, Util.getscreenwidth(mContext)));
				break;
			}
		}
	};
	
	class HandlerBody{
		Sticker mSticker;
		ViewHolder holder;
		
		public HandlerBody(Sticker mSticker,ViewHolder holder){
			this.mSticker = mSticker;
			this.holder = holder;
		}
	}

   class ViewHolder {
		public ProgressBar pro_bar;
		public RelativeLayout sticker_layout;
		public ImageView stickerlib_img;
		public ImageView img; // 图像
		public TextView title;// 标题
	}
}



