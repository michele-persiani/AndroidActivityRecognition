package umu.software.activityrecognition.data.sensors;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;

import com.google.common.collect.Lists;

import java.util.List;

import umu.software.activityrecognition.shared.AndroidUtils;

public enum Sensors
{
    INSTANCE;

    SensorManager mSensorManager;
    private Handler mHandler;
    List<SensorEventListener> mListeners = Lists.newArrayList();


    public void initialize(Context context)
    {
        mSensorManager = AndroidUtils.getSensorManager(context);
        mHandler = AndroidUtils.newHandler();
    }

    public void destroy()
    {
        for (SensorEventListener l : Lists.newArrayList(mListeners))
            unregisterListener(l);
        mSensorManager = null;
        mHandler.getLooper().quit();
        mHandler = null;
    }


    public List<Sensor> getSensors()
    {
        return getSensors(Sensor.TYPE_ALL);
    }


    public List<Sensor> getSensors(int type)
    {
        return mSensorManager.getSensorList(type);
    }


    public Sensor getSensor(String name)
    {
        for(Sensor s : getSensors())
            if (s.getName().equals(name))
                return s;
        return null;
    }


    public Sensor getDefaultSensor(int sensorType)
    {
        return mSensorManager.getDefaultSensor(sensorType);
    }


    public void registerListenerDefault(int sensorType, SensorEventListener listener)
    {
        Sensor sensor = getDefaultSensor(sensorType);
        registerListener(sensor, listener);
    }

    public void registerListener(Sensor sensor, SensorEventListener listener)
    {
        if (mListeners.contains(listener))
            return;
        mSensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_NORMAL, mHandler);
        mListeners.add(listener);
    }


    public void unregisterListener(SensorEventListener listener)
    {
        if (!mListeners.contains(listener))
            return;
        mSensorManager.unregisterListener(listener);
        mListeners.remove(listener);
    }
}
