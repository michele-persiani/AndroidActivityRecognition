package umu.software.activityrecognition.wear.watchfaces.drawing.impl;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;

import java.util.Arrays;

import umu.software.activityrecognition.wear.watchfaces.drawing.Painter;

public class PainterChooser extends Painter
{
    private final Painter[] mPainters;
    private int mCurrent = 0;

    public PainterChooser(Rect dest, Painter... painters)
    {
        super(dest);
        mPainters = painters;
    }

    public void setPainter(int painterNum)
    {
        mCurrent = Math.max(0, Math.min(mPainters.length-1, painterNum));
    }

    @Override
    public void draw(Canvas canvas, Rect dest, Paint paint)
    {
        Arrays.stream(mPainters).forEach(painter -> {
            painter.getPaint().set(getPaint());
            painter.getDest().set(getDest());
        });
        mPainters[mCurrent].draw(canvas, dest, paint);
    }
}
