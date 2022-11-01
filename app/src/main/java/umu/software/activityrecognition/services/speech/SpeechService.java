package umu.software.activityrecognition.services.speech;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Binder;
import android.os.IBinder;
import android.speech.RecognizerIntent;
import android.speech.tts.UtteranceProgressListener;
import android.speech.tts.Voice;
import android.util.Pair;

import androidx.annotation.Nullable;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import umu.software.activityrecognition.R;
import umu.software.activityrecognition.preferences.SpeechServicePreferences;
import umu.software.activityrecognition.shared.lifecycles.ForegroundServiceLifecycle;
import umu.software.activityrecognition.shared.services.LifecycleService;
import umu.software.activityrecognition.shared.services.ServiceBinder;
import umu.software.activityrecognition.shared.services.ServiceConnectionHandler;
import umu.software.activityrecognition.shared.util.UniqueId;
import umu.software.activityrecognition.speech.*;


/**
 * Service to utilize Automatic-Speech-Recognition and Text-To-Speech
 *
 * The service uses two languages: Source/target and spoken. Spoken language is the language of the speech interface
 *  - the language that is actually listened or registered from the user. Source/target language is the
 *  language to which spoken language is translated to.
 *  For example spoken language could be swedish and source english when APIs are written in english but the
 *  users speak swedish
 *
 * Started actions:
 * - ACTION_START_LISTENING
 *      Utilized to start listening from the device's microphone
 *      EXTRA_LANGUAGE (Locale) defines the target language
 * - ACTION_SAY
 *      Utilized to say something through the device's speakers
 *      EXTRA_LANGUAGE (Locale) defines the source language
 * - ACTION_CONFIGURE
 *      Configure the service
 *      EXTRA_FOREGROUND whether to put the service in foreground
 *      EXTRA_LANGUAGE (Locale) target language to translate the input speech to when using foreground
 *
 *
 * Can be bound. In this case the binding intent must specify EXTRA_LANGUAGE to set source language
 *
 * Whenever a speech is recognized a broadcast intent with action ACTION_RECOGNIZED_SPEECH is triggered,
 * with extras EXTRA_UTTERANCE, EXTRA_CONFIDENCE, EXTRA_LANGUAGE
 */
public class SpeechService extends LifecycleService
{

    public static class SpeechBinder extends Binder
    {
        private Locale mLanguage;
        private final SpeechService mService;
        private Voice mSelectedVoice;


        public SpeechBinder(SpeechService service, Locale language)
        {
            mService = service;
            mLanguage = language;
        }


        /**
         * Sets the input language for the binder.
         * @param locale
         */
        public void setSourceLanguage(Locale locale)
        {
            mLanguage = locale;
        }

        /**
         * Gets this binder's input language
         * @return this binder's input language
         */
        public Locale getSourceLanguage()
        {
            return mLanguage;
        }


        /**
         * Gets the spoken language whether listened or spoken.
         * The binder's language is translated to this language (and vice-versa) when interacting
         * with the user.
         * For example, binder's language could be english and spoken language swedish
         * @return the spoken language
         */
        public Locale getTargetLanguage()
        {
            return mService.getSpokenLanguage();
        }

        /**
         *
         * @param callback callback receiving the listened sentence in source language
         */
        public void startListening(@Nullable Consumer<String> callback)
        {
            mService.startListening(mLanguage, null, callback);
        }


        public void stopListening()
        {
            mService.stopListening();
        }

        /**
         * Say something
         * @param message message in source language
         * @param callback result callback
         */
        public void say(String message, @Nullable Consumer<Boolean> callback)
        {
            mService.say(message, mLanguage, builder -> {
                if (mSelectedVoice != null)
                    builder.setVoice(mSelectedVoice);
            }, callback);
        }


        /**
         * Sets the voice to use. To be used together with getAvailableVoices()
         * @param voice selected voice
         */
        public void setVoice(Voice voice)
        {
            mSelectedVoice = voice;
        }

