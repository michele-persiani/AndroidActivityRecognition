package umu.software.activityrecognition.wear.watchfaces.drawing;


import android.animation.RectEvaluator;
import android.animation.TypeEvaluator;
import android.animation.ValueAnimator;
import android.graphics.Bitmap;
import android.graphics.Paint;
import android.graphics.Rect;

import androidx.annotation.Nullable;

import com.google.common.collect.Queues;

import java.util.Arrays;
import java.util.Queue;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import umu.software.activityrecognition.wear.watchfaces.drawing.impl.BitmapPainter;
import umu.software.activityrecognition.wear.watchfaces.drawing.impl.PainterChooser;
import umu.software.activityrecognition.wear.watchfaces.drawing.impl.PainterComposite;
import umu.software.activityrecognition.wear.watchfaces.drawing.impl.TextPainter;

/**
 * Factory for Painter objects
 */
public class PainterFactory
{

    public static PainterChooser newBitmapChooser(Rect dest, Bitmap... bitmap)
    {
        Painter[] painters = Arrays.stream(bitmap).map(bitmap1 -> newBitmapPainter(dest, bitmap1)).toArray(Painter[]::new);
        return newPainterChooser(dest, painters);
    }

    public static PainterChooser newPainterChooser(Rect dest, Painter... painters)
    {
        return new PainterChooser(dest, painters);
    }

    public static Painter newBitmapPainter(Rect dest, Bitmap bitmap)
    {
        return new BitmapPainter(dest, bitmap);
    }

    public static Painter newTextPainter(Rect dest, String text)
    {
        return new TextPainter(dest, text);
    }
    
    public static Painter newComposite(Painter... painters)
    {
        Queue<Painter> queue = Queues.newArrayDeque(
                Arrays.stream(painters).collect(Collectors.toList())
        );
        return new PainterComposite(queue);
    }

    /**
     *
     * @param painter Painter to animate
     * @param evaluator painter evaluator. receives values between 0 and 1
     * @param builder builder
     * @param <T> type of Painter
     * @return
     */
    public static <T extends Painter> ValueAnimator newCustomAnimator(T painter, BiConsumer<T, Float> evaluator, @Nullable Consumer<ValueAnimator> builder)
    {
        ValueAnimator animator = ValueAnimator.ofFloat(0, 1);
        animator.addUpdateListener(valueAnimator -> {
            float value = (float) valueAnimator.getAnimatedValue();
            evaluator.accept(painter, value);
        });
        if (builder != null)
            builder.accept(animator);
        return animator;
    }

    /**
     * Creates a ValueAnimator that changes a painter's paint over time, reaching the goal paint 'targetPaint'
     * @param painter
     * @param targetPaint
     * @param evaluator
     * @param builder
     * @return
     */
    public static ValueAnimator newPaintAnimator(Painter painter, Paint targetPaint, TypeEvaluator<Paint> evaluator, @Nullable Consumer<ValueAnimator> builder)
    {
        Paint currPaint = painter.getPaint();
        ValueAnimator animator = ValueAnimator.ofObject(evaluator, currPaint, targetPaint);
        animator.addUpdateListener(valueAnimator -> {
            Paint value = (Paint) valueAnimator.getAnimatedValue();
            painter.getPaint().set(value);
        });
        if (builder != null)
            builder.accept(animator);
        return animator;
    }

    /**
     * Creates a ValueAnimator that moves the drawing destination over time
     * @param painter
     * @param targetDest
     * @param builder
     * @return
     */
    public static ValueAnimator newDestAnimator(Painter painter, Rect targetDest, @Nullable Consumer<ValueAnimator> builder)
    {
        Rect currDest = painter.getDest();
        ValueAnimator animator = ValueAnimator.ofObject(new RectEvaluator(new Rect()), currDest, targetDest);
        animator.addUpdateListener(valueAnimator -> {
            Rect value = (Rect) valueAnimator.getAnimatedValue();
            painter.getDest().set(value);
        });
        if (builder != null)
            builder.accept(animator);
        return animator;
    }


    /**
     * Creates a ValueAnimator that moves the drawing destination over time
     * @param painter
     * @param destTop
     * @param destLeft
     * @param width
     * @param height
     * @param builder
     * @return
     */
    public static ValueAnimator newXYWHDestAnimator(Painter painter, int destTop, int destLeft, int width, int height, @Nullable Consumer<ValueAnimator> builder)
    {
        Rect targetDest = new Rect(destTop, destLeft, destLeft + width, destTop - height);
        return newDestAnimator(painter, targetDest, builder);
    }

    /**
     * Creates a ValueAnimator that moves the drawing destination over time, keeping its height and width fixed
     * @param painter
     * @param destTop
     * @param destLeft
     * @param builder
     * @return
     */
    public static ValueAnimator newXYAnimator(Painter painter, int destTop, int destLeft, @Nullable Consumer<ValueAnimator> builder)
    {
        Rect currDest = painter.getDest();
        return newXYWHDestAnimator(painter, destTop, destLeft, currDest.width(), currDest.height(), builder);
    }

}
