package umu.software.activityrecognition.wear.watchfaces;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.Bundle;

import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.util.Pair;
import android.view.SurfaceHolder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.IntStream;


public class ActivityWatchFace extends CanvasWatchFaceService implements Supplier<Map<String, Float>>
{
    Engine mEngine = null;

    float[] mDummyValues = new float[]{.7f, 1.99f, .1f, .7f, .05f};
    String[] mDummyActivities = new String[]{"Have coffee", "Jogging", "Chit chat", "Eating", "Working"};


    @Override
    public Engine onCreateEngine() {
        mEngine = new Engine(this);
        mEngine.setWatchFaceStyle(new WatchFaceStyle.Builder(this)
                .setAcceptsTapEvents(true)
                .build());
        mEngine.startUpdateThread();
        return mEngine;
    }

    @Override
    public Map<String, Float> get()
    {
        Map<String, Float> activities = new HashMap<>();
        for (int i = 0; i < mDummyValues.length; i++)
        {
            float diff = (float) ((Math.random() - 0.5) * 1e-2);
            mDummyValues[i] = Math.max(mDummyValues[i] + diff, 0);
            activities.put(mDummyActivities[i], mDummyValues[i]);
        }
        return activities;
    }


    private class Engine extends CanvasWatchFaceService.Engine
    {
        Map<String, Paint> mSlicesPaint = new HashMap<>();

        ReentrantLock mLock = new ReentrantLock();

        /* Activities and values */
        float[] mValues = new float[]{};
        String[] mActivities = new String[]{};


        /* Text */
        int mTextSize = 26;


        /* Colors */
        int[] mActivitiesColor = new int[]{
                Color.argb(255, 61, 93, 154), //blu
                Color.argb(255, 245, 131, 77), //orange
                Color.argb(255, 245, 116, 97), // red
                Color.argb(255, 255, 226, 0),  // yellow
                Color.argb(255, 141, 236, 120), //green
                Color.argb(255, 199, 145, 206), //purple
        };
        int mBackgroundColor = Color.argb(255, 19, 27, 29);
        int mLinesColor = Color.argb(255, 220, 255, 251);


        /* Taps and rotations */
        boolean mTapping = false;
        long mTapTime = 0L;
        long mRotateAfter = 1000L;
        float mOffsetAngle = 0;

        /* Value updates */
        long mUpdateTimeMillis = 50;
        boolean mUpdating = true;
        private Thread mUpdateThread = null;

        Supplier<Map<String, Float>> mActivitiesCallable;


        Engine(Supplier<Map<String, Float>> activitiesCallable)
        {
            mActivitiesCallable = activitiesCallable;
        }

        public void stopUpdateThread()
        {
            if (mUpdateThread == null)
                return;
            mUpdating = false;
            mUpdateThread.interrupt();
        }

        public void startUpdateThread()
        {
            stopUpdateThread();
            mUpdating = true;
            mUpdateThread = new Thread(() -> {
                while (mUpdating) {
                    if (Thread.interrupted())
                        return;
                    try {
                        Thread.sleep(mUpdateTimeMillis);
                    } catch (InterruptedException e) {
                        return;
                    }
                    update();
                }
            });
            mUpdateThread.start();
        }

        @Override
        public void onTapCommand(int tapType, int x, int y, long eventTime)
        {
            switch (tapType) {
                case WatchFaceService.TAP_TYPE_TOUCH:
                    mTapping = true;
                    mTapTime = System.currentTimeMillis();
                    break;
                case WatchFaceService.TAP_TYPE_TAP:
                case WatchFaceService.TAP_TYPE_TOUCH_CANCEL:
                    mTapping = false;
                    mTapTime = 0;
                    break;
                default:
                    super.onTapCommand(tapType, x, y, eventTime);
                    break;
            }
        }


        public void setActivities(Map<String, Float> activities, boolean sorted)
        {
            List<Pair<String, Float>> activityList = new ArrayList<>();

            for (String a : activities.keySet())
                activityList.add(Pair.create(a, activities.get(a)));

            if (sorted)
                activityList.sort(Comparator.comparing(p -> -p.second));
            String[] activitiesNames = new String[activityList.size()];
            float[] values = new float[activityList.size()];
            for (int i = 0; i < activityList.size(); i++)
            {
                activitiesNames[i] = activityList.get(i).first;
                values[i] = activityList.get(i).second;
            }
            mLock.lock();
            mValues = values;
            mActivities = activitiesNames;
            mLock.unlock();
            invalidate();
        }

