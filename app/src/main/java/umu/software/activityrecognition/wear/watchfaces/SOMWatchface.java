package umu.software.activityrecognition.wear.watchfaces;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Handler;
import android.os.PowerManager;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.view.SurfaceHolder;


import androidx.lifecycle.Lifecycle;

import com.google.common.collect.Maps;
import com.google.common.primitives.Floats;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import umu.software.activityrecognition.shared.AndroidUtils;
import umu.software.activityrecognition.data.dataframe.DataFrame;
import umu.software.activityrecognition.tflite.TFLiteNamedModels;
import umu.software.activityrecognition.tflite.model.AccumulatorTFModel;

public class SOMWatchface extends CanvasWatchFaceService
{
    Engine mEngine = null;
    AccumulatorTFModel mModel;
    private PowerManager.WakeLock mWakeLock;
    private long mPredictionTime = 0L;

    @Override
    public void onCreate()
    {
        super.onCreate();
        mWakeLock = AndroidUtils.getWakeLock(this, PowerManager.PARTIAL_WAKE_LOCK);
        mWakeLock.acquire(10*60*1000L /*10 minutes*/);

        mModel = TFLiteNamedModels.SOM.newInstance(this);
        mModel.getLifecycle().handleLifecycleEvent(Lifecycle.Event.ON_CREATE);
        mModel.getLifecycle().handleLifecycleEvent(Lifecycle.Event.ON_START);

    }


    @Override
    public void onDestroy()
    {
        super.onDestroy();
        mWakeLock.release();
        mModel.getLifecycle().handleLifecycleEvent(Lifecycle.Event.ON_STOP);
        mModel.getLifecycle().handleLifecycleEvent(Lifecycle.Event.ON_DESTROY);
    }

    @Override
    public Engine onCreateEngine() {
        mEngine = new Engine();
        mEngine.setWatchFaceStyle(new WatchFaceStyle.Builder(this)
                .setAcceptsTapEvents(true)
                .build());
        return mEngine;
    }



    private class Engine extends CanvasWatchFaceService.Engine
    {
        Bitmap mBitmap = null;
        private boolean mUpdateCanvas = true;
        Handler mHandler = AndroidUtils.newMainLooperHandler();
        long mUpdateMillis = 100;
        float mBitmapPercSize = 0.7f;

        @Override
        public void onCreate(SurfaceHolder holder)
        {
            super.onCreate(holder);
            onUpdate();
        }

        @Override
        public void onDestroy()
        {
            super.onDestroy();
            mUpdateCanvas = false;
        }

        @Override
        public void onTimeTick()
        {
            super.onTimeTick();
            invalidate();
        }


        @Override
        public void onDraw(Canvas canvas, Rect bounds)
        {
            super.onDraw(canvas, bounds);
            if (mBitmap != null)
            {
                RectF dest = getOval(bounds, mBitmapPercSize);
                canvas.drawBitmap(mBitmap, null, dest, null);
                drawPredictionTime(canvas, dest.left, dest.top);
            }
        }

        private void drawPredictionTime(Canvas canvas, float x, float y)
        {
            String text = String.valueOf(mPredictionTime);
            Paint textPaint = new Paint();
            Rect textBounds = new Rect();
            textPaint.setColor(Color.GREEN);
            textPaint.setTextSize(20);
            textPaint.getTextBounds(text, 0, text.length(), textBounds);
            float text_height =  textBounds.height();
            canvas.drawText(text, x, y + text_height , textPaint);
        }

        private float[] toArray(DataFrame df)
        {
            Object[] row = df.getRowArray(0);
            float[] res = new float[row.length];
            for (int i = 0; i < row.length; i++)
                res[i] = Float.parseFloat(row[i].toString());
            return res;
        }

        private boolean updateBitmap()
        {
            Map<Integer, DataFrame> outputs = Maps.newHashMap();

            mPredictionTime = AndroidUtils.measureElapsedTime(() -> {
                Map<Integer, DataFrame> modelOutputs = mModel.getOutputDataFrames();
                if (modelOutputs != null)
                    outputs.putAll(modelOutputs);
            });
            if (outputs.size() == 0)
                return false;

            float[] output = toArray(outputs.get(0));


            double outputSize = output.length;
            outputSize = Math.sqrt(outputSize);
            assert outputSize % 1 == 0;

            int bitmapSize = (int)outputSize;

            Bitmap bitmap = Bitmap.createBitmap(bitmapSize, bitmapSize, Bitmap.Config.ARGB_8888);
            //Log.i(getClass().getSimpleName(), Arrays.toString(output));

            int[][] rgb = getRGB(output);
            int k;
            for (int i = 0; i < bitmapSize; i++)
                for(int j = 0; j < bitmapSize; j++)
                {
                    k = i * bitmapSize + j;
                    bitmap.setPixel(i, j, Color.rgb(rgb[0][k], rgb[1][k], rgb[2][k]));
                }

            mBitmap = bitmap;
            return true;
        }

        private int[][] getRGB(float[] somValues)
        {
            int[] r = new int[somValues.length];
            int[] g = new int[somValues.length];
            int[] b = new int[somValues.length];

            List<Float> outputList = Floats.asList(somValues);
            float max = Collections.max(outputList);
            float min = Collections.min(outputList);

            Function<Float, Double> norm = (v) -> 1 - Math.pow((v - min) / (max - min), 2);

            for (int i = 0; i < somValues.length; i++) {
                float v = somValues[i];
                r[i] = (int) (255 * norm.apply(v));
                g[i] = (somValues[i] <= min + 0.1)? 255 : 0;
                b[i] = 255 - r[i];
            }
            int[][] rgb = new int[][]{r, g, b};
            return rgb;
        }


        private void onUpdate()
        {
            if(mUpdateCanvas)
                updateBitmap();
                invalidate();
            mHandler.postDelayed(this::onUpdate, mUpdateMillis);
        }


        @Override
        public void onAmbientModeChanged(boolean inAmbientMode)
        {
            super.onAmbientModeChanged(inAmbientMode);
            //mUpdateCanvas = !inAmbientMode;
        }

        @Override
        public void onVisibilityChanged(boolean visible)
        {
            super.onVisibilityChanged(visible);
            mUpdateCanvas = visible;
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
    }
}
