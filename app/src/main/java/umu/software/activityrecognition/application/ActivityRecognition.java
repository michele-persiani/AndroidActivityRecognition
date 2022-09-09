package umu.software.activityrecognition.application;

import android.content.Context;
import android.os.Binder;
import android.widget.Toast;

import com.google.common.collect.Lists;

import java.util.List;
import java.util.function.Predicate;

import umu.software.activityrecognition.chatbot.ChatbotResponse;
import umu.software.activityrecognition.shared.services.ServiceBinder;
import umu.software.activityrecognition.shared.services.ServiceConnectionHandler;
import umu.software.activityrecognition.services.chatbot.DialogflowService;
import umu.software.activityrecognition.services.chatbot.DialogflowServiceHelper;
import umu.software.activityrecognition.services.chatbot.PingChatbotService;
import umu.software.activityrecognition.services.recordings.RecordServiceHelper;


/**
 * Singleton defining te main macro-procedures to perform during the application lifecycle, such as
 * starting/stopping the chatbot, recordings etc.
 */
public class ActivityRecognition
{
    public static final String ACTION_START_RECORDING           = "StartRecording";
    public static final String ACTION_STOP_RECORDING            = "StopRecording";
    public static final String INTENT_CLASSIFY_ACTIVITY_SHORT   = "ActionClassifyActivityShort";
    public static final String INTENT_CLASSIFY_ACTIVITY_LONG    = "ActionClassifyActivityLong";
    public static final String ACTION_START_QUESTIONS           = "ActionStartRecurrentQuestions";
    public static final String ACTION_STOP_QUESTIONS            = "ActionStopRecurrentQuestions";

    public static final String EVENT_START_QUESTIONS            = "EventStartRecurrentQuestions";
    public static final String EVENT_STOP_QUESTIONS             = "EventStopRecurrentQuestions";
    public static final String EVENT_CLASSIFY_ACTIVITY          = "EventClassifyActivityShort";
    public static final String EVENT_WELCOME                    = "Welcome";
    public static final String EVENT_GOODBYE                    = "Goodbye";


    public static final String SLOT_CONSENT                     = "consent";
    public static final String SLOT_PARTICIPANT                 = "participant";

    private static final String CHATBOT_USAGE                   = "CHATBOT_USAGE";

    private final RecordServiceHelper mRecordingsHelper;
    private final Context mContext;
    private String mParticipantName;
    private final ServiceConnectionHandler<ServiceBinder<DialogflowService>> mDialogflowConnection;
    private final ServiceConnectionHandler<Binder> mPingServiceConnection;

    private static final Object sLock = new Object();
    private static ActivityRecognition sInstance;


