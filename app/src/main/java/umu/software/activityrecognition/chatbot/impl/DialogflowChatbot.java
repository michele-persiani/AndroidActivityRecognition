package umu.software.activityrecognition.chatbot.impl;

import android.os.Handler;


import androidx.annotation.Nullable;

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
import java.io.InputStream;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;

import umu.software.activityrecognition.chatbot.Chatbot;
import umu.software.activityrecognition.chatbot.ChatbotResponse;
import umu.software.activityrecognition.shared.util.AndroidUtils;
import umu.software.activityrecognition.shared.util.Exceptions;
import umu.software.activityrecognition.shared.util.FunctionLock;
import umu.software.activityrecognition.shared.util.LogHelper;



/**
 * Chatbot implementation that uses Dialogflow. Requires a functioning Dialogflow agent and corresponding
 * API key
 */
public class DialogflowChatbot implements Chatbot
{
    private final Supplier<InputStream> credentials;
    private SessionsClient sessionsClient;
    private SessionName sessionName;
    private final String uuid = UUID.randomUUID().toString();

    private final Handler handler = AndroidUtils.newHandler();
    private final Handler mainHandler = AndroidUtils.newMainLooperHandler();

    private final FunctionLock lock = FunctionLock.newInstance();

    private Locale language = Locale.forLanguageTag("en-US");

    private final LogHelper log = LogHelper.newClassTag(this);


    public DialogflowChatbot(Supplier<InputStream> credentialsSupplier)
    {
        credentials = credentialsSupplier;
    }

    public DialogflowChatbot(String apiKey)
    {
        this(() -> new ByteArrayInputStream(apiKey.getBytes()));
    }

    public DialogflowChatbot(InputStream inputStream)
    {
        this(() -> inputStream);
    }

    @Override
    public void connect(@Nullable Consumer<Boolean> successCbk)
    {
        log.d("connect()");
        handler.post( () -> {
            lock.lock();
            if (isConnected()) {
                lock.unlock();
                return;
            }

            InputStream stream = credentials.get();

            boolean success = Exceptions.runCatch(() -> {
                GoogleCredentials credentials1 = GoogleCredentials.fromStream(stream).createScoped(Lists.newArrayList("https://www.googleapis.com/auth/cloud-platform"));

                String projectId = ((ServiceAccountCredentials) credentials1).getProjectId();

                SessionsSettings.Builder settingsBuilder = SessionsSettings.newBuilder();
                SessionsSettings sessionsSettings = settingsBuilder.setCredentialsProvider(FixedCredentialsProvider.create(credentials1)).build();
                sessionsClient = SessionsClient.create(sessionsSettings);
                sessionName = SessionName.of(projectId, uuid);
                stream.close();
            });
            lock.unlock();
            if (successCbk != null) mainHandler.post(() -> successCbk.accept(success));
        });
    }

    @Override
    public void disconnect(@Nullable Consumer<Boolean> successCbk)
    {
        log.d("disconnect()");
        handler.post( () -> {
            lock.lock();
            sessionsClient.close();
            sessionsClient = null;
            sessionName = null;
            lock.unlock();
            if (successCbk != null) mainHandler.post(() -> successCbk.accept(true));
        });
    }

    @Override
    public boolean isConnected()
    {
        return lock.withLock( () -> sessionsClient != null && !sessionsClient.isTerminated() && !sessionsClient.isShutdown());
    }

    /**
     * Send a message to the Dialogflow chatbot. The message will be transformed into a DetectIntentRequest request.
     * @param message the message to send
     * @param cbkResponse the callback receiving the chatbot's response
     */
    @Override
    public void sendMessage(CharSequence message, @Nullable Consumer<ChatbotResponse> cbkResponse)
    {
        log.d("sendMessage() (%s)", message);
        sendMessage(builder -> {
            builder.setText(
                    TextInput.newBuilder()
                            .setText(message.toString())
                            .setLanguageCode(language.toLanguageTag())
            );
        }, cbkResponse);
    }

    @Override
    public void sendEvent(String name, @Nullable Map<String, String> params, @Nullable Consumer<ChatbotResponse> cbkResponse)
    {
        log.d("sendEvent() (%s)", name);
        Map<String, String> finalParams = (params != null)? params : Maps.newHashMap();
        sendMessage(builder -> {
            Map<String, Value> paramValues = Maps.transformValues(finalParams, (x) -> {
                assert x != null;
                return Value.newBuilder().setStringValue(x).build();
            });
            EventInput event = EventInput.newBuilder()
                    .setName(name)
                    .setParameters(Struct.newBuilder().putAllFields(paramValues))
                    .setLanguageCode(language.toLanguageTag())
                    .build();
            builder.setEvent(event);
        }, cbkResponse);

    }



    @Override
    public Locale getLanguage()
    {
        return language;
    }

    @Override
    public void setLanguage(Locale language)
    {
        log.i("setLanguage() (%s)", language.toLanguageTag());
        this.language = language;
    }


    private void sendMessage(Consumer<QueryInput.Builder> messageBuilder, Consumer<ChatbotResponse> callback)
    {
        handler.post( () -> {
            if (!isConnected()) {
                mainHandler.post(() -> callback.accept(ChatbotResponse.forError("Chatbot not connected")));
                return;
            }

            QueryInput.Builder queryBuilder = QueryInput.newBuilder();
            messageBuilder.accept(queryBuilder);
            QueryInput query = queryBuilder.build();

            DetectIntentRequest detectIntentRequest = DetectIntentRequest.newBuilder()
                    .setSession(sessionName.toString())
                    .setQueryInput(query)
                    .build();

            try {
                DetectIntentResponse response = sessionsClient.detectIntent(detectIntentRequest);
                ChatbotResponse answer = buildResponse(response);
                mainHandler.post(() -> callback.accept(answer));
            } catch (Exception e) {
                mainHandler.post(() -> callback.accept(
                        ChatbotResponse.forError(
                                String.format("Exception in sendMessage(): %s, %s", e.getClass().getSimpleName(), e.getMessage()))
                        )
                );
            }
        });
    }

    private ChatbotResponse buildResponse(DetectIntentResponse response)
    {
        QueryResult result = response.getQueryResult();
        ChatbotResponse answer = new ChatbotResponse();
        answer.setIntent(result.getIntent().getDisplayName());
        answer.setPromptText(result.getQueryText());
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
