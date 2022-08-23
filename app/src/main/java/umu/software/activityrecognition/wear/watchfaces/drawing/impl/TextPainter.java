package umu.software.activityrecognition.wear.watchfaces.drawing.impl;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;

import umu.software.activityrecognition.wear.watchfaces.drawing.Painter;

public class TextPainter extends Painter
{
    private String mText;

    public TextPainter(Rect mDest, String text)
    {
        super(mDest);
        setText(text);
    }

    public String getText()
    {
        return mText;
    }

    public void setText(String text)
    {
        mText = text;
    }

    @Override
    public void draw(Canvas canvas, Rect dest, Paint paint)
    {
        canvas.drawText(mText, dest.top, dest.left, paint);
    }
}
