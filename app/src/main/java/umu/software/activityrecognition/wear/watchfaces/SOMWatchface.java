package umu.software.activityrecognition.wear.watchfaces;

import android.content.Intent;
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


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import umu.software.activityrecognition.common.AndroidUtils;
import umu.software.activityrecognition.tflite.SensorTensorflowLiteModels;

public class SOMWatchface extends CanvasWatchFaceService
{
    Engine mEngine = null;
    SensorTensorflowLiteModels mModel = SensorTensorflowLiteModels.SOM;
    private PowerManager.WakeLock mWakeLock;

    @Override
    public void onCreate()
    {
        super.onCreate();
        mWakeLock = AndroidUtils.getWakeLock(this, PowerManager.SCREEN_BRIGHT_WAKE_LOCK);
        mWakeLock.acquire(10*60*1000L /*10 minutes*/);

        mModel.onCreate(this);
        mModel.onStart(this);
    }


    @Override
    public void onDestroy()
    {
        super.onDestroy();
        mWakeLock.release();
        mModel.onStop(this);
        mModel.onDestroy(this);
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
        long mUpdateMillis = 50;


        @Override
        public void onCreate(SurfaceHolder holder)
        {
            super.onCreate(holder);
            onUpdate();
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
                canvas.drawBitmap(mBitmap, null, getOval(bounds, .7f), null);
            }
        }


        private boolean updateBitmap()
        {
            boolean success = mModel.predict();
            if (!success) return false;
            float[] output = mModel.getOutput(0)[0];

            ArrayList<Float> outputList = new ArrayList<>(output.length);
            for (float v : output)
                outputList.add(v);
            float max = Collections.max(outputList);


            double outputSize = output.length;
            outputSize = Math.sqrt(outputSize);
            assert outputSize % 1 == 0;

            int bitmapSize = (int)outputSize;

            Bitmap bitmap = Bitmap.createBitmap(bitmapSize, bitmapSize, Bitmap.Config.ARGB_8888);

            for (int i = 0; i < bitmapSize; i++)
                for(int j = 0; j < bitmapSize; j++)
                {
                    float v = output[i*bitmapSize + j];
                    int r = (int) (255 * v / max);
                    int b = (int) (255 * (1- v / max));
                    bitmap.setPixel(i, j, Color.rgb(r, 0, b));
                }
            mBitmap = bitmap;
            return true;
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
            mUpdateCanvas = !inAmbientMode;
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
