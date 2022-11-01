package umu.software.activityrecognition.services.recordings;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.util.Pair;

import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.AtomicDouble;

import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import umu.software.activityrecognition.preferences.ClassifyRecordingsPreferences;
import umu.software.activityrecognition.services.speech.SpeechService;
import umu.software.activityrecognition.shared.audio.Wave;
import umu.software.activityrecognition.shared.audio.WaveRecorder;
import umu.software.activityrecognition.shared.lifecycles.BinderLifecycle;
import umu.software.activityrecognition.shared.lifecycles.ExclusiveResourceLifecycle;
import umu.software.activityrecognition.shared.persistance.Directories;
import umu.software.activityrecognition.shared.services.LifecycleService;
import umu.software.activityrecognition.shared.services.ServiceBinder;
import umu.software.activityrecognition.shared.util.Exceptions;
import umu.software.activityrecognition.shared.util.FunctionLock;
import umu.software.activityrecognition.shared.util.RepeatingBroadcast;
import umu.software.activityrecognition.shared.util.VibratorManager;
import umu.software.activityrecognition.shared.resourceaccess.ExclusiveResource;


/**
 * Service to ask the user a set of questions and record their answers
 * Actions:
 * - ACTION_CLASSIFY asks the user the series of questions specified in the extra EXTRA_PROMPTS
 * - ACTION_RECURRENT_CLASSIFY_START start recurrently asking classifications
 * - ACTION_RECURRENT_CLASSIFY_STOP stop to recurrently asking classifications
 */
public class ClassificationService extends LifecycleService
{
    public static final String ACTION_CLASSIFY                 = "umu.software.activityrecognition.ACTION_CLASSIFY";

    public static final String ACTION_RECURRENT_CLASSIFY_START = "umu.software.activityrecognition.ACTION_RECURRENT_CLASSIFY_START";

    public static final String ACTION_RECURRENT_CLASSIFY_STOP  = "umu.software.activityrecognition.ACTION_RECURRENT_CLASSIFY_STOP";

    public static final String ACTION_ON_CLASSIFICATION_DONE   = "umu.software.activityrecognition.ACTION_ON_CLASSIFICATION_DONE";
    public static final String EXTRA_ZIP_FILENAME              = "EXTRA_ZIP_FILENAME";



    private static final String FILENAME_PROMPTS_FILE           = "prompts";
    private static final String FILENAME_ANSWER_FILE            = "answer";


    private BinderLifecycle<SpeechService.SpeechBinder> mSpeechBinder;

    WaveRecorder mRecorder = new WaveRecorder();
    private boolean mIsRecording = false;

    private boolean mShutdown = false;
    private final FunctionLock mLock = FunctionLock.newInstance();
    private final SimpleDateFormat mDateFormat = new SimpleDateFormat("dd-M-yyyy_hh-mm-ss", Locale.getDefault());

    private BroadcastReceiver mCallback = null;
    private ClassifyRecordingsPreferences mPreferences;
    private RepeatingBroadcast mRepeatingBroadcast;
    private ExclusiveResourceLifecycle mTokensLifecycle;
    private final Object mObjectToken = new Object();

    @Nullable
    @Override
    public IBinder onBind(Intent intent)
    {
        return new ServiceBinder<>(this);
    }



    @Override
    public void onCreate()
    {
        super.onCreate();
        mPreferences = new ClassifyRecordingsPreferences(this);

        mRepeatingBroadcast = new RepeatingBroadcast(this);

        mSpeechBinder = new BinderLifecycle<>(
                        this,
                        SpeechService.class,
                        intent -> intent.putExtra(SpeechService.EXTRA_LANGUAGE,
                                (Serializable) Locale.forLanguageTag(mPreferences.questionsLanguage().get())
                        )
                );
        getLifecycle().addObserver(mSpeechBinder);


        mTokensLifecycle = new ExclusiveResourceLifecycle();
        getLifecycle().addObserver(mTokensLifecycle);

        mTokensLifecycle.registerToken(mObjectToken,
                ExclusiveResource.PRIORITY_HIGH,
                ExclusiveResource.AUDIO_INPUT, ExclusiveResource.AUDIO_OUTPUT
        );


        mPreferences.minSilenceLengthSeconds().registerListener( p -> {
            mRecorder.setNotifyIntervalMillis(p.get());
        });

        mPreferences.askRecurrentQuestions().registerListener(p -> {
            if (p.get() && !mRepeatingBroadcast.isBroadcasting())
                onStartRecurrentQuestions(null);
            else if (mRepeatingBroadcast.isBroadcasting() && !p.get())
                onStopRecurrentQuestions(null);
        });

        mPreferences.recurrentQuestionsEveryMinutes().registerListener( p -> {
            if (mRepeatingBroadcast.isBroadcasting())
            {
                mRepeatingBroadcast.stop();
                startRepeatingBroadcast();
            }
        });

        registerAction(this::onClassifyAction, ACTION_CLASSIFY);
        registerAction(this::onStartRecurrentQuestions, ACTION_RECURRENT_CLASSIFY_START);
        registerAction(this::onStopRecurrentQuestions, ACTION_RECURRENT_CLASSIFY_STOP);
    }


