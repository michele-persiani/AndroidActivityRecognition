package umu.software.activityrecognition.application;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.VibrationEffect;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.util.Locale;

import umu.software.activityrecognition.services.recordings.ClassificationService;
import umu.software.activityrecognition.services.speech.SpeechService;
import umu.software.activityrecognition.shared.services.ServiceBinder;
import umu.software.activityrecognition.shared.services.ServiceConnectionHandler;
import umu.software.activityrecognition.services.recordings.RecordServiceHelper;
import umu.software.activityrecognition.shared.util.AndroidUtils;
import umu.software.activityrecognition.shared.util.VibratorManager;


/**
 * Singleton defining te main macro-procedures to perform during the application lifecycle, such as
 * starting/stopping the chatbot, recordings etc.
 */
public class ActivityRecognition
{
    private static final String CHATBOT_USAGE                   = "CHATBOT_USAGE";

    private final RecordServiceHelper mRecordingsHelper;
    private final Context mContext;


    private final ServiceConnectionHandler<ServiceBinder<ClassificationService>> mClassifyServiceBinder;
    private final ServiceConnectionHandler<ServiceBinder<SpeechService>> mSpeechServiceBinder;

    private static final Object sLock = new Object();
    private static ActivityRecognition sInstance;


    private final BroadcastReceiver mLabelsReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            String sensorsLabel = intent.getStringExtra(ClassificationService.EXTRA_ZIP_FILENAME);
            mRecordingsHelper.setSensorsLabel(sensorsLabel);
            Log.i(getClass().getSimpleName(), String.format("Setting sensors label to (%s)", sensorsLabel));
        }
    };


    private ActivityRecognition(Context context)
    {
        context = context.getApplicationContext();
        mContext = context;
        mRecordingsHelper = RecordServiceHelper.newInstance(context);
        mClassifyServiceBinder = new ServiceConnectionHandler<>(context);
        mSpeechServiceBinder = new ServiceConnectionHandler<>(context);
    }


    public static ActivityRecognition getInstance(Context context)
    {
        synchronized (sLock)
        {
            if (sInstance == null)
                sInstance = new ActivityRecognition(context);
            return sInstance;
        }
    }


    public void onCreate()
    {
        mClassifyServiceBinder.bind(ClassificationService.class);
        mSpeechServiceBinder
                .setIntentBuilder(intent -> intent.putExtra(SpeechService.EXTRA_LANGUAGE, Locale.ENGLISH))
                .bind(SpeechService.class);
        IntentFilter intentFilter =  new IntentFilter();
        intentFilter.addAction(ClassificationService.ACTION_ON_CLASSIFICATION_DONE);
        LocalBroadcastManager.getInstance(mContext).registerReceiver(mLabelsReceiver, intentFilter);
    }


    public void onDestroy()
    {
        stopRecurrentQuestions();
        stopRecordService();
        mClassifyServiceBinder.unbind();
        mSpeechServiceBinder.unbind();
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(mLabelsReceiver);
    }


    public void startRecurrentQuestions()
    {
        mRecordingsHelper.bind().enqueue(binder -> {
            if (binder.getService().isRecording()) {
                Intent i = new Intent(mContext, ClassificationService.class);
                i.setAction(ClassificationService.ACTION_RECURRENT_CLASSIFY_START);
                mContext.startService(i);
            }
        }).enqueueUnbind();
        //mRecordingsHelper.recordEvent(CHATBOT_USAGE, event -> event.put("asking_questions", true));

        VibratorManager.getInstance(mContext).vibrateDoubleClick();
    }


    public void stopRecurrentQuestions()
    {
        Intent i = new Intent(mContext, ClassificationService.class);
        i.setAction(ClassificationService.ACTION_RECURRENT_CLASSIFY_STOP);
        mContext.startService(i);
        //mRecordingsHelper.recordEvent(CHATBOT_USAGE, event -> event.put("asking_questions", false));
        mRecordingsHelper.setSensorsLabel(null);
        VibratorManager.getInstance(mContext).vibrateLongTick();
    }


    public boolean isPingingQuestions()
    {
        return mClassifyServiceBinder.applyBoundFunction( binder -> {
            return binder.getService().isRecurrentlyAskingQuestions();
        }, false);
    }


    public boolean isAskingQuestions()
    {
        return  mClassifyServiceBinder.applyBoundFunction(binder -> binder.getService().isCurrentlyAskingQuestions(), false);
    }


    public void classifyActivity()
    {
        Intent i = new Intent(mContext, ClassificationService.class);
        i.setAction(ClassificationService.ACTION_CLASSIFY);
        mContext.startService(i);

    }


    public void startRecordService()
    {
        mRecordingsHelper.startRecording(null);
        mRecordingsHelper.startRecurrentSave();

        //mRecordingsHelper.setupBroadcastReceiver(
        //        CHATBOT_USAGE,
        //         null
        //);
        //mRecordingsHelper.recordEvent(CHATBOT_USAGE, event -> event.put("recording", true));
    }


    public void stopRecordService()
    {
        //mRecordingsHelper.recordEvent(CHATBOT_USAGE, event -> event.put("recording", false));
        stopRecurrentQuestions();
        //mRecordingsHelper.removeBroadcastReceiver(CHATBOT_USAGE, null);
        mRecordingsHelper.saveZipClearFiles();
        mRecordingsHelper.stopRecurrentSave();
        mRecordingsHelper.stopRecording();
    }
}
