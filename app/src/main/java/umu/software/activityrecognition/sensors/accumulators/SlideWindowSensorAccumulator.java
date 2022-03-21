package umu.software.activityrecognition.sensors.accumulators;

import android.hardware.SensorEvent;


public class SlideWindowSensorAccumulator extends SensorAccumulator
{
    private final int windowSize;

    public SlideWindowSensorAccumulator(int windowSize)
    {
        super();
        assert windowSize > 0;
        this.windowSize = windowSize;
    }

    public int getWindowSize()
    {
        return windowSize;
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent)
    {
        super.onSensorChanged(sensorEvent);
        if (dataframe.countRows() > windowSize)
            dataframe.popFirstRow();
    }
}