        protected String[] getActivitiesNames()
        {
            return mActivities;
        }


        protected float[] getUnnormalizedValues()
        {
            mLock.lock();
            float[] v = Arrays.copyOf(mValues, mValues.length);
            mLock.unlock();
            return v;
        }

        protected float[] getNormalizedValues()
        {
            mLock.lock();
            float[] v = getUnnormalizedValues();
            float sum = 0;
            for (int i = 0; i < v.length; i++)
                sum += v[i];

            for (int i = 0; i < v.length; i++)
                v[i] /= sum;
            mLock.unlock();
            return v;
        }

        protected void forEachValue(BiConsumer<Integer, Float> consumer)
        {
            float[] v = getNormalizedValues();

            float[] finalV = v;
            int[] sortedIndices = IntStream.range(0, v.length)
                    .boxed().sorted((i, j) -> -Float.compare(finalV[i], finalV[j]))
                    .mapToInt(ele -> ele).toArray();

            v = Arrays.copyOf(v, v.length);
            for (int sortedIndex : sortedIndices)
                consumer.accept(sortedIndex, v[sortedIndex]);
        }

        @Override
        public void onCreate(SurfaceHolder holder)
        {
            super.onCreate(holder);
        }

        @Override
        public void onPropertiesChanged(Bundle properties)
        {
            super.onPropertiesChanged(properties);
        }

        @Override
        public void onTimeTick()
        {
            super.onTimeTick();
            update();
        }


        protected void update()
        {
            if (mActivitiesCallable == null)
                return;
            Map<String, Float> activities = mActivitiesCallable.get();
            setActivities(activities, true);
            invalidate();
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode)
        {
            super.onAmbientModeChanged(inAmbientMode);
            if (inAmbientMode)
                stopUpdateThread();
            else
                startUpdateThread();
        }

        @Override
        public void onVisibilityChanged(boolean visible)
        {
            super.onVisibilityChanged(visible);
            if (visible)
                startUpdateThread();
            else
                stopUpdateThread();
        }


        @Override
        public void onDraw(Canvas canvas, Rect bounds)
        {
            if (mTapping && (System.currentTimeMillis() - mTapTime) > mRotateAfter)
            {
                mOffsetAngle += 1;
                mOffsetAngle = mOffsetAngle % 360;
                invalidate();
            }
            drawCircle(canvas, bounds, 1.f, paint -> paint.setColor(mBackgroundColor));

            int l = getNormalizedValues().length;
            forEachValue((i, v) -> {
                String activity = getActivitiesNames()[i];
                setActivityPaint(activity, (paint -> paint.setColor(mActivitiesColor[mSlicesPaint.size()])));
                drawActivitySlice(canvas, bounds, i, l, paint -> {
                    paint.setAlpha((int) (255* Math.pow(v, 1./4)));
                    paint.setShader(new RadialGradient(
                            (float)bounds.centerX(),
                            (float)bounds.centerY(),
                            (float) (Math.max(1, Math.min(bounds.width(), bounds.height()) * deformValue(v)/2)),
                            new int[]{paint.getColor(), Color.TRANSPARENT},
                            new float[]{.96f, 1.f},
                            Shader.TileMode.MIRROR )
                    );
                });
                drawActivityText(canvas, bounds, i, l);
            });


            drawCircle(canvas, bounds, .75f, paint -> {
                paint.setColor(mLinesColor);
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(1f);
                paint.setPathEffect(new DashPathEffect(new float[]{5, 20}, 0));
            });

            drawCircle(canvas, bounds, .5f, paint -> {
                paint.setColor(mLinesColor);
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(2f);
            });

            drawCircle(canvas, bounds, .25f, paint -> {
                paint.setColor(mLinesColor);
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(1f);
                paint.setPathEffect(new DashPathEffect(new float[]{5, 15}, 0));
            });


            float percSizeInner = .10f;
            drawCircle(canvas, bounds, percSizeInner, paint -> {
                paint.setColor(mBackgroundColor);
                paint.setShader(new RadialGradient(
                        (float)bounds.centerX(),
                        (float)bounds.centerY(),
                        (float) Math.max(1, Math.min(bounds.width(), bounds.height()) * deformValue(percSizeInner)/2),
                        new int[]{mBackgroundColor, Color.TRANSPARENT},
                        new float[]{.4f, .44f},
                        Shader.TileMode.MIRROR )
                );

            });

            drawCircle(canvas, bounds, 1.f, paint -> {
                paint.setColor(mBackgroundColor);
                paint.setShader(new RadialGradient(
                        (float)bounds.centerX(),
                        (float)bounds.centerY(),
                        (float) (Math.min(bounds.width(), bounds.height()) /2),
                        new int[]{Color.TRANSPARENT, mBackgroundColor},
                        new float[]{.96f, 1.f},
                        Shader.TileMode.MIRROR )
                );

            });


        }

