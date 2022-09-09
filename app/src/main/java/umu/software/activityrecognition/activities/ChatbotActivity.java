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
import umu.software.activityrecognition.speech.ASR;


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
        mActivityRecognition.startChatbot();                                                   // Bind and start the chatbot (keep binding on)
        mActivityRecognition.startRecordService();                                                  // Start recordings and auto-save
        mActivityRecognition.askStartRecurrentQuestions();
        //mActivityRecognition.sendWelcomeEvent()
        ASR.FREE_FORM.getSupportedLanguages(this);
    }

    @Override
    protected void onStop()
    {
        super.onStop();
        //mActivityRecognition.sendGoodbyeEvent()
        //mActivityRecognition.stopRecordService();
        mActivityRecognition.stopRecurrentQuestions();                                              // Stop chatbot's recurrent questions if ever started
        mActivityRecognition.shutdownChatbot();                                                     // Unbind chatbot and clear sensor label
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        mWifiLock.release();
    }


    private void initializeGUI()
    {
        mImageView = findViewById(R.id.imageView);
        mImageView.setOnClickListener(view -> mActivityRecognition.startListening());
        mImageView.setImageResource(R.drawable.chatbot_off);
    }



    private void setChatbotImage()
    {
        boolean busy = mActivityRecognition.isChatbotBusy();
        if (getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.CREATED))
            mHandler.postDelayed(this::setChatbotImage, 300);

        if (busy == mBusy)
            return;

        mBusy = busy;
        mImageView.setImageResource((busy) ? R.drawable.chatbot_on : R.drawable.chatbot_off);
    }


}