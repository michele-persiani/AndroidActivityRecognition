package umu.software.activityrecognition.config;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;

import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import umu.software.activityrecognition.R;
import umu.software.activityrecognition.shared.AndroidUtils;
import umu.software.activityrecognition.shared.preferences.PreferencesBuilder;
import umu.software.activityrecognition.shared.preferences.PreferencesModule;

public class RecordingsPreferences extends PreferencesModule
{

    public RecordingsPreferences(Context context)
    {
        super(context);
    }

    @Override
    protected void initialize(Context context, PreferencesBuilder builder)
    {
        builder.initBoolean(getStringFromId(R.string.recordings_use_wake_lock), true);
        builder.initString(getStringFromId(R.string.save_interval_minutes), "10");
        builder.initString(getStringFromId(R.string.read_sensor_delay_millis), "50");
        builder.initString(getStringFromId(R.string.read_models_delay_millis), "2000");

        SensorManager sensorManager = AndroidUtils.getSensorManager(context);
        for (Sensor s : sensorManager.getSensorList(Sensor.TYPE_ALL))
            builder.initBoolean(getSensorKey(s), false);
        builder.putBoolean(
                getSensorKey(sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)),
                true
        );
        builder.putBoolean(
                getSensorKey(sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)),
                true
        );
        builder.putBoolean(
                getSensorKey(sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)),
                true
        );
    }

    public Predicate<Sensor> getRecordSensorPredicate()
    {
        return (sensor) -> getBoolean(getSensorKey(sensor), false);
    }


    public long saveIntervalMillis()
    {
        return TimeUnit.MILLISECONDS.convert(
                getInt(getStringFromId(R.string.save_interval_minutes), 10),
                TimeUnit.MINUTES
        );
    }


    public boolean useWakeLock()
    {
        return getBoolean(getStringFromId(R.string.recordings_use_wake_lock), true);
    }


    public long sensorsReadingsDelayMillis()
    {
        return getLong(getStringFromId(R.string.read_sensor_delay_millis), 50);
    }


    public long modelsReadingsDelayMillis()
    {
        return getLong(getStringFromId(R.string.read_models_delay_millis), 50);
    }


    public static String getSensorKey(Sensor sensor)
    {
        return String.format("RecordSensor_%s", sensor.getName().replace(" ", "_"));
    }

}