        private float deformValue(float value)
        {
            return (float) Math.pow(value, 1/3.);
        }


        private void drawCircle(Canvas canvas, Rect bounds, float percSize, Consumer<Paint> paintBuilder)
        {
            Paint paint = newPaint();
            paintBuilder.accept(paint);
            canvas.drawCircle(
                    bounds.centerX(),
                    bounds.centerY(),
                    (float) (Math.min(bounds.width(), bounds.height()) * deformValue(percSize)/2),
                    paint
            );
        }


        private void drawActivitySlice(Canvas canvas, Rect bounds, int sliceNum, int numSlices, Consumer<Paint> paintBuilder)
        {
            float value = deformValue(getNormalizedValues()[sliceNum]);
            String activity = getActivitiesNames()[sliceNum];
            Paint paint = mSlicesPaint.get(activity);
            paintBuilder.accept(paint);
            RectF oval = getOval(bounds, value);
            float angle = 360 / numSlices;
            float startAngle = sliceNum * angle + mOffsetAngle;
            canvas.drawArc(oval, startAngle, angle, true, paint);

        }

        private void drawActivityText(Canvas canvas, Rect bounds, int sliceNum, int numSlices)
        {
            float angle = 360f / numSlices;
            float startAngle = sliceNum * angle + mOffsetAngle;
            float rotationAngle = startAngle + angle/2;
            boolean flipCanvas = (270 >= (rotationAngle%360) && (rotationAngle%360) >= 90);
            float adjv = flipCanvas? -1 : 1;
            float adjv0 = flipCanvas? 1 : 0;
            String text = getActivitiesNames()[sliceNum];
            Paint textPaint = newPaint();
            textPaint.setColor(mLinesColor);
            textPaint.setTextSize(mTextSize);
            Rect textBounds = new Rect();

            textPaint.getTextBounds(text, 0, text.length(), textBounds);

            int text_height =  textBounds.height();
            int text_width =  textBounds.width();

            canvas.save();
            canvas.translate(bounds.centerX(), bounds.centerY());
            canvas.rotate(rotationAngle);
            if (flipCanvas)
                canvas.scale(-1, -1);
            Paint.FontMetrics fm = textPaint.getFontMetrics();
            canvas.drawText(
                    text,
                    (bounds.width()* 0.15f) * adjv - adjv0 * text_width,
                     (text_height/2),
                    textPaint
            );
            canvas.restore();
        }

        private RectF getOval(Rect bounds, float perc)
        {
            float lx = (bounds.right - bounds.left);
            float ly = (bounds.bottom - bounds.top);
            float dx = lx * (1-perc);
            float dy = ly * (1-perc);
            RectF oval = new RectF(bounds);
            oval.offset(dx/2, dy/2);
            oval.set(oval.left, oval.top, oval.right-dx, oval.bottom-dx);
            return oval;
        }


        private void setActivityPaint(String activity, Consumer<Paint> paintBuilder)
        {
            if (!mSlicesPaint.containsKey(activity))
            {
                Paint paint = newPaint();
                paintBuilder.accept(paint);
                mSlicesPaint.put(activity, paint);
            }
        }

        private Paint newPaint()
        {
            return new Paint(Paint.ANTI_ALIAS_FLAG);
        }


        @Override
        public void onDestroy()
        {
            super.onDestroy();
            stopUpdateThread();
        }
    }
}