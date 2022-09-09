package umu.software.activityrecognition.services.speech;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Binder;
import android.os.IBinder;
import android.speech.tts.UtteranceProgressListener;
import android.speech.tts.Voice;
import android.util.Pair;

import androidx.annotation.Nullable;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.startup.AppInitializer;

import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

import umu.software.activityrecognition.R;
import umu.software.activityrecognition.preferences.SpeechServicePreferences;
import umu.software.activityrecognition.preferences.initializers.SpeechPreferencesInitializer;
import umu.software.activityrecognition.shared.lifecycles.ForegroundServiceLifecycle;
import umu.software.activityrecognition.shared.services.LifecycleService;
import umu.software.activityrecognition.shared.services.ServiceBinder;
import umu.software.activityrecognition.shared.services.ServiceConnectionHandler;
import umu.software.activityrecognition.shared.util.LogHelper;
import umu.software.activityrecognition.shared.util.UniqueId;
import umu.software.activityrecognition.speech.ASR;
import umu.software.activityrecognition.speech.BaseSpeechRecognitionListener;
import umu.software.activityrecognition.speech.TTS;


/**
 * Service to utilize Automatic-Speech-Recognition and Text-To-Speech
 *
 * Started actions:
 * - ACTION_START_LISTENING
 *      Utilized to start listening from the device's microphone
 *      EXTRA_SRC_TRG_LANGUAGE (Locale) defines the target language
 * - ACTION_SAY
 *      Utilized to say something through the device's speakers
 *      EXTRA_SRC_TRG_LANGUAGE (Locale) defines the source language
 * For any started action EXTRA_FOREGROUND can be set to start/stop foreground mode
 *
 * Setting the foreground also utilized EXTRA_SRC_TRG_LANGUAGE to specify the target language
 *
 * Can be bound. In this case the binding intent must specify EXTRA_SRC_TRG_LANGUAGE
 *
 * Can be set as foreground. In this case it will broadcasts intents with action ACTION_RECOGNIZED_SPEECH
 * and extras EXTRA_UTTERANCE, EXTRA_CONFIDENCE
 */
public class SpeechService extends LifecycleService
{

    public static class SpeechBinder extends Binder
    {
        private final Locale mLanguage;
        private final SpeechService mService;

        public SpeechBinder(SpeechService service, Locale language)
        {
            mService = service;
            mLanguage = language;
        }

        public void startListening(@Nullable Consumer<String> callback)
        {
            mService.startListening(mLanguage, callback);
        }

        public void stopListening()
        {
            mService.stopListening();
        }

        public void say(String message, @Nullable Consumer<Boolean> callback)
        {
            mService.say(message, mLanguage, callback);
        }

        public void setForeground(boolean foreground)
        {
            mService.setForeground(foreground, mLanguage);
        }

        public boolean isBusy()
        {
            return mService.isBusy();
        }
    }

    public static final String ACTION_CONFIGURE         = "org.software.activityrecognition.ACTION_CONFIGURE";

    public static final String EXTRA_SRC_TRG_LANGUAGE   = "EXTRA_SRC_TRG_LANGUAGE";
    public static final String ACTION_START_LISTENING   = "org.software.activityrecognition.ACTION_START_LISTENING";
    public static final String EXTRA_SET_FOREGROUND = "EXTRA_FOREGROUND";


    public static final String ACTION_SAY               = "org.software.activityrecognition.ACTION_SAY";
    public static final String ACTION_RECOGNIZED_SPEECH = "org.software.activityrecognition.ACTION_RECOGNIZED_SPEECH";
    public static final String EXTRA_UTTERANCE          = "EXTRA_UTTERANCE";
    public static final String EXTRA_CONFIDENCE         = "EXTRA_CONFIDENCE";



    private ServiceConnectionHandler<ServiceBinder<TranslationService>> mTranslationConnection;

    private LogHelper mLog;
    private DefaultLifecycleObserver mForegroundLifecycle;

    private SpeechServicePreferences mPreferences;
    private List<Voice> mAvailableVoices;

    @Nullable
    @Override
    public IBinder onBind(Intent intent)
    {
        Locale language = intent.hasExtra(EXTRA_SRC_TRG_LANGUAGE)? (Locale) intent.getSerializableExtra(EXTRA_SRC_TRG_LANGUAGE) : getLanguage();
        return new SpeechBinder(this, language);
    }


