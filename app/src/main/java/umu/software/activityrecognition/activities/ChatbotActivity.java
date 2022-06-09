package umu.software.activityrecognition.activities;

import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import umu.software.activityrecognition.R;
import umu.software.activityrecognition.chatbot.ActivityRecognitionConsumerImpl;
import umu.software.activityrecognition.shared.AndroidUtils;
import umu.software.activityrecognition.shared.lifecycles.WakeLockLifecycle;


public class ChatbotActivity extends AppCompatActivity
{
    public static final String EXTRA_BOT_LANGUAGE               = "BotLanguage";
    public static final String BOT_LANGUAGE_ENGLISH             = "en-US";
    public static final String BOT_LANGUAGE_SWEDISH             = "sv-SE";







    private ActivityRecognitionConsumerImpl mChatBot;

    private boolean mStarted = false;

    private final Handler mHandler = AndroidUtils.newMainLooperHandler();

    private boolean mBusy = false;
    private ImageView mImageView;

    private WifiManager.WifiLock mWifiLock;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chatbot);

        Intent intent = getIntent();
        Bundle extras = (intent != null && intent.getExtras() != null)? intent.getExtras() : new Bundle();
        if (savedInstanceState != null)
            extras.putAll(savedInstanceState);

        initializeChatbot(extras);
        initializeGUI();

        getLifecycle().addObserver(WakeLockLifecycle.newPartialWakeLock(this));

        mWifiLock = AndroidUtils.forceWifiOn(this);
        //setAmbientEnabled();

    }

    @Override
    protected void onStart()
    {
        super.onStart();
        mStarted = true;
        mChatBot.bindChatbotService();
        setChatbotImage();
        mChatBot.startRecording();
    }

    @Override
    protected void onStop()
    {
        super.onStop();
        mStarted = false;
        mChatBot.stopRecording();
        mChatBot.unbindChatbotService();
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        mWifiLock.release();
    }


    private void initializeChatbot(@NonNull Bundle extras)
    {
        mChatBot = new ActivityRecognitionConsumerImpl(this);
    }

    private void initializeGUI()
    {
        mImageView = findViewById(R.id.imageView);
        mImageView.setOnClickListener(view -> {
            if (mChatBot.isRepeatingClassify())
                mChatBot.sendClassifyEvent();
            else
                mChatBot.startListening();
        });
        mImageView.setImageResource(R.drawable.chatbot_off);
    }



    private void setChatbotImage()
    {
        boolean busy = mChatBot.isBusy();

        if (mStarted)
            mHandler.postDelayed(this::setChatbotImage, 500);

        if (busy == mBusy)
            return;

        if (busy)
            mImageView.setImageResource(R.drawable.chatbot_on);
        else
            mImageView.setImageResource(R.drawable.chatbot_off);
        mBusy = busy;

    }


}