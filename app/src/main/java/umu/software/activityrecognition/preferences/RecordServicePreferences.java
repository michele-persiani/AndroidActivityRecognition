package umu.software.activityrecognition.preferences;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Environment;

import java.nio.file.Paths;
import java.util.function.Predicate;

import umu.software.activityrecognition.R;
import umu.software.activityrecognition.shared.preferences.PreferencesInitializer;
import umu.software.activityrecognition.shared.util.AndroidUtils;
import umu.software.activityrecognition.shared.preferences.Preference;
import umu.software.activityrecognition.shared.preferences.PreferencesModule;
import umu.software.activityrecognition.tflite.TFLiteNamedModels;

public class RecordServicePreferences extends PreferencesModule
{
    static {
        PreferencesInitializer.addInitialization(RecordServicePreferences.class);
    }

    public RecordServicePreferences(Context context)
    {
        super(context);
    }


    protected void initialize()
    {
        useWakeLock().init(
                getResources().getBoolean(R.bool.recordings_default_use_wake_lock)
        );
        saveIntervalMinutes().init(
                getResources().getInteger(R.integer.recordings_default_save_interval_minutes)
        );
        sensorsReadingsDelayMillis().init(
                getResources().getInteger(R.integer.recordings_default_sensors_delay_millis)
        );
        modelsReadingsDelayMillis().init(
                getResources().getInteger(R.integer.recordings_default_models_delay_millis)
        );

        saveFolderPath().init(
                Paths.get(
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).getAbsolutePath(),
                        getResources().getString(R.string.application_documents_folder),
                        getResources().getString(R.string.application_recordings_folder)
                ).toString()
        );

        SensorManager sensorManager = AndroidUtils.getSensorManager(mContext);
        for (Sensor s : sensorManager.getSensorList(Sensor.TYPE_ALL))
            recordSensor(s).init(false);

        recordSensor(sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)).set(true);
        recordSensor(sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)).set(true);
        recordSensor(sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)).set(true);


        for (TFLiteNamedModels model : TFLiteNamedModels.values())
            recordModel(model.getModelName()).init(
                    getResources().getBoolean(R.bool.recordings_default_record_models)
            );
    }

    public Predicate<Sensor> recordSensorPredicate()
    {
        return (sensor) -> recordSensor(sensor).get();
    }

    public Predicate<TFLiteNamedModels> recordModelPredicate()
    {
        return (model) -> recordModel(model.getModelName()).get();
    }

    public Preference<Integer> saveIntervalMinutes()
    {
        return getInt(R.string.save_interval_minutes);
    }

    public Preference<String> saveFolderPath()
    {
        return getString(R.string.recordings_save_folder);
    }

    public Preference<Boolean> useWakeLock()
    {
        return getBoolean(getStringRes(R.string.recordings_use_wake_lock));
    }


    public Preference<Integer> sensorsReadingsDelayMillis()
    {
        return getInt(getStringRes(R.string.read_sensor_delay_millis));
    }


    public Preference<Integer> modelsReadingsDelayMillis()
    {
        return getInt(getStringRes(R.string.read_models_delay_millis));
    }

    public Preference<Boolean> recordSensor(Sensor sensor)
    {
        return getBoolean(getSensorKey(sensor));
    }


    public Preference<Boolean> recordModel(String modelName)
    {
        return getBoolean(getModelKey(modelName));
    }


    public static String getSensorKey(Sensor sensor)
    {
        return String.format("RecordSensor_%s", sensor.getName().replace(" ", "_"));
    }


    public static String getModelKey(String modelName)
    {
        return String.format("RecordModel_%s", modelName.replace(" ", "_"));
    }

}