        /**
         * Gets the voices available in the binder's language
         * @return  the voices available in the binder's language
         */
        public List<Voice> getAvailableVoices()
        {
            return mService.getAvailableVoices(mService.getSpokenLanguage());
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
    public static final String ACTION_START_LISTENING   = "org.software.activityrecognition.ACTION_START_LISTENING";
    public static final String ACTION_SAY               = "org.software.activityrecognition.ACTION_SAY";
    public static final String ACTION_RECOGNIZED_SPEECH = "org.software.activityrecognition.ACTION_RECOGNIZED_SPEECH";

    public static final String EXTRA_UTTERANCE          = "EXTRA_UTTERANCE";
    public static final String EXTRA_CONFIDENCE         = "EXTRA_CONFIDENCE";
    public static final String EXTRA_SET_FOREGROUND     = "EXTRA_FOREGROUND";
    public static final String EXTRA_LANGUAGE           = "EXTRA_LANGUAGE";



    private ServiceConnectionHandler<ServiceBinder<TranslationService>> mTranslationConnection;


    private DefaultLifecycleObserver mForegroundLifecycle;

    private SpeechServicePreferences mPreferences;
    private List<Voice> mAvailableVoices;




    @Nullable
    @Override
    public IBinder onBind(Intent intent)
    {
        Locale language = intent.hasExtra(EXTRA_LANGUAGE)? (Locale) intent.getSerializableExtra(EXTRA_LANGUAGE) : getSpokenLanguage();
        return new SpeechBinder(this, language);
    }


    @Override
    public void onCreate()
    {
        super.onCreate();
        mPreferences = new SpeechServicePreferences(this);
        mTranslationConnection = new ServiceConnectionHandler<>(this);
        mTranslationConnection.bind(TranslationService.class);

        getAvailableVoices(getSpokenLanguage());
        mAvailableVoices = null;

        mPreferences.language().registerListener( p -> {
            getAvailableVoices(getSpokenLanguage());
        });


        registerAction(this::onConfigureIntent, ACTION_CONFIGURE);
        registerAction(this::onStartListeningIntent, ACTION_START_LISTENING);
        registerAction(this::onSayIntent, ACTION_SAY, EXTRA_UTTERANCE);
    }


    @Override
    public void onDestroy()
    {
        super.onDestroy();
        mTranslationConnection.unbind();
        mPreferences.clearListeners();
    }

    /**
     * Uses extras EXTRA_LANGUAGE
     * @param intent
     */
    private void onStartListeningIntent(Intent intent)
    {
        Locale sourceLanguage = intent.hasExtra(EXTRA_LANGUAGE)?
                (Locale) intent.getSerializableExtra(EXTRA_LANGUAGE) :
                getSpokenLanguage();
        startListening(sourceLanguage, null, null);
    }


    /**
     * Uses extras EXTRA_LANGUAGE, EXTRA_UTTERANCE
     * @param intent
     */
    private void onSayIntent(Intent intent)
    {
        Locale sourceLanguage = intent.hasExtra(EXTRA_LANGUAGE)?
                (Locale) intent.getSerializableExtra(EXTRA_LANGUAGE) :
                getSpokenLanguage();

        String utterance =  intent.getStringExtra(EXTRA_UTTERANCE);

        if (utterance != null)
            say(utterance, sourceLanguage, null, null);
    }


    /**
     * Uses extras EXTRA_LANGUAGE, EXTRA_SET_FOREGROUND
     * @param intent
     */
    private void onConfigureIntent(Intent intent)
    {
        Locale targetLanguage = intent.hasExtra(EXTRA_LANGUAGE)?
                (Locale) intent.getSerializableExtra(EXTRA_LANGUAGE) :
                getSpokenLanguage();

        setForeground(
                intent.getBooleanExtra(EXTRA_SET_FOREGROUND, false),
                targetLanguage
        );
    }


    /**
     * Returns the voices that are available for the given language
     * @param language language of the voices to retrieve
     * @return
     */
    public List<Voice> getAvailableVoices(Locale language)
    {
        if (mAvailableVoices == null || mAvailableVoices.size() == 0 || !mAvailableVoices.get(0).getLocale().toLanguageTag().equals(language.toLanguageTag()))
            mAvailableVoices = TTS.getInstance().getAvailableVoices(voice -> voice.getLocale().getLanguage().equals(language.getLanguage()));
        return mAvailableVoices;
    }


    /**
     * Gets the voice selected in the preferences
     * @return selected voice
     */
    private Voice getSpeakVoice()
    {
        String voiceName = mPreferences.voiceName(getSpokenLanguage()).get(null);

        List<Voice> filtered = getAvailableVoices(getSpokenLanguage())
                .stream()
                .filter( v -> voiceName == null || v.getName().equals(voiceName))
                .collect(Collectors.toList());
        return (filtered.size() == 0)? null : filtered.get(0);
    }


    /**
     * Returns the speech interface language. That is the language used by the user when talking or listening
     * from/to the device
     * @return the user's language
     */
    public Locale getSpokenLanguage()
    {
        return Locale.forLanguageTag(mPreferences.language().get());
    }




    /**
     * Start listening from the device's microphone. The language used to interpret of speech is the same
     * as the spoken language
     * @param callback callback for the operation
     */
    public void startListening(Consumer<String> callback)
    {
        startListening(getSpokenLanguage(), null, callback);
    }


    /**
     * Start listening from the device's microphone
     *
     * @param targetLanguage target language to which the recognized speech is translated to.
     *                       The recognized speech is translated to this language before calling the callback
     * @param callback       callback for the operation
     */
    public void startListening(Locale targetLanguage, @Nullable Consumer<ListenCommand.Builder> commandBuilder, @Nullable Consumer<String> callback)
    {
        if (isBusy())
        {
            logger().w("Ignoring a startListening() call because the service is already busy.");
            return;
        }


        int maxRecognizedResults = mPreferences.maxRecognizedResults().get();
        int minSilenceMillis     = mPreferences.minSilenceMillis().get();

        ListenCommand.Builder builder = new ListenCommand.Builder();
        builder.setLanguage(getSpokenLanguage())
                .setMaxResults(maxRecognizedResults)
                .setLanguageModel(RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                .setCompleteSilenceMillis(minSilenceMillis);

        if (commandBuilder != null)
            commandBuilder.accept(builder);

        Locale sourceLanguage = getSpokenLanguage();
        builder.setLanguage(sourceLanguage)
                .setListener(new BaseSpeechRecognitionListener()
                {
                    @Override
                    public void onError(int i)
                    {
                        super.onError(i);
                        if (callback != null)
                            callback.accept(null);
                        logger().e("ASR error (%s)", i);
                    }

                    @Override
                    protected void onRecognizedSpeech(List<Pair<String, Float>> results)
                    {
                        super.onRecognizedSpeech(results);
                        String userSpeech = results.get(0).first;

                        logger().i("Translating (%s) from (%s) to (%s)", userSpeech, sourceLanguage, targetLanguage);
                        userSpeech = translate(userSpeech, sourceLanguage, targetLanguage);
                        if (callback != null)
                            callback.accept(userSpeech);

                        Intent broadcast = new Intent(ACTION_RECOGNIZED_SPEECH);
                        broadcast.putExtra(EXTRA_UTTERANCE, userSpeech);
                        broadcast.putExtra(EXTRA_CONFIDENCE, results.get(0).second);
                        broadcast.putExtra(EXTRA_LANGUAGE, targetLanguage);
                        LocalBroadcastManager.getInstance(SpeechService.this).sendBroadcast(broadcast);
                    }
                });


        ASR.getInstance().startListening(builder.build());
    }



    /**
     * Stop listening from the device's microphone
     */
    public void stopListening()
    {
        ASR.getInstance().stopListening();
    }




    /**
     * Say something through the device's speakers
     * @param prompt message to say
     * @param sourceLanguage source language of the message. Before saying it, the message will be
     *                       translated from this language to the service's language
     * @param callback optional callback for the operation
     */
    public void say(String prompt, Locale sourceLanguage, @Nullable Consumer<SpeakCommand.Builder> commandBuilder, @Nullable Consumer<Boolean> callback)
    {
        if (isBusy())
        {
            logger().w("Ignoring a say() call because the service is already busy.");
            return;
        }
        Locale targetLanguage = getSpokenLanguage();
        float speechRate = mPreferences.voiceSpeed().get() / 100.f;


        prompt = translate(prompt, sourceLanguage, targetLanguage);

        SpeakCommand.Builder builder = new SpeakCommand.Builder();
        builder.setSpeechRate(speechRate)
                .setPrompt(prompt)
                .setLanguage(targetLanguage)
                .setVoice(getSpeakVoice());
        if (commandBuilder != null)
            commandBuilder.accept(builder);


        builder.setListener(new UtteranceProgressListener()
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
                logger().e("TTS error. Error message: (%s)", s);
            }
        });

        TTS.getInstance().say(builder.build());
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
        return ASR.getInstance().isListening();
    }

    /**
     * Returns whether the service is talking
     * @return true or false
     */
    public boolean isTalking()
    {
        return TTS.getInstance().isTalking();
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
     * @param targetLanguage target language to use in the listen action.
     *                       The recognized speech is translated to this language before broadcasting the result
     */
    @SuppressLint("LaunchActivityFromNotification")
    private void setForeground(boolean setForeground, Locale targetLanguage)
    {
        if (setForeground && !isForeground())
        {
            PendingIntent listenPendingIntent = PendingIntent.getService(this,
                    10476,
                    new Intent(this, SpeechService.class)
                            .setAction(ACTION_START_LISTENING)
                            .putExtra(EXTRA_LANGUAGE, targetLanguage),
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
            intent.putExtra(EXTRA_LANGUAGE, srcTrgLanguage);
        });
    }
}