    @Override
    public void onDestroy()
    {
        super.onDestroy();
        mPreferences.clearListeners();
        mLock.lock();
        if (mCallback != null)
            LocalBroadcastManager.getInstance(this).unregisterReceiver(mCallback);
        mRecorder.stopRecording();
        mShutdown = true;
        mIsRecording = false;
        mLock.unlock();
    }



    /**
     * Sets a callback that is notified when classifications are performed. Received intents will contain
     * the action ACTION_ON_CLASSIFICATION_DONE
     * @param callback callback to register
     */
    public void setCallback(BiConsumer<Context, Intent> callback)
    {
        if (mCallback != null)
            LocalBroadcastManager.getInstance(this).unregisterReceiver(mCallback);

        mCallback = new BroadcastReceiver()
        {
            @Override
            public void onReceive(Context context, Intent intent)
            {
                callback.accept(context, intent);
            }
        };
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_ON_CLASSIFICATION_DONE);
        LocalBroadcastManager.getInstance(this).registerReceiver(mCallback, filter);
    }






    /* ---------------------- Classify activity ---------------------- */

    /**
     * Performs classification, save to file, and broadcast of label
     * @param intent
     */
    private void onClassifyAction(Intent intent)
    {
        String saveFolder = mPreferences.zipDestinationFolder().get();
        List<String> prompts = mPreferences.questions().get();
        if (mIsRecording)
        {
            logger().w("Already asking questions. Skipping Intent: %s", intent);
            return;
        }
        mIsRecording = true;

        boolean tokenAcquired = mTokensLifecycle.getToken(mObjectToken).acquire(false);
        if (!tokenAcquired)
            return;


        runAsync(() -> {
            try
            {
                List<Pair<String, Wave>> recordings = registerAnwsers(prompts);

                if (recordings == null)
                    return;

                String zipFilename = saveAndZip(saveFolder, recordings);

                Intent resultBroadcast = new Intent()
                        .setAction(ACTION_ON_CLASSIFICATION_DONE)
                        .putExtra(EXTRA_ZIP_FILENAME, zipFilename);
                LocalBroadcastManager.getInstance(this).sendBroadcast(resultBroadcast);
            }
            catch(Throwable t)
            {
                logger().e(t.getMessage());
                t.printStackTrace();
            }
            finally
            {
                mIsRecording = false;
                mTokensLifecycle.getToken(mObjectToken).release();
            }
        });
    }



    private List<Pair<String,Wave>> registerAnwsers(List<String> prompts)
    {
        List<Pair<String,Wave>> recordings = Lists.newArrayList();
        for (String prompt : Lists.newArrayList(prompts))
        {
            if (mLock.withLock(() -> mShutdown)) return null;
            CountDownLatch latch = new CountDownLatch(1);
            mSpeechBinder.enqueue(binder -> {
                binder.say(
                        prompt,
                        result -> {
                            if (result)
                            {
                                VibratorManager.getInstance(this).vibrateLightClick();
                                logger().i("Registering answer for prompt: %s", prompt);
                                Wave answer = registerAnswer();
                                if (answer != null)
                                    recordings.add(Pair.create(prompt, answer));
                            }
                            latch.countDown();
                        });
            });

            if (!Exceptions.runCatch(latch::await))
                return null;
        }
        return recordings;
    }


    @Nullable
    private Wave registerAnswer()
    {
        int silenceDBThreshold = mPreferences.silenceDbThreshold().get();
        int silenceMinLengthMillis = mPreferences.minSilenceLengthSeconds().get() * 1000;
        int maxRecordingLengthMillis = mPreferences.maxSpeechLengthSeconds().get() * 1000;
        long onSpeechVibrationLength = mPreferences.onSpeechVibrationLength().get();

        mLock.lock();
        final Wave[] wave = {null};


        CountDownLatch latch = new CountDownLatch(1);

        AtomicDouble prevRms = new AtomicDouble(0);

        mRecorder.setMaxRecordingLength(maxRecordingLengthMillis);
        mRecorder.setSilenceRMSThreshold(silenceDBThreshold);
        mRecorder.setCallback(new WaveRecorder.Callback()
        {
            @Override
            public void onUpdate()
            {
                if (mRecorder.getElapsedSilenceMillis() > silenceMinLengthMillis)
                    mRecorder.stopRecording();
                double rms = mRecorder.getRMS();

                if ((rms >= silenceDBThreshold) && (rms > prevRms.getAndSet(rms) + mPreferences.onSpeechVibrationMinDbDelta().get()))
                    VibratorManager
                            .getInstance(ClassificationService.this)
                            .vibrate(onSpeechVibrationLength);

                //Log.i(getClass().getSimpleName(), String.format("Recording... RMS: %s", mRecorder.getRMS()));
            }

            @Override
            public void onFinish()
            {
                wave[0] = mRecorder.getWave();
                mLock.withLock(latch::countDown);
            }
        });

        mRecorder.startRecording();
        mLock.unlock();

        return Exceptions.runCatch(() -> {
                    latch.await();
                    return wave[0];
                },null);
    }



    private String saveAndZip(String saveFolder, List<Pair<String, Wave>> recordings)
    {
        String zipFile = String.format("%s.zip", mDateFormat.format(Calendar.getInstance().getTime()));
        Directories.peformOnDirectory(
                saveFolder,
                null,
                dir -> {
                    List<String> files = Lists.newArrayList();

                    // Save prompts file
                    String tmp;
                    tmp = String.format("%s.txt", FILENAME_PROMPTS_FILE);
                    files.add(tmp);

                    dir.writeToFile(
                            tmp,
                            os -> {
                                OutputStreamWriter osw = new OutputStreamWriter(os, StandardCharsets.UTF_8);
                                for (String p : recordings.stream().map(p -> p.first).collect(Collectors.toList()))
                                    osw.write(p + System.lineSeparator());
                                osw.close();
                                return null;
                            }
                    );

                    // Save wave files
                    int i = 0;
                    for (Wave w : recordings.stream().map(p -> p.second).collect(Collectors.toList()))
                    {
                        tmp = String.format("%s_%s.wav", FILENAME_ANSWER_FILE, i);
                        files.add(tmp);
                        dir.writeToFile(
                                tmp,
                                os -> {
                                    w.writeToOutputStream(os);
                                    return null;
                                }
                        );
                        i ++;
                    }

                    // Zip file
                    Directories.createZip(dir, zipFile, files);

                    // Clear unzipped files
                    dir.delete(files::contains);
                    return null;
                });

        return Paths.get(saveFolder, zipFile).toString();
    }














    /*
    * ---------------------- Start/stop recurrent questions ----------------------
    */

    private void onStartRecurrentQuestions(@Nullable Intent intent)
    {
        startRepeatingBroadcast();
        logger().i(
                "Classifying the activity every %s minutes",
                mPreferences.recurrentQuestionsEveryMinutes().get()
        );

    }


    private void onStopRecurrentQuestions(@Nullable Intent intent)
    {
        mRepeatingBroadcast.stop();
        stopSelf();
    }



    private void startRepeatingBroadcast()
    {
        mRepeatingBroadcast.start(
                TimeUnit.MILLISECONDS.convert(mPreferences.recurrentQuestionsEveryMinutes().get(), TimeUnit.MINUTES),
                (context, intent1) -> {
                    Intent i = new Intent(this, ClassificationService.class);
                    i.setAction(ACTION_CLASSIFY);
                    startService(i);
                });
    }

    /**
     * Returns whether the service is recurrently asking question by sending ACTION_CLASSIFY intents
     * to itself
     * @return whether the service is recurrently asking
     */
    public boolean isRecurrentlyAskingQuestions()
    {
        return mRepeatingBroadcast.isBroadcasting();
    }



    /**
     * Returns whether the service is currently recording questions. That is it received an intent with
     * ACTION_CLASSIFY
     * @return whether the service is currently recording questions
     */
    public boolean isCurrentlyAskingQuestions()
    {
        return mIsRecording;
    }

}