    @Override
    public void onCreate()
    {
        super.onCreate();
        ASR.FREE_FORM.initialize(this);
        TTS.INSTANCE.initialize(this);
        mPreferences = AppInitializer.getInstance(this).initializeComponent(SpeechPreferencesInitializer.class);
        mLog = LogHelper.newClassTag(this);
        mTranslationConnection = new ServiceConnectionHandler<>(this);
        mTranslationConnection.bind(TranslationService.class);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        super.onStartCommand(intent, flags, startId);



        switch(intent.getAction())
        {
            case ACTION_CONFIGURE:
                onConfigureIntent(intent);
                break;

            case ACTION_START_LISTENING:
                onStartListeningIntent(intent);
                break;

            case ACTION_SAY:
                onSayIntent(intent);
                break;
        }


        return START_REDELIVER_INTENT;
    }

    /**
     * Uses extras EXTRA_SRC_TRG_LANGUAGE
     * @param intent
     */
    private void onStartListeningIntent(Intent intent)
    {
        Locale sourceLanguage = intent.hasExtra(EXTRA_SRC_TRG_LANGUAGE)?
                (Locale) intent.getSerializableExtra(EXTRA_SRC_TRG_LANGUAGE) :
                getLanguage();
        startListening(sourceLanguage, null);
    }

    /**
     * Uses extras EXTRA_SRC_TRG_LANGUAGE, EXTRA_UTTERANCE
     * @param intent
     */
    private void onSayIntent(Intent intent)
    {
        Locale sourceLanguage = intent.hasExtra(EXTRA_SRC_TRG_LANGUAGE)?
                (Locale) intent.getSerializableExtra(EXTRA_SRC_TRG_LANGUAGE) :
                getLanguage();
        String utterance = intent.hasExtra(EXTRA_UTTERANCE)? intent.getStringExtra(EXTRA_UTTERANCE) : null;
        if (utterance != null)
            say(utterance, sourceLanguage, null);
    }

    /**
     * Uses extras EXTRA_SRC_TRG_LANGUAGE, EXTRA_FOREGROUND
     * @param intent
     */
    private void onConfigureIntent(Intent intent)
    {
        Locale sourceLanguage = intent.hasExtra(EXTRA_SRC_TRG_LANGUAGE)?
                (Locale) intent.getSerializableExtra(EXTRA_SRC_TRG_LANGUAGE) :
                getLanguage();
        if (intent.hasExtra(EXTRA_SET_FOREGROUND))
            setForeground(intent.getBooleanExtra(EXTRA_SET_FOREGROUND, false), sourceLanguage);
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        mTranslationConnection.unbind();
        ASR.FREE_FORM.destroy();
        TTS.INSTANCE.destroy();
        mPreferences.clearListeners();
    }

    public List<Voice> getAvailableVoices()
    {
        return mAvailableVoices;
    }

    /**
     * Returns the speech interface language. That is the language used by the user when talking or listening
     * from/to the device
     * @return the user's language
     */
    public Locale getLanguage()
    {
        return Locale.forLanguageTag(mPreferences.language().get());
    }




    /**
     * Start listening from the device's microphone
     * @param callback callback for the operation
     */
    public void startListening(Consumer<String> callback)
    {
        startListening(getLanguage(), callback);
    }


    /**
     * Start listening from the device's microphone
     * @param targetLanguage target language to which the recognized speech is translated
     * @param callback callback for the operation
     */
    public void startListening(Locale targetLanguage, Consumer<String> callback)
    {
        if (isBusy())
        {
            mLog.w("Ignoring a startListening() call because the service is already busy.");
            return;
        }

        int maxRecognizedResults = mPreferences.maxRecognizedResults().get();


        Locale sourceLanguage = getLanguage();
        ASR.FREE_FORM.setLanguage(sourceLanguage);
        ASR.FREE_FORM.setMaxRecognitionResults(maxRecognizedResults);
        ASR.FREE_FORM.startListening(new BaseSpeechRecognitionListener()
        {
            @Override
            public void onError(int i)
            {
                super.onError(i);
                callback.accept(null);
                mLog.e("ASR error (%s)", i);
            }

            @Override
            protected void onRecognizedSpeech(List<Pair<String, Float>> results)
            {
                super.onRecognizedSpeech(results);
                String userSpeech = results.get(0).first;
                userSpeech = translate(userSpeech, sourceLanguage, targetLanguage);
                callback.accept(userSpeech);

                Intent broadcast = new Intent(ACTION_RECOGNIZED_SPEECH);
                broadcast.putExtra(EXTRA_UTTERANCE, userSpeech);
                broadcast.putExtra(EXTRA_CONFIDENCE, results.get(0).second);
                broadcast.putExtra(EXTRA_SRC_TRG_LANGUAGE, targetLanguage);
                sendBroadcast(broadcast);
            }
        });
    }


    /**
     * Stop listening from the device's microphone
     */
    public void stopListening()
    {
        ASR.FREE_FORM.stopListening();
    }


    /**
     * Say something through the device's speakers
     * @param message message to say
     * @param callback optional callback for the operation
     */
    public void say(String message, @Nullable Consumer<Boolean> callback)
    {
        say(message, getLanguage(), callback);
    }




