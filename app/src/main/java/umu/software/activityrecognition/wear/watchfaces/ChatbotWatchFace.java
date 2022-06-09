package umu.software.activityrecognition.wear.watchfaces;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Handler;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.util.Log;
import android.view.SurfaceHolder;

import com.google.common.collect.Maps;

import java.util.function.Consumer;

import umu.software.activityrecognition.R;
import umu.software.activityrecognition.chatbot.ChatbotResponse;
import umu.software.activityrecognition.chatbot.SpeechChatbot;
import umu.software.activityrecognition.config.ChatBotPreferences;
import umu.software.activityrecognition.services.DialogflowService;
import umu.software.activityrecognition.services.RecordServiceHelper;
import umu.software.activityrecognition.services.ServiceConnectionHandler;
import umu.software.activityrecognition.shared.AndroidUtils;
import umu.software.activityrecognition.shared.RepeatingBroadcast;


public class ChatbotWatchFace extends CanvasWatchFaceService implements Consumer<ChatbotResponse>
{
    public static final String ACTION_START_RECORDING           = "StartRecording";
    public static final String ACTION_STOP_RECORDING            = "StopRecording";
    public static final String ACTION_CLASSIFY_ACTIVITY_SHORT   = "ClassifyActivityShort";
    public static final String ACTION_CLASSIFY_ACTIVITY_LONG    = "ClassifyActivityLong";
    public static final String EVENT_CLASSIFY_ACTIVITY          = "ClassifyActivityShort";

    public static final String SLOT_PARTICIPANT                 = "participant";


    private ServiceConnectionHandler<DialogflowService.Binder> mConnection;

    private String mParticipantName;
    private RepeatingBroadcast mClassifyReceiver;


    private void bindChatbotService()
    {
        if (mConnection == null)
        {
            String apiKey = AndroidUtils.readRawResourceFile(this, R.raw.dialogflow_credentials);
            if (apiKey == null)
                return;
            mConnection = DialogflowService.getConnection(this, apiKey, null, null);
        }
        if(!mConnection.isBound() && !mConnection.isBinding())
            mConnection.bind(DialogflowService.class);
    }

    private void chatbotOperation(Consumer<SpeechChatbot> operation)
    {
        mConnection.applyBound((binder) -> {
            SpeechChatbot chatbot = binder.getService().getChatBot();
            if (chatbot != null)
                operation.accept(chatbot);
        });
    }

    private boolean isBusy()
    {
        Boolean busy = mConnection.applyBoundFunction((binder) ->{
            SpeechChatbot chatbot = binder.getService().getChatBot();
            return chatbot != null && chatbot.isConnected() && chatbot.isBusy();
        });
        return (busy != null)? busy : false;
    }


    @Override
    public void onCreate()
    {
        super.onCreate();
        bindChatbotService();
        startRecording();
    }


    @Override
    public void onDestroy()
    {
        super.onDestroy();
        stopRecording();
        stopRecurrentQuestions();
        mConnection.unbind();
    }


    @Override
    public Engine onCreateEngine()
    {
        return new ChatbotWatchFace.Engine();
    }


    @Override
    public void accept(ChatbotResponse response)
    {
        if (response.hasError())
        {
            Log.w(getClass().getSimpleName(), String.format("Error in chatbot response %s", response));
            return;
        }
        String action = response.getAction();
        RecordServiceHelper serviceHelper = RecordServiceHelper.newInstance(this);
        switch (action)
        {
            case ACTION_START_RECORDING:
                mParticipantName = response.getSlot(SLOT_PARTICIPANT);
                if (mParticipantName != null)
                {
                    startRecording();
                    startRecurrentQuestions();
                }
                break;
            case ACTION_STOP_RECORDING:
                stopRecording();
                stopRecurrentQuestions();
                break;
            case ACTION_CLASSIFY_ACTIVITY_LONG:
            case ACTION_CLASSIFY_ACTIVITY_SHORT:
                String activity = response.toString();
                serviceHelper.setSensorsLabel(activity);
                break;
            default:
                break;
        }
    }



    private void startRecording()
    {
        stopRecording();
        RecordServiceHelper serviceHelper = RecordServiceHelper.newInstance(this);

        serviceHelper.startRecording(null);
        serviceHelper.startRecurrentSave(
                null,
                mParticipantName,
                null
        );
    }

    private void stopRecording()
    {
        RecordServiceHelper serviceHelper = RecordServiceHelper.newInstance(this);
        serviceHelper.saveZipClearFiles(mParticipantName);
        serviceHelper.stopRecurrentSave();
        serviceHelper.stopRecording();
    }

    private void startRecurrentQuestions()
    {
        stopRecurrentQuestions();
        ChatBotPreferences preferences = new ChatBotPreferences(this);
        long intervalMillis = preferences.recurrentEventMillis();
        mClassifyReceiver = new RepeatingBroadcast(this);
        mClassifyReceiver.start(intervalMillis, ((context, intent) -> {
            if (preferences.sendRecurrentEvent())
                chatbotOperation((chatbot) -> {
                    chatbot.sendEvent(preferences.recurrentEventName(), Maps.newHashMap(), this);
                });
        }));


        Log.i("CHATBOT", "Started recurring classify every "+intervalMillis+" milliseconds");
    }

    private void stopRecurrentQuestions()
    {
        if(mClassifyReceiver == null)
            return;
        mClassifyReceiver.stop();
        mClassifyReceiver = null;
    }

    private class Engine extends CanvasWatchFaceService.Engine
    {
        private static final long INVALIDATE_MILLIS = 200;

        private Bitmap mOnPicture;
        private Bitmap mOffPicture;

        private final Handler mHandler = AndroidUtils.newHandler();


        public Bitmap getChatbotImage()
        {
            return (isBusy())? mOnPicture : mOffPicture;
        }

        private void loopInvalidate()
        {
            invalidate();
            mHandler.postDelayed(this::loopInvalidate, INVALIDATE_MILLIS);
        }

        @Override
        public void onCreate(SurfaceHolder holder)
        {
            super.onCreate(holder);
            mOnPicture = BitmapFactory.decodeResource(getResources(), R.drawable.chatbot_on);
            mOffPicture = BitmapFactory.decodeResource(getResources(), R.drawable.chatbot_off);
            setWatchFaceStyle(new WatchFaceStyle.Builder(ChatbotWatchFace.this)
                    .setAcceptsTapEvents(true)
                    // other settings
                    .build());

            loopInvalidate();
        }


        @Override
        public void onTapCommand(int tapType, int x, int y, long eventTime)
        {
            switch (tapType) {
                case WatchFaceService.TAP_TYPE_TAP:
                    chatbotOperation((chatbot) -> chatbot.startListening(ChatbotWatchFace.this));
                    break;
                default:
                    super.onTapCommand(tapType, x, y, eventTime);
                    break;
            }
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode)
        {
            super.onAmbientModeChanged(inAmbientMode);
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds)
        {
            super.onDraw(canvas, bounds);
            Bitmap image = getChatbotImage();
            Rect size = new Rect(0, 0, image.getWidth(), image.getHeight());
            canvas.drawBitmap(image, size, bounds, null);
        }


        public Paint getPaint(Consumer<Paint> initializer)
        {
            Paint p = new Paint();
            if (initializer != null)
                initializer.accept(p);
            return p;
        }

        @Override
        public void onDestroy()
        {
            super.onDestroy();
            mHandler.getLooper().quit();
        }
    }

}
