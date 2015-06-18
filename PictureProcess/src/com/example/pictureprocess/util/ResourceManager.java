package com.example.pictureprocess.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.graphics.Bitmap;
import android.os.Environment;

import com.example.pictureprocess.R;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;

public class ResourceManager {
	
	public static String CAMERA_ROOT = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getAbsolutePath();
	
	public static String HONEY_CACHE_DIR = Environment.getExternalStorageDirectory().getPath() + "/Honey";
	public static String IMAGE_CACHE_DIR = HONEY_CACHE_DIR + "/image";
	public static String STICKER_DIR = HONEY_CACHE_DIR + "/stickers";
	public static String RECORD_CACHE_DIR = HONEY_CACHE_DIR + "/record";
	public static String TEMP_PATH = HONEY_CACHE_DIR + "/temp";
	
	
	public static boolean isUserProfileUpdata = false;
	public static boolean isChatRoomListviewScrolling = false;
	public static boolean isContactsDataChange = false;
	
	public static Map<String, Integer>statusMaps = new HashMap<String, Integer>();
	public static boolean isDynamicPublicing = false;
	
	public static boolean isStartDynamicPublicing = false;
	
	public static boolean isNetConnected = true;
	public static boolean isNeedBeeAnim = true;
	
	public DisplayImageOptions options;
	private static ResourceManager _instance = null;
	
	public static String date_separator = "-";
	public static String sesseionid = "";
	
	public static long chatTime = 0;
	public static int chatCount = 0;
	
	public static int CAMERA_RESULTCODE = 0;
	public static int ALBUM_RESULTCODE = 1;
	
	public static int dynamics_total_count = 0;
	

}
