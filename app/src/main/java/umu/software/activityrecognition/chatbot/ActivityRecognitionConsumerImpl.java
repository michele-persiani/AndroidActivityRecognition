package umu.software.activityrecognition.chatbot;

import android.content.Context;
import android.util.Log;

import com.google.common.collect.Maps;

import java.util.function.Consumer;

import umu.software.activityrecognition.config.ChatBotPreferences;
import umu.software.activityrecognition.services.DialogflowService;
import umu.software.activityrecognition.services.RecordServiceHelper;
import umu.software.activityrecognition.services.ServiceConnectionHandler;
import umu.software.activityrecognition.shared.RepeatingBroadcast;

public class ActivityRecognitionConsumerImpl implements Consumer<ChatbotResponse>
{
    public static final String ACTION_START_RECORDING           = "StartRecording";
    public static final String ACTION_STOP_RECORDING            = "StopRecording";
    public static final String ACTION_CLASSIFY_ACTIVITY_SHORT   = "ClassifyActivityShort";
    public static final String ACTION_CLASSIFY_ACTIVITY_LONG    = "ClassifyActivityLong";

    public static final String EVENT_CLASSIFY_ACTIVITY          = "ClassifyActivityShort";

    public static final String SLOT_PARTICIPANT                 = "participant";


    private final Context mContext;
    private final RecordServiceHelper mServiceHelper;
    private final ChatBotPreferences mPreferences;
    private String mParticipantName;
    private RepeatingBroadcast mClassifyReceiver;
    private final ServiceConnectionHandler<DialogflowService.Binder> mConnection;


    public ActivityRecognitionConsumerImpl(Context context)
    {
        mContext = context.getApplicationContext();
        mServiceHelper = RecordServiceHelper.newInstance(mContext);
        mPreferences = new ChatBotPreferences(mContext);
        mConnection = DialogflowService.getConnection(mContext,
                mPreferences.getApiKey(),
                mPreferences.getLanguage(),
                mPreferences.getVoiceSpeed()
        );
    }


    @Override
    public void accept(ChatbotResponse response)
    {
        String action = response.getAction();

        if (response.hasError() || action == null)
            return;

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
                startRecurrentQuestions();
                break;
            case ACTION_CLASSIFY_ACTIVITY_LONG:
            case ACTION_CLASSIFY_ACTIVITY_SHORT:
                String activity = response.toString();
                mServiceHelper.setSensorsLabel(activity);
                break;
            default:
                break;
        }
    }

    public void sendClassifyEvent()
    {
        chatbotOperation((chatbot) -> {
            chatbot.sendEvent(EVENT_CLASSIFY_ACTIVITY, Maps.newHashMap(), this);
        });
    }

    public void bindChatbotService()
    {
        mConnection.bind(DialogflowService.class);
    }

    public void unbindChatbotService()
    {
        mConnection.unbind();
    }

    private void chatbotOperation(Consumer<SpeechChatbot> operation)
    {
        mConnection.applyBound((binder) -> {
            if (binder.getService().isConnected())
                operation.accept(binder.getService().getChatBot());
        });
    }

    public void startRecording()
    {
        mServiceHelper.startRecording(null);
        mServiceHelper.startRecurrentSave(
                null,
                mParticipantName,
                null
        );
    }

    public void stopRecording()
    {
        mServiceHelper.saveZipClearFiles(mParticipantName);
        mServiceHelper.stopRecurrentSave();
        mServiceHelper.stopRecording();
    }


    public void startRecurrentQuestions()
    {
       stopRecurrentQuestions();
       if(!mPreferences.sendRecurrentEvent())
           return;
        mClassifyReceiver = new RepeatingBroadcast(mContext);
        mClassifyReceiver.start(
                mPreferences.recurrentEventMillis(),
                ((context, intent) -> {
                    if (mPreferences.sendRecurrentEvent() && mConnection.isBound())
                        chatbotOperation((chatbot) -> {
                            chatbot.sendEvent(mPreferences.recurrentEventName(), Maps.newHashMap(), this);
                        });
                    else
                        stopRecurrentQuestions();
                }));


        Log.i("CHATBOT", "Started recurring classify every "+mPreferences.recurrentEventMillis()+" milliseconds");
    }

    public void stopRecurrentQuestions()
    {
        if(mClassifyReceiver == null)
            return;
        mClassifyReceiver.stop();
        mClassifyReceiver = null;
    }

    public boolean isRepeatingClassify()
    {
        return mClassifyReceiver != null;
    }


    public void startListening()
    {
        chatbotOperation((chatbot) -> {
            chatbot.startListening(this);
        });
    }


    public boolean isBusy()
    {
        Boolean busy = mConnection.applyBoundFunction((binder) ->{
            SpeechChatbot chatbot = binder.getService().getChatBot();
            return chatbot != null && chatbot.isConnected() && chatbot.isBusy();
        });
        return (busy != null)? busy : false;
    }
}
