package umu.software.activityrecognition.activities.preferences;

import android.annotation.SuppressLint;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Handler;
import android.widget.SeekBar;
import android.widget.TextView;

import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.lifecycle.Lifecycle;
import androidx.wear.ambient.AmbientModeSupport;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

import umu.software.activityrecognition.R;
import umu.software.activityrecognition.activities.shared.MultiItemListViewActivity;
import umu.software.activityrecognition.preferences.ClassifyRecordingsPreferences;
import umu.software.activityrecognition.shared.audio.WaveRecorder;
import umu.software.activityrecognition.shared.lifecycles.ExclusiveResourceLifecycle;
import umu.software.activityrecognition.shared.lifecycles.WakeLockLifecycle;
import umu.software.activityrecognition.shared.resourceaccess.ExclusiveResource;
import umu.software.activityrecognition.shared.util.AndroidUtils;
import umu.software.activityrecognition.shared.util.VibratorManager;

public class AudioRMSActivity extends MultiItemListViewActivity
{
    public static final long REFRESH_DELAY_MILLIS = 100;
    public static final long MAX_DURATION_MINUTES = 2;

    private TextView mTextView;
    private WaveRecorder mRecorder;
    private Handler mHandler;

    private double mPrevRms = 0;
    private ClassifyRecordingsPreferences mPreferences;
    private int mTextViewFlags;


    @Override
    protected int getItemCount()
    {
        return 4;
    }

    @Override
    protected int getMasterLayout()
    {
        return R.layout.holder_voice_recorder;
    }

    @Override
    protected void registerBinders()
    {
        registerBinding(0, R.id.linearLayout_rms, true, (holder, position) -> {
            mTextView = holder.getView().findViewById(R.id.textView_rms);
            mTextViewFlags = mTextView.getPaintFlags();
        });

        registerSettingBinding(1,
                R.string.speech_silence_db_threshold_title,
                R.string.speech_silence_db_threshold_summary,
                0,
                100,
                () -> mPreferences.silenceDbThreshold().get(),
                (v) -> mPreferences.silenceDbThreshold().set(v)
        );

        registerSettingBinding(2,
                R.string.speech_on_speech_vibration_length_title,
                R.string.speech_on_speech_vibration_length_summary,
                0,
                30,
                () -> mPreferences.onSpeechVibrationLength().get(),
                (v) -> mPreferences.onSpeechVibrationLength().set(v)
        );

        registerSettingBinding(3,
                R.string.speech_on_speech_vibration_min_db_title,
                R.string.speech_on_speech_vibration_min_db_summary,
                0,
                100,
                () -> (int) (mPreferences.onSpeechVibrationMinDbDelta().get() * 100),
                (v) -> mPreferences.onSpeechVibrationMinDbDelta().set(v / 100.f)
        );
    }

    private void registerSettingBinding(int position, int titleResId, int summaryResId, int min, int max, Supplier<Integer> preferenceReader, Consumer<Integer> preferenceWriter)
    {

        registerBinding(position, R.id.linearLayout_option, (holder, pos) -> {

            ((TextView)holder.getView().findViewById(R.id.textView_title)).setText(titleResId);
            ((TextView)holder.getView().findViewById(R.id.textView_summary)).setText(summaryResId);

            SeekBar seekBar = holder.getView().findViewById(R.id.seekBar);
            seekBar.setMin(min);
            seekBar.setMax(max);
            seekBar.setProgress(preferenceReader.get());
            seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener()
            {
                @Override
                public void onProgressChanged(SeekBar seekBar, int i, boolean b)
                {
                    preferenceWriter.accept(i);
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar){}

                @Override
                public void onStopTrackingTouch(SeekBar seekBar){}
            });
        });
    }



    @Override
    protected void onViewRecycled(ViewHolder holder)
    {
        super.onViewRecycled(holder);
        TextView text = holder.getView().findViewById(R.id.textView_rms);
        if (text != null)
            mTextView = null;
    }




    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);


        ExclusiveResourceLifecycle audioLifecycle = new ExclusiveResourceLifecycle();
        audioLifecycle.registerToken(audioLifecycle, ExclusiveResource.PRIORITY_HIGH, ExclusiveResource.AUDIO_INPUT);
        getLifecycle().addObserver(audioLifecycle);
        boolean acquired = audioLifecycle.getToken(audioLifecycle).acquire(false);
        if (!acquired)
            Toast.makeText(this, "Couldn't acquire audio token", Toast.LENGTH_SHORT).show();


        mRecorder = new WaveRecorder();
        mRecorder.setMaxRecordingLength(
                TimeUnit.MILLISECONDS.convert(MAX_DURATION_MINUTES, TimeUnit.MINUTES)
        );
        mHandler = AndroidUtils.newMainLooperHandler();

        if (isOnWearable())
            AmbientModeSupport.attach(this);

        getLifecycle().addObserver(WakeLockLifecycle.newPartialWakeLock(this));

        mPreferences = new ClassifyRecordingsPreferences(this);
    }


    @Override
    protected void onResume()
    {
        super.onResume();
        mRecorder.setCallback(new WaveRecorder.Callback()
        {
            @Override
            public void onUpdate()
            {
                double rms = mRecorder.getRMS();
                WaveRecorder.Callback.super.onUpdate();
                if (rms > mPreferences.silenceDbThreshold().get() && rms > mPrevRms + mPreferences.onSpeechVibrationMinDbDelta().get())
                    VibratorManager.getInstance(AudioRMSActivity.this).vibrate(mPreferences.onSpeechVibrationLength().get());
                mPrevRms = rms;
            }

            @Override
            public void onFinish()
            {
            }
        });

        mRecorder.startRecording();
        mHandler.post(this::updateText);
    }


    @SuppressLint({"DefaultLocale", "ResourceAsColor"})
    public void updateText()
    {
        if (!getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.RESUMED))
            return;

        mTextView.setText(String.format("%.1f", mRecorder.getRMS()));

        if (mRecorder.getRMS() > mPreferences.silenceDbThreshold().get())
            mTextView.setPaintFlags(mTextViewFlags | Paint.UNDERLINE_TEXT_FLAG);
        else
            mTextView.setPaintFlags(mTextViewFlags);

        mHandler.postDelayed(this::updateText, REFRESH_DELAY_MILLIS);
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        mRecorder.stopRecording();
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        mPreferences.clearListeners();
    }
}
