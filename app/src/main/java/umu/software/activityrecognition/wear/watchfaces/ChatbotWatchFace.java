package umu.software.activityrecognition.wear.watchfaces;

import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.PowerManager;
import android.os.SystemClock;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.util.Log;
import android.view.SurfaceHolder;

import androidx.annotation.NonNull;

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
    private PowerManager.WakeLock mWakeLock;
    private WifiManager.WifiLock mWifiLock;
    private ConnectivityManager.NetworkCallback mNetworkCallback;


    @Override
    public void onCreate()
    {
        super.onCreate();
        mActivity = ActivityRecognition.getInstance(this);
        mActivity.onCreate();
        mActivity.startRecordService();
        mActivity.startRecurrentQuestions();

        mWakeLock = AndroidUtils.getWakeLock(this, PowerManager.FULL_WAKE_LOCK);
        mWakeLock.acquire();
        requestInternet();
        Log.i("Chatbot", "onCreate()");
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        mActivity.onDestroy();
        mWakeLock.release();
        releaseInternet();
    }


    private void requestInternet()
    {
        ConnectivityManager cm = getSystemService(ConnectivityManager.class);
        NetworkRequest request = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build();
        mNetworkCallback = new ConnectivityManager.NetworkCallback(){
            @Override
            public void onAvailable(@NonNull Network network)
            {
                super.onAvailable(network);
            }
        };
        cm.requestNetwork(request, mNetworkCallback);
        mWifiLock = AndroidUtils.forceWifiOn(this);

    }

    private void releaseInternet()
    {
        if (mNetworkCallback == null)
            return;
        ConnectivityManager cm = getSystemService(ConnectivityManager.class);
        cm.unregisterNetworkCallback(mNetworkCallback);
        mWifiLock.release();
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
        private static final long INVALIDATE_MILLIS = 250;
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
                        mActivity.classifyActivity();
                        break;
                    case TapDetector.DOUBLE_TAP:
                        if (!mActivity.isPingingQuestions())
                            mActivity.startRecurrentQuestions();
                        else
                            mActivity.stopRecurrentQuestions();
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
            mPainterChooser.setPainter(mActivity.isAskingQuestions()? 1 : 0);
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
