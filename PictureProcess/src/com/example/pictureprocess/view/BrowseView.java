package com.example.pictureprocess.view;

import com.example.pictureprocess.ui.Activity_Edit;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.FloatMath;
import android.view.MotionEvent;
import android.widget.ImageView;
import android.widget.Toast;

public class BrowseView extends ImageView {
	private int Img_ID;
	private float startDis;
	private PointF midPoint;
	private float oldRotation = 0;
	private float rotation = 0;
	private PointF startPoint = new PointF();
	private Matrix matrix = new Matrix();
	private Matrix currentMatrix = new Matrix();
	private Activity mActivity;
	private boolean is_Editable = false;

	boolean flag = false;

	private enum MODE {
		NONE, DRAG, ZOOM

	};

	private MODE mode = MODE.NONE;

	public BrowseView(Context context) {
		super(context);
	}

	public void setmActivity(Activity mActivity) {
		this.mActivity = mActivity;
	}

	public void setis_Editable(boolean is_Editable) {
		this.is_Editable = is_Editable;
	}

	public boolean getis_Editable() {
		return this.is_Editable;
	}

	public BrowseView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public static float distance(MotionEvent event) {
		float dx = event.getX(1) - event.getX(0);
		float dy = event.getY(1) - event.getY(0);
		return FloatMath.sqrt(dx * dx + dy * dy);
	}

	public static PointF mid(MotionEvent event) {
		float midX = (event.getX(1) + event.getX(0)) / 2;
		float midY = (event.getY(1) + event.getY(0)) / 2;
		return new PointF(midX, midY);
	}

	private float rotation(MotionEvent event) {
		double delta_x = (event.getX(0) - event.getX(1));
		double delta_y = (event.getY(0) - event.getY(1));
		double radians = Math.atan2(delta_y, delta_x);
		return (float) Math.toDegrees(radians);

	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {

		if (is_Editable == true) {
			switch (event.getAction() & MotionEvent.ACTION_MASK) {
			case MotionEvent.ACTION_DOWN:
				mode = MODE.DRAG;

				currentMatrix.set(this.getImageMatrix());
				matrix.set(currentMatrix);
				startPoint.set(event.getX(), event.getY());
				postInvalidate();

				break;

			case MotionEvent.ACTION_POINTER_DOWN:
				mode = MODE.ZOOM;
				oldRotation = rotation(event);
				startDis = distance(event);
				if (startDis > 10f) {
					midPoint = mid(event);
					currentMatrix.set(this.getImageMatrix());
				}
				break;

			case MotionEvent.ACTION_MOVE:

				if (mode == MODE.DRAG) {
					float dx = event.getX() - startPoint.x;
					float dy = event.getY() - startPoint.y;

					float deltaX = Math.abs(dx);
					float deltaY = Math.abs(dy);
					if (deltaX + deltaY < 5) {
						flag = true;
					} else {
						flag = false;
					}

					matrix.set(currentMatrix);
					matrix.postTranslate(dx, dy);
				} else if (mode == MODE.ZOOM) {
					float endDis = distance(event);
					rotation = (rotation(event) - oldRotation);
					if (endDis > 10f) {
						float scale = endDis / startDis;
						matrix.set(currentMatrix);
						matrix.postScale(scale, scale, midPoint.x, midPoint.y);
						matrix.postRotate(rotation, midPoint.x, midPoint.y);
					}
				}

				break;

			case MotionEvent.ACTION_UP:

				if (flag) {
					deleteImage();
				}

				break;
			case MotionEvent.ACTION_POINTER_UP: 
				mode = MODE.NONE;
				break;
			}
			this.setImageMatrix(matrix);
		}
		return true;

	}

	private void deleteImage() {
		AlertDialog.Builder builder = new Builder(getContext());
		builder.setTitle("删除");

		builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {

		

				Activity_Edit.edit_img.removeView(Activity_Edit.imageView);
				// EditActivity.stickerflag = true;
			}
		});
		builder.setPositiveButton("取消", new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {

			}
		});
		builder.create().show(); 
	}

}