package com.example.pictureprocess.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;


import org.json.JSONArray;
import org.json.JSONObject;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

public class ACache {
	public static final int TIME_HOUR = 60 * 60;
	public static final int TIME_DAY = TIME_HOUR * 24;
	private static final int MAX_SIZE = 1000 * 1000 * 50; // 50 mb
	private static final int MAX_COUNT = Integer.MAX_VALUE; // �����ƴ����ݵ�����
	private static Map<String, ACache> mInstanceMap = new HashMap<String, ACache>();
	private ACacheManager mCache;

	public static ACache get(Context ctx) {
		return get(ctx, "ACache");
	}

	public static ACache get(Context ctx, String cacheName) {
		File f = new File(ctx.getCacheDir(), cacheName);
		return get(f, MAX_SIZE, MAX_COUNT);
	}

	public static ACache get(File cacheDir) {
		return get(cacheDir, MAX_SIZE, MAX_COUNT);
	}

	public static ACache get(Context ctx, long max_zise, int max_count) {
		File f = new File(ctx.getCacheDir(), "ACache");
		return get(f, max_zise, max_count);
	}

	public static ACache get(File cacheDir, long max_zise, int max_count) {
		ACache manager = mInstanceMap.get(cacheDir.getAbsoluteFile() + myPid());
		if (manager == null) {
			manager = new ACache(cacheDir, max_zise, max_count);
			mInstanceMap.put(cacheDir.getAbsolutePath() + myPid(), manager);
		}
		return manager;
	}

	private static String myPid() {
		return "_" + android.os.Process.myPid();
	}

	private ACache(File cacheDir, long max_size, int max_count) {
		if (!cacheDir.exists() && !cacheDir.mkdirs()) {
			throw new RuntimeException("can't make dirs in " + cacheDir.getAbsolutePath());
		}
		mCache = new ACacheManager(cacheDir, max_size, max_count);
	}

	/**
	 * Provides a means to save a cached file before the data are available.
	 * Since writing about the file is complete, and its close method is called,
	 * its contents will be registered in the cache. Example of use:
	 * 
	 * ACache cache = new ACache(this) try { OutputStream stream =
	 * cache.put("myFileName") stream.write("some bytes".getBytes()); // now
	 * update cache! stream.close(); } catch(FileNotFoundException e){
	 * e.printStackTrace() }
	 */
	class xFileOutputStream extends FileOutputStream {
		File file;

		public xFileOutputStream(File file) throws FileNotFoundException {
			super(file);
			this.file = file;
		}

		public void close() throws IOException {
			super.close();
			mCache.put(file);
		}
	}

