package umu.software.activityrecognition.wear.watchfaces;

import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.os.Handler;
import android.os.SystemClock;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.util.Log;
import android.view.SurfaceHolder;

import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Consumer;

import umu.software.activityrecognition.R;
import umu.software.activityrecognition.application.ActivityRecognition;
import umu.software.activityrecognition.shared.util.AndroidUtils;
import umu.software.activityrecognition.shared.util.Exceptions;
import umu.software.activityrecognition.wear.watchfaces.drawing.impl.PainterChooser;
import umu.software.activityrecognition.wear.watchfaces.drawing.PainterFactory;


public class ChatbotWatchFace extends CanvasWatchFaceService
{
    private ActivityRecognition mActivity;


    @Override
    public void onCreate()
    {
        super.onCreate();
        mActivity = ActivityRecognition.getInstance(this);
        mActivity.startChatbot();
        mActivity.startRecordService();
        mActivity.askStartRecurrentQuestions();
        Log.i("Chatbot", "onCreate()");
    }


    @Override
    public void onDestroy()
    {
        super.onDestroy();
        mActivity.stopRecordService();
        mActivity.stopRecurrentQuestions();
        mActivity.shutdownChatbot();
    }



    @Override
    public Engine onCreateEngine()
    {
        return new ChatbotWatchFace.Engine();
    }



    private static class TapDetector
    {

        public static final int SINGLE_TAP = 0;
        public static final int DOUBLE_TAP = 1;


        private final long mMaxDelay;
        private long mLastTapTime;
        private Consumer<Integer> mListener;
        private Timer mTimer;

        TapDetector(long doubleTapMaxDelayMillis)
        {
            mMaxDelay = doubleTapMaxDelayMillis;
            mLastTapTime = SystemClock.elapsedRealtime();
            mListener = (t) -> {};
            mTimer = new Timer();
        }

        public void setListener(Consumer<Integer> tapListener)
        {
            mListener = tapListener;
        }

        public void onTap()
        {
            long currTime = SystemClock.elapsedRealtime();
            long timeDiff = currTime - mLastTapTime;
            boolean doubleTap = timeDiff < mMaxDelay;

            mLastTapTime = currTime;
            if (doubleTap)
            {
                Exceptions.runCatch(() -> mTimer.cancel());
                mListener.accept(DOUBLE_TAP);
            }
            else
            {
                mTimer = new Timer();
                mTimer.schedule(new TimerTask()
                {
                    @Override
                    public void run()
                    {
                        mListener.accept(SINGLE_TAP);
                    }
                }, mMaxDelay);
            }
        }
    }





    private class Engine extends CanvasWatchFaceService.Engine
    {
        private static final long INVALIDATE_MILLIS = 700;
        private final Handler mHandler = AndroidUtils.newHandler();
        private PainterChooser mPainterChooser;

        private TapDetector mTapDetector;

        private void loopInvalidate()
        {
            invalidate();
            mHandler.postDelayed(this::loopInvalidate, INVALIDATE_MILLIS);
        }

        @Override
        public void onCreate(SurfaceHolder holder)
        {
            super.onCreate(holder);
            mPainterChooser = PainterFactory.newBitmapChooser(
                    new Rect(),
                    BitmapFactory.decodeResource(getResources(), R.drawable.chatbot_off),
                    BitmapFactory.decodeResource(getResources(), R.drawable.chatbot_on)
                    );

            mTapDetector = new TapDetector(400);
            mTapDetector.setListener((tapType) -> {
                switch (tapType)
                {
                    case TapDetector.SINGLE_TAP:
                        mActivity.sendClassifyEvent();
                        break;
                    case TapDetector.DOUBLE_TAP:
                        if (!mActivity.isAskingQuestions())
                            mActivity.askStartRecurrentQuestions();
                        else
                            mActivity.askStopRecurrentQuestions();
                        break;
                }
            });

            setWatchFaceStyle(new WatchFaceStyle.Builder(ChatbotWatchFace.this)
                    .setAcceptsTapEvents(true)
                    // other settings
                    .build());

            loopInvalidate();
        }



        @Override
        public void onTapCommand(int tapType, int x, int y, long eventTime)
        {
            super.onTapCommand(tapType, x, y, eventTime);

            if (tapType == TAP_TYPE_TAP)
                mTapDetector.onTap();

        }


        @Override
        public void onDraw(Canvas canvas, Rect bounds)
        {
            super.onDraw(canvas, bounds);
            mPainterChooser.setPainter(mActivity.isChatbotBusy()? 1 : 0);
            mPainterChooser.getDest().set(bounds);
            mPainterChooser.accept(canvas);
            //Bitmap image = getChatbotImage();
            //Rect src = new Rect(0, 0, image.getWidth(), image.getHeight());
            //Paint paint = getPaint(null);
            //canvas.drawBitmap(image, src, bounds, paint);
        }


        @Override
        public void onDestroy()
        {
            super.onDestroy();
            mHandler.getLooper().quit();
        }
    }

}
