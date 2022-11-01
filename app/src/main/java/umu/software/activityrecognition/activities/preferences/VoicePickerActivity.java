package umu.software.activityrecognition.activities.preferences;

import android.os.Bundle;
import android.speech.tts.Voice;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.Nullable;
import com.google.common.collect.Lists;
import umu.software.activityrecognition.R;
import umu.software.activityrecognition.activities.shared.MultiItemListViewActivity;
import umu.software.activityrecognition.preferences.SpeechServicePreferences;
import umu.software.activityrecognition.services.speech.SpeechService;
import umu.software.activityrecognition.shared.services.ServiceConnectionHandler;
import umu.software.activityrecognition.shared.util.VibratorManager;

import java.util.List;
import java.util.Locale;



/**
 * Activity to listen to and select voices for the SpeechService
 */
public class VoicePickerActivity extends MultiItemListViewActivity
{
    private List<Voice> mVoices = Lists.newArrayList();
    private ServiceConnectionHandler<SpeechService.SpeechBinder> mBinder;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        mBinder = SpeechService.newConnection(this, Locale.ENGLISH);

    }

    @Override
    protected void onStart()
    {
        super.onStart();
        mBinder.enqueue(binder -> {
                    mVoices = binder.getAvailableVoices();
                    refreshListView();
                })
                .bind(SpeechService.class);
    }

    @Override
    protected void onStop()
    {
        super.onStop();
        mBinder.unbind();
    }


    @Override
    protected int getMasterLayout()
    {
        return R.layout.holder_voice_select;
    }

    @Override
    protected void registerBinders()
    {
        registerBinding(0, R.id.linearLayout_title, (holder, position) -> {});


        registerDefaultBinding(R.id.linearLayout_voice, (holder, position) -> {


            Voice v = mVoices.get(position-1);

            SpeechServicePreferences prefs = new SpeechServicePreferences(this);

            TextView voiceNameTextView = holder.getView().findViewById(R.id.textView1);

            voiceNameTextView.setText(String.format("%s", v.getName()));

            holder.getView().setOnClickListener(view -> {
                mBinder.applyBound(binder -> {
                    VibratorManager.getInstance(this).vibrateLightClick();
                    binder.setVoice(v);
                    binder.say(
                            getString(R.string.voice_picker_prompt),
                            null
                    );
                });
            });


            ImageButton btn = holder.getView().findViewById(R.id.button_select);
            btn.setOnClickListener(view -> {
                mBinder.applyBound(binder -> {
                    prefs.voiceName(binder.getTargetLanguage()).set(v.getName());
                    finish();
                });
            });


            List<String> requirements = Lists.newArrayList(
                    String.format("Requires internet: %s", v.isNetworkConnectionRequired()),
                    String.format("Quality: %s", v.getQuality()),
                    String.format("Latency: %s", v.getLatency())
            );


            TextView infoTextView = holder.getView().findViewById(R.id.textView2);
            infoTextView.setText(String.join(System.lineSeparator(), requirements));

        });
    }



    @Override
    protected int getItemCount()
    {
        return mVoices.size() + 1;
    }


}
