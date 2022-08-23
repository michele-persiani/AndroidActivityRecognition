package umu.software.activityrecognition.wear.watchfaces.drawing;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;

import java.util.function.Consumer;

public abstract class Painter implements Consumer<Canvas>
{
    private final Rect mDest;
    private final Paint mPaint = new Paint();

    protected Painter(Rect mDest)
    {
        this.mDest = mDest;
    }

    @Override
    public void accept(Canvas canvas)
    {
        draw(canvas, getDest(), getPaint());
    }

    public Paint getPaint()
    {
        return mPaint;
    }

    public Rect getDest()
    {
        return mDest;
    }


    public abstract void draw(Canvas canvas, Rect dest, Paint paint);
}
