package umu.software.activityrecognition.shared.util;

import android.content.Context;
import android.os.Handler;
import android.os.VibrationEffect;
import android.os.Vibrator;

import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

import umu.software.activityrecognition.R;
import umu.software.activityrecognition.shared.preferences.NamedSharedPreferences;
import umu.software.activityrecognition.shared.preferences.Preference;
import umu.software.activityrecognition.shared.preferences.PreferenceFactory;


/**
 * Singleton for sending vibrations. Whether vibrations are enabled is set through preferences
 */
public class VibratorManager
{

    public class FuzzyVibrator implements Runnable
    {

        public final static long DEFAULT_VIBRATION_LENGTH_MILLIS = 15;
        public final static long DEFAULT_RANDOM_DELAY_RANGE_MILLIS = 800;

        private final Handler mHandler;
        boolean mStopped = true;
        boolean mDestroyed = false;
        private long mVibratelength = DEFAULT_VIBRATION_LENGTH_MILLIS;
        private long mRandomDelayRange = DEFAULT_RANDOM_DELAY_RANGE_MILLIS;

        FuzzyVibrator(Handler handler)
        {
            mHandler = handler;
        }


        @Override
        public void run()
        {
            boolean stopped;
            synchronized (this)
            {
                stopped = mStopped || mDestroyed;
            }
            if (stopped)
                return;
            vibrate(mVibratelength);

            long delay = Math.max(mVibratelength, ThreadLocalRandom.current().nextLong(mRandomDelayRange));
            mHandler.postDelayed(this, delay);

        }


        public void setParams(long vibrateLength, long randomDelayRange)
        {
            mVibratelength = vibrateLength;
            mRandomDelayRange = Math.max(vibrateLength, randomDelayRange);
        }

        public synchronized void start()
        {
            if(isRunning())
                return;
            mStopped = false;
            mHandler.post(this);
        }

        public synchronized boolean isRunning()
        {
            return !mStopped;
        }

        public synchronized void stop()
        {
            mStopped = true;
        }

        public synchronized void destroy()
        {
            mDestroyed = false;
        }

        @Override
        protected void finalize() throws Throwable
        {
            super.finalize();
            destroy();
        }
    }


    private static VibratorManager sInstance;
    private final Context mContext;
    private Preference<Boolean> mPreference;

    private final Handler mHandler;


    private VibratorManager(Context context)
    {
        mContext = context.getApplicationContext();
        mHandler = AndroidUtils.newHandler();

        setEnabledPreferenceKey(R.string.enable_vibration);
        mPreference.init(
                mContext.getResources().getBoolean(R.bool.default_enable_vibration)
        );
    }


    public static VibratorManager getInstance(Context context)
    {
        if (sInstance == null)
            sInstance = new VibratorManager(context.getApplicationContext());
        return sInstance;
    }

    /**
     * Sets the key of the preference deciding whether VibratorManager is enabled
     * @param key
     */
    public void setEnabledPreferenceKey(String key)
    {
        if (mPreference != null)
            mPreference.clearListeners();

        mPreference = PreferenceFactory
                .newInstance(
                        NamedSharedPreferences
                                .DEFAULT_PREFERENCES
                                .getInstance(mContext)
                ).booleanPreference(key);
    }


    public void setEnabledPreferenceKey(int stringKeyResId)
    {
        setEnabledPreferenceKey(mContext.getString(stringKeyResId));
    }


    public void vibrate(long milliseconds)
    {
        executeIfEnabled(v -> v.vibrate(milliseconds));
    }


    public void vibrate(long[] pattern)
    {
        executeIfEnabled(v -> v.vibrate(VibrationEffect.createWaveform(pattern, -1)));
    }


    public void vibrateLightClick()
    {
        vibrate(new long[]{0, 30});
    }


    public void vibrateHeavyClick()
    {
        vibrate(new long[]{0, 60});
    }


    public void vibrateDoubleClick()
    {
        vibrate(new long[]{0, 30, 150, 30});
    }


    public void vibrateLongTick()
    {
        vibrate(new long[]{0, 250});
    }


    public boolean isEnabled()
    {
        return mPreference.get();
    }


    /**
     * Sets whether vibration is enabled
     * @param enabled whether vibration is enabled
     */
    public void setEnabled(boolean enabled)
    {
        mPreference.set(enabled);
    }


    /**
     * Executes a command only if vibration is enabled
     * @param c command to execute
     */
    private void executeIfEnabled(Consumer<Vibrator> c)
    {
        if (isEnabled())
            c.accept(AndroidUtils.getVibrator(mContext));
    }


    public FuzzyVibrator newFuzzyVibrator(long vibrationlength, long randomDelayRange)
    {
        FuzzyVibrator vibr = new FuzzyVibrator(mHandler);
        vibr.setParams(vibrationlength, randomDelayRange);
        return vibr;
    }


    public FuzzyVibrator newFuzzyVibrator()
    {
        return newFuzzyVibrator(FuzzyVibrator.DEFAULT_VIBRATION_LENGTH_MILLIS, FuzzyVibrator.DEFAULT_RANDOM_DELAY_RANGE_MILLIS);
    }

}