    private ActivityRecognition(Context context)
    {
        context = context.getApplicationContext();
        mContext = context;
        mRecordingsHelper = RecordServiceHelper.newInstance(context);
        mDialogflowConnection = new ServiceConnectionHandler<>(context);
        mPingServiceConnection = new ServiceConnectionHandler<>(context);
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



    public void startChatbot()
    {
        mDialogflowConnection.unbind();
        mDialogflowConnection
                .enqueue(binder -> {
                    DialogflowService service = binder.getService();

                    service.setResponseCallback(
                            ACTION_START_QUESTIONS,
                            newResponseFilter(true, ACTION_START_QUESTIONS),
                            (context, response) -> {
                                if (response.getSlot(SLOT_CONSENT).equalsIgnoreCase("yes"))
                                    startRecurrentQuestions();
                            }
                    );

                    service.setResponseCallback(
                            ACTION_STOP_QUESTIONS,
                            newResponseFilter(true, ACTION_STOP_QUESTIONS),
                            (context, response) -> {
                                if (response.getSlot(SLOT_CONSENT).equalsIgnoreCase("yes"))
                                    stopRecurrentQuestions();
                            }
                    );

                    service.setResponseCallback(
                            INTENT_CLASSIFY_ACTIVITY_LONG + INTENT_CLASSIFY_ACTIVITY_SHORT,
                            newResponseFilter(false, INTENT_CLASSIFY_ACTIVITY_LONG, INTENT_CLASSIFY_ACTIVITY_SHORT),
                            (context, response) -> {
                                if (isAskingQuestions()) {
                                    String activity = response.toString();
                                    mRecordingsHelper.setSensorsLabel(activity);
                                }
                            }
                    );
                }).bind(DialogflowService.class);
        mRecordingsHelper.recordEvent(CHATBOT_USAGE, event -> event.put("chatbot_on", true));
    }

    public void sendWelcomeEvent()
    {
        mDialogflowConnection.applyBound(binder -> binder.getService().sendChatbotEvent(EVENT_WELCOME, null));
    }


    public void sendGoodbyeEvent()
    {
        mDialogflowConnection.applyBound(binder -> binder.getService().sendChatbotEvent(EVENT_GOODBYE, null));
    }


    public void sendClassifyEvent()
    {
        mDialogflowConnection.applyBound(binder -> binder.getService().sendChatbotEvent(EVENT_CLASSIFY_ACTIVITY, null));
    }


    public void shutdownChatbot()
    {
        stopRecurrentQuestions();
        mDialogflowConnection.unbind();
        mRecordingsHelper.setSensorsLabel(null);
        mRecordingsHelper.recordEvent(CHATBOT_USAGE, event -> event.put("chatbot_on", false));
    }


    public void askStartRecurrentQuestions()
    {
        if (isAskingQuestions())
            return;
        mDialogflowConnection.applyBound(binder -> binder.getService().sendChatbotEvent(EVENT_START_QUESTIONS, null));
    }

    public void askStopRecurrentQuestions()
    {
        mDialogflowConnection.applyBound(binder -> binder.getService().sendChatbotEvent(EVENT_STOP_QUESTIONS, null));
    }

    public void startRecurrentQuestions()
    {
        mRecordingsHelper.bind().enqueue(binder -> {
            if (binder.getService().isRecording()) {
                PingChatbotService.startPingDialogflowService(mContext);
                mPingServiceConnection.bind(PingChatbotService.class);
            }
        }).enqueueUnbind();
        mRecordingsHelper.recordEvent(CHATBOT_USAGE, event -> event.put("asking_questions", true));
    }

    public void stopRecurrentQuestions()
    {
        PingChatbotService.stopPingDialogflowService(mContext);
        mPingServiceConnection.unbind();
        mRecordingsHelper.setSensorsLabel(null);
    }


    public boolean isAskingQuestions()
    {
        return mPingServiceConnection.applyBoundFunction( binder -> {
            ServiceBinder<PingChatbotService> serviceBInder = (ServiceBinder<PingChatbotService>) binder;
            return serviceBInder.getService().isSendingPingEvents();
        }, false);
    }

    public void startListening()
    {
        mDialogflowConnection.applyBound(binder -> {
            boolean connected = binder.getService().isConnected();
            if (!connected) {
                Toast.makeText(mContext, "Chatbot not connected", Toast.LENGTH_SHORT).show();
            }
            binder.getService().startListening();
            mRecordingsHelper.recordEvent(CHATBOT_USAGE, event -> {
                event.put("start_listening", true);
                event.put("connected", connected);
            });
        });
    }


    public boolean isChatbotBusy()
    {
        Boolean busy = mDialogflowConnection.applyBoundFunction((binder) ->{
            return binder.getService().applyIfConnected(speechChatbot -> {
                        return speechChatbot.isConnected() && speechChatbot.isBusy();
                    },
                    null);
        }, null);
        return (busy != null)? busy : true;
    }




    public void startRecordService()
    {
        mRecordingsHelper.startRecording(null);
        mRecordingsHelper.startRecurrentSave();

        mRecordingsHelper.setupBroadcastReceiver(
                CHATBOT_USAGE,
                null
        );
        mRecordingsHelper.recordEvent(CHATBOT_USAGE, event -> event.put("recording", true));
    }

    public void stopRecordService()
    {
        mRecordingsHelper.recordEvent(CHATBOT_USAGE, event -> event.put("recording", false));
        stopRecurrentQuestions();
        mRecordingsHelper.removeBroadcastReceiver(CHATBOT_USAGE, null);
        mRecordingsHelper.saveZipClearFiles();
        mRecordingsHelper.stopRecurrentSave();
        mRecordingsHelper.stopRecording();
    }


    private static Predicate<ChatbotResponse> newResponseFilter(boolean requiresAllSlots, String... actionName)
    {
        List<String> actions = Lists.newArrayList(actionName);
        Predicate<ChatbotResponse> testActionName = response -> actions.size() == 0 || actions.contains(response.getAction());

        return response -> testActionName.test(response) && (response.allSlotsSet() || !requiresAllSlots);
    }

}
