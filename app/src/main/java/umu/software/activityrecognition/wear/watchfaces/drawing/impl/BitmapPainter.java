package umu.software.activityrecognition.wear.watchfaces.drawing.impl;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;

import umu.software.activityrecognition.wear.watchfaces.drawing.Painter;

public class BitmapPainter extends Painter
{
    private final Bitmap mBitmap;


    public BitmapPainter(Rect dest, Bitmap bitmap)
    {
        super(dest);
        mBitmap = bitmap;
    }


    @Override
    public void draw(Canvas canvas, Rect dest, Paint paint)
    {
        Rect size = new Rect(0, 0, mBitmap.getWidth(), mBitmap.getHeight());
        canvas.drawBitmap(mBitmap, size, dest, paint);
    }

}
