package umu.software.activityrecognition.chatbot;

import android.os.Handler;


import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.dialogflow.v2.DetectIntentRequest;
import com.google.cloud.dialogflow.v2.DetectIntentResponse;
import com.google.cloud.dialogflow.v2.EventInput;
import com.google.cloud.dialogflow.v2.QueryInput;
import com.google.cloud.dialogflow.v2.QueryResult;
import com.google.cloud.dialogflow.v2.SessionName;
import com.google.cloud.dialogflow.v2.SessionsClient;
import com.google.cloud.dialogflow.v2.SessionsSettings;
import com.google.cloud.dialogflow.v2.TextInput;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.protobuf.Struct;
import com.google.protobuf.Value;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Semaphore;
import java.util.function.Consumer;
import java.util.function.Supplier;

import umu.software.activityrecognition.shared.AndroidUtils;
import umu.software.activityrecognition.shared.Exceptions;

/**
 * Chatbot that uses Dialogflow
 */
public class DialogflowChatBot implements ChatBot
{
    private final Supplier<InputStream> credentials;
    private SessionsClient sessionsClient;
    private SessionName sessionName;
    private final String uuid = UUID.randomUUID().toString();

    private final Handler handler = AndroidUtils.newHandler();
    private final Handler mainHandler = AndroidUtils.newMainLooperHandler();


    private Locale language = Locale.forLanguageTag("en-US");


    public DialogflowChatBot(Supplier<InputStream> credentialsSupplier)
    {
        credentials = credentialsSupplier;
    }

    public DialogflowChatBot(String apiKey)
    {
        this(() -> new ByteArrayInputStream(apiKey.getBytes()));
    }

    public DialogflowChatBot(InputStream inputStream)
    {
        this(() -> inputStream);
    }

    @Override
    public void connect(Consumer<Boolean> successCbk)
    {
        if (isConnected())
            return;

        InputStream stream = credentials.get();

        boolean success = Exceptions.runCatch(() -> {
            GoogleCredentials credentials1 = GoogleCredentials.fromStream(stream)
                    .createScoped(Lists.newArrayList("https://www.googleapis.com/auth/cloud-platform"));

            String projectId = ((ServiceAccountCredentials) credentials1).getProjectId();

            SessionsSettings.Builder settingsBuilder = SessionsSettings.newBuilder();
            SessionsSettings sessionsSettings = settingsBuilder.setCredentialsProvider(
                    FixedCredentialsProvider.create(credentials1)).build();
            sessionsClient = SessionsClient.create(sessionsSettings);
            sessionName = SessionName.of(projectId, uuid);
            stream.close();
        });

        mainHandler.post(() -> successCbk.accept(success));
    }

    @Override
    public void disconnect(Consumer<Boolean> successCbk)
    {
        sessionsClient.close();
        sessionsClient = null;
        sessionName = null;
        mainHandler.post(() -> successCbk.accept(true));
    }

    @Override
    public boolean isConnected()
    {
        return sessionsClient != null && !sessionsClient.isTerminated() && !sessionsClient.isShutdown();
    }

    /**
     * Send a message to the Dialogflow chatbot. The message will be transformed into a DetectIntentRequest request.
     * @param message the message to send
     * @param cbkResponse the callback receiving the chatbot's response
     */
    @Override
    public void sendMessage(CharSequence message, Consumer<ChatbotResponse> cbkResponse)
    {
        handler.post(() -> {

            if (!isConnected())
            {
                mainHandler.post(() -> cbkResponse.accept(
                        ChatbotResponse.forError("Chatbot not connected"))
                );
                return;
            }

            QueryInput queryInput = QueryInput.newBuilder()
                    .setText(TextInput.newBuilder().setText(message.toString()).setLanguageCode(language.toLanguageTag()))
                    .build();

            DetectIntentRequest detectIntentRequest =
                    DetectIntentRequest.newBuilder()
                            .setSession(sessionName.toString())
                            .setQueryInput(queryInput)
                            .build();


            try {
                DetectIntentResponse response = sessionsClient.detectIntent(detectIntentRequest);
                ChatbotResponse answer = buildResponse(response);
                answer.setPromptText(message.toString());
                mainHandler.post(() -> cbkResponse.accept(answer));
            }
            catch (Exception e)
            {
                mainHandler.post(() -> cbkResponse.accept(ChatbotResponse.forError("Error while sending detectIntent() request.")));
            }
        });
    }

    @Override
    public void sendEvent(String name, Map<String, String> params, Consumer<ChatbotResponse> cbkResponse)
    {
        handler.post( () -> {

            Map<String, Value> paramValues = Maps.transformValues(params, (x) -> {
                assert x != null;
                return Value.newBuilder().setStringValue(x).build();
            });
            EventInput event = EventInput.newBuilder()
                    .setName(name)
                    .setParameters(Struct.newBuilder().putAllFields(paramValues))
                    .setLanguageCode(language.toLanguageTag())
                    .build();
            QueryInput queryInput = QueryInput.newBuilder()
                    .setEvent(event)
                    .build();


            DetectIntentRequest detectIntentRequest =
                    DetectIntentRequest.newBuilder()
                            .setSession(sessionName.toString())
                            .setQueryInput(queryInput)
                            .build();


            try
            {
                DetectIntentResponse response = sessionsClient.detectIntent(detectIntentRequest);
                ChatbotResponse answer = buildResponse(response);
                mainHandler.post(() -> cbkResponse.accept(answer));
            }
            catch (Exception e)
            {
                mainHandler.post(() -> cbkResponse.accept(ChatbotResponse.forError("Error while sending detectIntent() request.")));
            }
        });
    }



    @Override
    public Locale getLanguage()
    {
        return language;
    }

    @Override
    public void setLanguage(Locale language)
    {
        this.language = language;
    }


    private ChatbotResponse buildResponse(DetectIntentResponse response)
    {
        QueryResult result = response.getQueryResult();
        ChatbotResponse answer = new ChatbotResponse();
        answer.setIntent(result.getIntent().getDisplayName());
        answer.setAnswerText(result.getFulfillmentText());
        answer.setAction(result.getAction());
        for(Map.Entry<String, Value> p : result.getParameters().getFieldsMap().entrySet()) {
            String slotName = p.getKey();
            String slotValue = p.getValue().getStringValue();
            if (slotValue.equals("")) slotValue = null;
            answer.setSlot(slotName, slotValue);
        }
        return answer;
    }

}
