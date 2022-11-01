package umu.software.activityrecognition.activities;

import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Lifecycle;
import androidx.wear.ambient.AmbientModeSupport;

import umu.software.activityrecognition.R;
import umu.software.activityrecognition.application.ActivityRecognition;
import umu.software.activityrecognition.shared.util.AndroidUtils;
import umu.software.activityrecognition.shared.lifecycles.WakeLockLifecycle;


public class ChatbotActivity extends AppCompatActivity
{
    private ActivityRecognition mActivityRecognition;

    private final Handler mHandler = AndroidUtils.newMainLooperHandler();

    private boolean mBusy = false;
    private ImageView mImageView;
    private WifiManager.WifiLock mWifiLock;
    private AmbientModeSupport.AmbientController mAmbientController;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chatbot);

        initializeGUI();

        getLifecycle().addObserver(WakeLockLifecycle.newPartialWakeLock(this));

        mWifiLock = AndroidUtils.forceWifiOn(this);

        //setAmbientEnabled();
        mActivityRecognition = ActivityRecognition.getInstance(this);
        mActivityRecognition.onCreate();
        if (getResources().getBoolean(R.bool.iswearable))
            setupWear();
    }


    private void setupWear()
    {
        getSupportActionBar().hide();
        mAmbientController = AmbientModeSupport.attach(this);
    }


    @Override
    protected void onStart()
    {
        super.onStart();
        setChatbotImage();
        mActivityRecognition.startRecordService();                                                  // Start recordings and auto-save
        mActivityRecognition.startRecurrentQuestions();
    }

    @Override
    protected void onStop()
    {
        super.onStop();
        mActivityRecognition.stopRecordService();
        mActivityRecognition.stopRecurrentQuestions();                                              // Stop chatbot's recurrent questions if ever started
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        mActivityRecognition.onDestroy();
        mWifiLock.release();
    }


    private void initializeGUI()
    {
        mImageView = findViewById(R.id.imageView);
        mImageView.setOnClickListener(view -> {
            mActivityRecognition.classifyActivity();
        });
        mImageView.setImageResource(R.drawable.chatbot_off);
    }



    private void setChatbotImage()
    {
        boolean busy = mActivityRecognition.isAskingQuestions();
        if (getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.CREATED))
            mHandler.postDelayed(this::setChatbotImage, 300);

        if (busy == mBusy)
            return;

        mBusy = busy;
        mImageView.setImageResource((busy) ? R.drawable.chatbot_on : R.drawable.chatbot_off);
    }


}