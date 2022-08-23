package umu.software.activityrecognition.preferences;

import android.content.Context;

import umu.software.activityrecognition.R;
import umu.software.activityrecognition.shared.preferences.Preference;
import umu.software.activityrecognition.shared.preferences.PreferencesModule;

public class PingEventServicePreferences extends PreferencesModule
{

    public PingEventServicePreferences(Context context)
    {
        super(context);
    }


    @Override
    protected void initialize()
    {
        sendPingEvent().init(
                getResources().getBoolean(R.bool.ping_event_default_send_ping)
        );
        pingEventMinutes().init(
                getResources().getInteger(R.integer.ping_event_default_ping_minutes)
        );
        pingEventName().init(
                getResources().getString(R.string.ping_event_default_event_name)
        );
    }



    public Preference<Boolean> sendPingEvent()
    {
        return getBoolean(R.string.chatbot_send_recurrent_event);
    }


    public Preference<Integer> pingEventMinutes()
    {
        return getInt(R.string.chatbot_recurrent_event_minutes);
    }


    public Preference<String> pingEventName()
    {
        return getString(R.string.chatbot_recurrent_event_name);
    }
}
