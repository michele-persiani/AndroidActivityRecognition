package umu.software.activityrecognition.preferences;

import android.content.Context;

import umu.software.activityrecognition.R;
import umu.software.activityrecognition.shared.util.AndroidUtils;
import umu.software.activityrecognition.shared.preferences.Preference;
import umu.software.activityrecognition.shared.preferences.PreferencesModule;

public class DialogflowServicePreferences extends PreferencesModule
{

    public DialogflowServicePreferences(Context context)
    {
        super(context);
    }


    @Override
    protected void initialize()
    {
        dialogflowApiKey().init(
                AndroidUtils.readRawResourceFile(mContext, R.raw.dialogflow_credentials)
        );
    }


    public Preference<String> dialogflowApiKey()
    {
        return getString(R.string.dialogflow_api_key);
    }



}
