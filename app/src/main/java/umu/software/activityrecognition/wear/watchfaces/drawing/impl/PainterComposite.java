package umu.software.activityrecognition.wear.watchfaces.drawing.impl;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;

import java.util.Queue;

import umu.software.activityrecognition.wear.watchfaces.drawing.Painter;

public class PainterComposite extends Painter
{
    private final Queue<Painter> mPainters;

    public PainterComposite(Queue<Painter> painters)
    {
        super(new Rect());
        mPainters = painters;
    }


    public Queue<Painter> getPainters()
    {
        return mPainters;
    }


    @Override
    public void accept(Canvas canvas)
    {
        super.accept(canvas);
        for (Painter p : mPainters)
            p.accept(canvas);
    }

    @Override
    public void draw(Canvas canvas, Rect dest, Paint paint)
    {
    }
}
