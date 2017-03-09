package com.bluegecko.sos.utils;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.SeekBar;

/**
 * Created by Bob on 03/07/2016
 */
public class SeekBarHint extends SeekBar {
	public String Hint = "";
	private  Context mContext;
	Bitmap thumb;
	Paint paint;
	Rect bounds;

	public SeekBarHint(Context context) {
		super(context);
		mContext = context;
	}

	public SeekBarHint(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		mContext = context;
	}

	public SeekBarHint(Context context, AttributeSet attrs) {
		super(context, attrs);
		mContext = context;
		thumb = drawableToBitmap(this.getThumb());
		Resources resources = mContext.getResources();
		float scale = resources.getDisplayMetrics().density;
		paint = new Paint();
		paint.setColor(Color.BLACK);
		paint.setTextSize(16 * scale);
		bounds = new Rect();
	}

	@Override
	protected void onDraw(Canvas c) {
		super.onDraw(c);

		paint.getTextBounds(Hint, 0, Hint.length(), bounds);

		double width = this.getWidth()-thumb.getWidth();
		int thumb_x = (int) (((double)this.getProgress()/this.getMax() ) * width);
		int x = (thumb.getWidth() - bounds.width())/2;
		int y = (thumb.getHeight() + bounds.height())/2;
		c.drawText(Hint, thumb_x + x, y, paint);
	}

	public static Bitmap drawableToBitmap (Drawable drawable) {
		Bitmap bitmap;

		if (drawable instanceof BitmapDrawable) {
			BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
			if(bitmapDrawable.getBitmap() != null) {
				return bitmapDrawable.getBitmap();
			}
		}

		if(drawable.getIntrinsicWidth() <= 0 || drawable.getIntrinsicHeight() <= 0) {
			bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888); // Single color bitmap will be created of 1x1 pixel
		} else {
			bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
		}

		Canvas canvas = new Canvas(bitmap);
		drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
		drawable.draw(canvas);
		return bitmap;
	}
}