    /**
     * Say something through the device's speakers
     * @param message message to say
     * @param sourceLanguage source language of the message. Before saying it, the message will be
     *                       translated from this language to the service's language
     * @param callback optional callback for the operation
     */
    public void say(String message, Locale sourceLanguage, @Nullable Consumer<Boolean> callback)
    {
        if (isBusy())
        {
            mLog.w("Ignoring a say() call because the service is already busy.");
            return;
        }
        Locale targetLanguage = getLanguage();
        float speechRate = mPreferences.voiceSpeed().get() / 100.f;
        String voiceName = mPreferences.voiceName().get();

        message = translate(message, sourceLanguage, targetLanguage);
        TTS.INSTANCE.setSpeechRate(speechRate);
        TTS.INSTANCE.setVoice(voiceName);
        TTS.INSTANCE.setLanguage(targetLanguage);

        if (mAvailableVoices == null)
            mAvailableVoices = TTS.INSTANCE.getAvailableVoices(voice -> voice.getLocale().equals(getLanguage()));
        if (mAvailableVoices != null && mAvailableVoices.size() > 0)
            TTS.INSTANCE.setVoice(mAvailableVoices.get(0));


        TTS.INSTANCE.say(message, new UtteranceProgressListener()
        {
            @Override
            public void onStart(String s)
            {

            }

            @Override
            public void onDone(String s)
            {
                if (callback != null)
                    callback.accept(true);
            }

            @Override
            public void onError(String s)
            {
                if (callback != null)
                    callback.accept(false);
                mLog.e("TTS error: (%s)", s);
            }
        });
    }

    /**
     * Whether isListening() or isTalking() is true
     * @return true or false
     */
    public boolean isBusy()
    {
        return isTalking() || isListening();
    }


    /**
     * Returns whether the service is listening to user speech
     * @return true or false
     */
    public boolean isListening()
    {
        return ASR.FREE_FORM.isListening();
    }

    /**
     * Returns whether the service is talking
     * @return true or false
     */
    public boolean isTalking()
    {
        return TTS.INSTANCE.isTalking();
    }


    /**
     * Returns whether the service is currently running in foreground
     * @return whether the service is currently running in foreground
     */
    public boolean isForeground()
    {
        return mForegroundLifecycle != null;
    }


    /**
     * Translate a text
     * @param text text to translate
     * @param sourceLanguage source language
     * @param targetLanguage target language to which the text is to be translated
     * @return the translated text
     */
    private String translate(String text, Locale sourceLanguage, Locale targetLanguage)
    {
        return mTranslationConnection.applyBoundFunction(
                binder -> binder.getService().translate(text, sourceLanguage, targetLanguage),
                text);
    }


    /**
     * Show the foreground service notification
     * @param setForeground whether to show the foreground notification
     * @param language source/target language to use in the listen action
     */
    @SuppressLint("LaunchActivityFromNotification")
    private void setForeground(boolean setForeground, Locale language)
    {
        if (setForeground && !isForeground())
        {
            PendingIntent listenPendingIntent = PendingIntent.getService(this,
                    10476,
                    new Intent(this, SpeechService.class)
                            .setAction(ACTION_START_LISTENING)
                            .putExtra(EXTRA_SRC_TRG_LANGUAGE, language),
                    PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );


            mForegroundLifecycle = new ForegroundServiceLifecycle(
                    this,
                    UniqueId.uniqueInt(),
                    getString(R.string.notification_channel_id),
                    builder -> {
                        builder.setContentTitle(getString(R.string.speech_notification_title))
                                .setContentText(getString(R.string.speech_notification_text))
                                .setGroup(getString(R.string.notification_group_id))
                                .setSmallIcon(R.mipmap.ic_watch_round)
                                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_watch_round))
                                .setContentIntent(listenPendingIntent)
                                .setAutoCancel(false)
                                .setOngoing(true);
                    }
            );

            getLifecycle().addObserver(mForegroundLifecycle);
        }
        else if(!setForeground && isForeground())
        {
            getLifecycle().removeObserver(mForegroundLifecycle);
            mForegroundLifecycle = null;
            stopForeground(true);
        }
    }


    /* Helper functions */

    /**
     * Gets a new connection handler to bind the service
     * @param context calling context
     * @param srcTrgLanguage EXTRA_SRC_TRG_LANGUAGE to use in the binding intent
     * @return a newly created unbound ServiceConnectionHandler
     */
    public static ServiceConnectionHandler<SpeechBinder> newConnection(Context context, Locale srcTrgLanguage)
    {
        return new ServiceConnectionHandler<SpeechBinder>(context).setIntentBuilder(intent -> {
            intent.putExtra(EXTRA_SRC_TRG_LANGUAGE, srcTrgLanguage);
        });
    }
}