	// =======================================
	// ============ String��� ��д ==============
	// =======================================
	/**
	 * ���� String��� �� ������
	 * 
	 * @param key
	 *            �����key
	 * @param value
	 *            �����String���
	 */
	public void put(String key, String value) {
		File file = mCache.newFile(key);
		BufferedWriter out = null;
		try {
			out = new BufferedWriter(new FileWriter(file), 1024);
			out.write(value);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (out != null) {
				try {
					out.flush();
					out.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			mCache.put(file);
		}
	}

	/**
	 * ���� String��� �� ������
	 * 
	 * @param key
	 *            �����key
	 * @param value
	 *            �����String���
	 * @param saveTime
	 *            �����ʱ�䣬��λ����
	 */
	public void put(String key, String value, int saveTime) {
		put(key, Utils.newStringWithDateInfo(saveTime, value));
	}

	/**
	 * ��ȡ String���
	 * 
	 * @param key
	 * @return String ���
	 */
	public String getAsString(String key) {
		File file = mCache.get(key);
		if (!file.exists())
			return null;
		boolean removeFile = false;
		BufferedReader in = null;
		try {
			in = new BufferedReader(new FileReader(file));
			String readString = "";
			String currentLine;
			while ((currentLine = in.readLine()) != null) {
				readString += currentLine;
			}
			if (!Utils.isDue(readString)) {
				return Utils.clearDateInfo(readString);
			} else {
				removeFile = true;
				return null;
			}
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if (removeFile)
				remove(key);
		}
	}

	// =======================================
	// ============= JSONObject ��� ��д ==============
	// =======================================
	/**
	 * ���� JSONObject��� �� ������
	 * 
	 * @param key
	 *            �����key
	 * @param value
	 *            �����JSON���
	 */
	public void put(String key, JSONObject value) {
		put(key, value.toString());
	}

	/**
	 * ���� JSONObject��� �� ������
	 * 
	 * @param key
	 *            �����key
	 * @param value
	 *            �����JSONObject���
	 * @param saveTime
	 *            �����ʱ�䣬��λ����
	 */
	public void put(String key, JSONObject value, int saveTime) {
		put(key, value.toString(), saveTime);
	}

	/**
	 * ��ȡJSONObject���
	 * 
	 * @param key
	 * @return JSONObject���
	 */
	public JSONObject getAsJSONObject(String key) {
		String JSONString = getAsString(key);
		try {
			JSONObject obj = new JSONObject(JSONString);
			return obj;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	// =======================================
	// ============ JSONArray ��� ��д =============
	// =======================================
	/**
	 * ���� JSONArray��� �� ������
	 * 
	 * @param key
	 *            �����key
	 * @param value
	 *            �����JSONArray���
	 */
	public void put(String key, JSONArray value) {
		put(key, value.toString());
	}

	/**
	 * ���� JSONArray��� �� ������
	 * 
	 * @param key
	 *            �����key
	 * @param value
	 *            �����JSONArray���
	 * @param saveTime
	 *            �����ʱ�䣬��λ����
	 */
	public void put(String key, JSONArray value, int saveTime) {
		put(key, value.toString(), saveTime);
	}

	/**
	 * ��ȡJSONArray���
	 * 
	 * @param key
	 * @return JSONArray���
	 */
	public JSONArray getAsJSONArray(String key) {
		String JSONString = getAsString(key);
		try {
			JSONArray obj = new JSONArray(JSONString);
			return obj;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	// =======================================
	// ============== byte ��� ��д =============
	// =======================================
	/**
	 * ���� byte��� �� ������
	 * 
	 * @param key
	 *            �����key
	 * @param value
	 *            ��������
	 */
	public void put(String key, byte[] value) {
		File file = mCache.newFile(key);
		FileOutputStream out = null;
		try {
			out = new FileOutputStream(file);
			out.write(value);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (out != null) {
				try {
					out.flush();
					out.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			mCache.put(file);
		}
	}

	/**
	 * Cache for a stream
	 * 
	 * @param key
	 *            the file name.
	 * @return OutputStream stream for writing data.
	 * @throws FileNotFoundException
	 *             if the file can not be created.
	 */
	public OutputStream put(String key) throws FileNotFoundException {
		return new xFileOutputStream(mCache.newFile(key));
	}

	/**
	 * 
	 * @param key
	 *            the file name.
	 * @return (InputStream or null) stream previously saved in cache.
	 * @throws FileNotFoundException
	 *             if the file can not be opened
	 */
	public InputStream get(String key) throws FileNotFoundException {
		File file = mCache.get(key);
		if (!file.exists())
			return null;
		return new FileInputStream(file);
	}

	/**
	 * ���� byte��� �� ������
	 * 
	 * @param key
	 *            �����key
	 * @param value
	 *            ��������
	 * @param saveTime
	 *            �����ʱ�䣬��λ����
	 */
	public void put(String key, byte[] value, int saveTime) {
		put(key, Utils.newByteArrayWithDateInfo(saveTime, value));
	}

	/**
	 * ��ȡ byte ���
	 * 
	 * @param key
	 * @return byte ���
	 */
	public byte[] getAsBinary(String key) {
		RandomAccessFile RAFile = null;
		boolean removeFile = false;
		try {
			File file = mCache.get(key);
			if (!file.exists())
				return null;
			RAFile = new RandomAccessFile(file, "r");
			byte[] byteArray = new byte[(int) RAFile.length()];
			RAFile.read(byteArray);
			if (!Utils.isDue(byteArray)) {
				return Utils.clearDateInfo(byteArray);
			} else {
				removeFile = true;
				return null;
			}
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		} finally {
			if (RAFile != null) {
				try {
					RAFile.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if (removeFile)
				remove(key);
		}
	}

	// =======================================
	// ============= ���л� ��� ��д ===============
	// =======================================
	/**
	 * ���� Serializable��� �� ������
	 * 
	 * @param key
	 *            �����key
	 * @param value
	 *            �����value
	 */
	public void put(String key, Serializable value) {
		put(key, value, -1);
	}

	/**
	 * ���� Serializable��ݵ� ������
	 * 
	 * @param key
	 *            �����key
	 * @param value
	 *            �����value
	 * @param saveTime
	 *            �����ʱ�䣬��λ����
	 */
	public void put(String key, Serializable value, int saveTime) {
		ByteArrayOutputStream baos = null;
		ObjectOutputStream oos = null;
		try {
			baos = new ByteArrayOutputStream();
			oos = new ObjectOutputStream(baos);
			oos.writeObject(value);
			byte[] data = baos.toByteArray();
			if (saveTime != -1) {
				put(key, data, saveTime);
			} else {
				put(key, data);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				oos.close();
			} catch (IOException e) {
			}
		}
	}

	/**
	 * ��ȡ Serializable���
	 * 
	 * @param key
	 * @return Serializable ���
	 */
	public Object getAsObject(String key) {
		byte[] data = getAsBinary(key);
		if (data != null) {
			ByteArrayInputStream bais = null;
			ObjectInputStream ois = null;
			try {
				bais = new ByteArrayInputStream(data);
				ois = new ObjectInputStream(bais);
				Object reObject = ois.readObject();
				return reObject;
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			} finally {
				try {
					if (bais != null)
						bais.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
				try {
					if (ois != null)
						ois.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return null;

	}

	// =======================================
	// ============== bitmap ��� ��д =============
	// =======================================
	/**
	 * ���� bitmap �� ������
	 * 
	 * @param key
	 *            �����key
	 * @param value
	 *            �����bitmap���
	 */
	public void put(String key, Bitmap value) {
		put(key, Utils.Bitmap2Bytes(value));
	}

	/**
	 * ���� bitmap �� ������
	 * 
	 * @param key
	 *            �����key
	 * @param value
	 *            ����� bitmap ���
	 * @param saveTime
	 *            �����ʱ�䣬��λ����
	 */
	public void put(String key, Bitmap value, int saveTime) {
		put(key, Utils.Bitmap2Bytes(value), saveTime);
	}

	/**
	 * ��ȡ bitmap ���
	 * 
	 * @param key
	 * @return bitmap ���
	 */
	public Bitmap getAsBitmap(String key) {
		if (getAsBinary(key) == null) {
			return null;
		}
		return Utils.Bytes2Bimap(getAsBinary(key));
	}

	// =======================================
	// ============= drawable ��� ��д =============
	// =======================================
	/**
	 * ���� drawable �� ������
	 * 
	 * @param key
	 *            �����key
	 * @param value
	 *            �����drawable���
	 */
	public void put(String key, Drawable value) {
		put(key, Utils.drawable2Bitmap(value));
	}

	/**
	 * ���� drawable �� ������
	 * 
	 * @param key
	 *            �����key
	 * @param value
	 *            ����� drawable ���
	 * @param saveTime
	 *            �����ʱ�䣬��λ����
	 */
	public void put(String key, Drawable value, int saveTime) {
		put(key, Utils.drawable2Bitmap(value), saveTime);
	}

	/**
	 * ��ȡ Drawable ���
	 * 
	 * @param key
	 * @return Drawable ���
	 */
	public Drawable getAsDrawable(String key) {
		if (getAsBinary(key) == null) {
			return null;
		}
		return Utils.bitmap2Drawable(Utils.Bytes2Bimap(getAsBinary(key)));
	}

	/**
	 * ��ȡ�����ļ�
	 * 
	 * @param key
	 * @return value ������ļ�
	 */
	public File file(String key) {
		File f = mCache.newFile(key);
		if (f.exists())
			return f;
		return null;
	}

	/**
	 * �Ƴ�ĳ��key
	 * 
	 * @param key
	 * @return �Ƿ��Ƴ�ɹ�
	 */
	public boolean remove(String key) {
		return mCache.remove(key);
	}

	/**
	 * ����������
	 */
	public void clear() {
		mCache.clear();
	}

	/**
	 * @title ���������
	 * @author �����michael�� www.yangfuhai.com
	 * @version 1.0
	 */
	public class ACacheManager {
		private final AtomicLong cacheSize;
		private final AtomicInteger cacheCount;
		private final long sizeLimit;
		private final int countLimit;
		private final Map<File, Long> lastUsageDates = Collections.synchronizedMap(new HashMap<File, Long>());
		protected File cacheDir;

		private ACacheManager(File cacheDir, long sizeLimit, int countLimit) {
			this.cacheDir = cacheDir;
			this.sizeLimit = sizeLimit;
			this.countLimit = countLimit;
			cacheSize = new AtomicLong();
			cacheCount = new AtomicInteger();
			calculateCacheSizeAndCacheCount();
		}

		/**
		 * ���� cacheSize��cacheCount
		 */
		private void calculateCacheSizeAndCacheCount() {
			new Thread(new Runnable() {
				@Override
				public void run() {
					int size = 0;
					int count = 0;
					File[] cachedFiles = cacheDir.listFiles();
					if (cachedFiles != null) {
						for (File cachedFile : cachedFiles) {
							size += calculateSize(cachedFile);
							count += 1;
							lastUsageDates.put(cachedFile, cachedFile.lastModified());
						}
						cacheSize.set(size);
						cacheCount.set(count);
					}
				}
			}).start();
		}

		private void put(File file) {
			int curCacheCount = cacheCount.get();
			while (curCacheCount + 1 > countLimit) {
				long freedSize = removeNext();
				cacheSize.addAndGet(-freedSize);

				curCacheCount = cacheCount.addAndGet(-1);
			}
			cacheCount.addAndGet(1);

			long valueSize = calculateSize(file);
			long curCacheSize = cacheSize.get();
			while (curCacheSize + valueSize > sizeLimit) {
				long freedSize = removeNext();
				curCacheSize = cacheSize.addAndGet(-freedSize);
			}
			cacheSize.addAndGet(valueSize);

			Long currentTime = System.currentTimeMillis();
			file.setLastModified(currentTime);
			lastUsageDates.put(file, currentTime);
		}

		private File get(String key) {
			File file = newFile(key);
			Long currentTime = System.currentTimeMillis();
			file.setLastModified(currentTime);
			lastUsageDates.put(file, currentTime);

			return file;
		}

		private File newFile(String key) {
			return new File(cacheDir, key.hashCode() + "");
		}

		private boolean remove(String key) {
			File image = get(key);
			return image.delete();
		}

		private void clear() {
			lastUsageDates.clear();
			cacheSize.set(0);
			File[] files = cacheDir.listFiles();
			if (files != null) {
				for (File f : files) {
					f.delete();
				}
			}
		}

		/**
		 * �Ƴ�ɵ��ļ�
		 * 
		 * @return
		 */
		private long removeNext() {
			if (lastUsageDates.isEmpty()) {
				return 0;
			}

			Long oldestUsage = null;
			File mostLongUsedFile = null;
			Set<Entry<File, Long>> entries = lastUsageDates.entrySet();
			synchronized (lastUsageDates) {
				for (Entry<File, Long> entry : entries) {
					if (mostLongUsedFile == null) {
						mostLongUsedFile = entry.getKey();
						oldestUsage = entry.getValue();
					} else {
						Long lastValueUsage = entry.getValue();
						if (lastValueUsage < oldestUsage) {
							oldestUsage = lastValueUsage;
							mostLongUsedFile = entry.getKey();
						}
					}
				}
			}

			long fileSize = calculateSize(mostLongUsedFile);
			if (mostLongUsedFile.delete()) {
				lastUsageDates.remove(mostLongUsedFile);
			}
			return fileSize;
		}

		private long calculateSize(File file) {
			return file.length();
		}
	}

	/**
	 * @title ʱ����㹤����
	 * @author �����michael�� www.yangfuhai.com
	 * @version 1.0
	 */
	private static class Utils {

		/**
		 * �жϻ����String����Ƿ���
		 * 
		 * @param str
		 * @return true�������� false����û�е���
		 */
		private static boolean isDue(String str) {
			return isDue(str.getBytes());
		}

		/**
		 * �жϻ����byte����Ƿ���
		 * 
		 * @param data
		 * @return true�������� false����û�е���
		 */
		private static boolean isDue(byte[] data) {
			String[] strs = getDateInfoFromDate(data);
			if (strs != null && strs.length == 2) {
				String saveTimeStr = strs[0];
				while (saveTimeStr.startsWith("0")) {
					saveTimeStr = saveTimeStr.substring(1, saveTimeStr.length());
				}
				long saveTime = Long.valueOf(saveTimeStr);
				long deleteAfter = Long.valueOf(strs[1]);
				if (System.currentTimeMillis() > saveTime + deleteAfter * 1000) {
					return true;
				}
			}
			return false;
		}

		private static String newStringWithDateInfo(int second, String strInfo) {
			return createDateInfo(second) + strInfo;
		}

		private static byte[] newByteArrayWithDateInfo(int second, byte[] data2) {
			byte[] data1 = createDateInfo(second).getBytes();
			byte[] retdata = new byte[data1.length + data2.length];
			System.arraycopy(data1, 0, retdata, 0, data1.length);
			System.arraycopy(data2, 0, retdata, data1.length, data2.length);
			return retdata;
		}

		private static String clearDateInfo(String strInfo) {
			if (strInfo != null && hasDateInfo(strInfo.getBytes())) {
				strInfo = strInfo.substring(strInfo.indexOf(mSeparator) + 1, strInfo.length());
			}
			return strInfo;
		}

		private static byte[] clearDateInfo(byte[] data) {
			if (hasDateInfo(data)) {
				return copyOfRange(data, indexOf(data, mSeparator) + 1, data.length);
			}
			return data;
		}

		private static boolean hasDateInfo(byte[] data) {
			return data != null && data.length > 15 && data[13] == '-' && indexOf(data, mSeparator) > 14;
		}

		private static String[] getDateInfoFromDate(byte[] data) {
			if (hasDateInfo(data)) {
				String saveDate = new String(copyOfRange(data, 0, 13));
				String deleteAfter = new String(copyOfRange(data, 14, indexOf(data, mSeparator)));
				return new String[] { saveDate, deleteAfter };
			}
			return null;
		}

		private static int indexOf(byte[] data, char c) {
			for (int i = 0; i < data.length; i++) {
				if (data[i] == c) {
					return i;
				}
			}
			return -1;
		}

		private static byte[] copyOfRange(byte[] original, int from, int to) {
			int newLength = to - from;
			if (newLength < 0)
				throw new IllegalArgumentException(from + " > " + to);
			byte[] copy = new byte[newLength];
			System.arraycopy(original, from, copy, 0, Math.min(original.length - from, newLength));
			return copy;
		}

		private static final char mSeparator = ' ';

		private static String createDateInfo(int second) {
			String currentTime = System.currentTimeMillis() + "";
			while (currentTime.length() < 13) {
				currentTime = "0" + currentTime;
			}
			return currentTime + "-" + second + mSeparator;
		}

		/*
		 * Bitmap �� byte[]
		 */
		private static byte[] Bitmap2Bytes(Bitmap bm) {
			if (bm == null) {
				return null;
			}
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			bm.compress(Bitmap.CompressFormat.PNG, 100, baos);
			return baos.toByteArray();
		}

		/*
		 * byte[] �� Bitmap
		 */
		private static Bitmap Bytes2Bimap(byte[] b) {
			if (b.length == 0) {
				return null;
			}
			return BitmapFactory.decodeByteArray(b, 0, b.length);
		}

		/*
		 * Drawable �� Bitmap
		 */
		private static Bitmap drawable2Bitmap(Drawable drawable) {
			if (drawable == null) {
				return null;
			}
			// ȡ drawable �ĳ���
			int w = drawable.getIntrinsicWidth();
			int h = drawable.getIntrinsicHeight();
			// ȡ drawable ����ɫ��ʽ
			Bitmap.Config config = drawable.getOpacity() != PixelFormat.OPAQUE ? Bitmap.Config.ARGB_8888 : Bitmap.Config.RGB_565;
			// ������Ӧ bitmap
			Bitmap bitmap = Bitmap.createBitmap(w, h, config);
			// ������Ӧ bitmap �Ļ���
			Canvas canvas = new Canvas(bitmap);
			drawable.setBounds(0, 0, w, h);
			// �� drawable ���ݻ���������
			drawable.draw(canvas);
			return bitmap;
		}

		/*
		 * Bitmap �� Drawable
		 */
		@SuppressWarnings("deprecation")
		private static Drawable bitmap2Drawable(Bitmap bm) {
			if (bm == null) {
				return null;
			}
			BitmapDrawable bd=new BitmapDrawable(bm);
			bd.setTargetDensity(bm.getDensity());
			return new BitmapDrawable(bm);
		}
	}

}
