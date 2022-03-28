package umu.software.activityrecognition.sensors.persistence.tasks;

import java.io.File;

import umu.software.activityrecognition.common.persistence.DeleteFileAsyncTask;
import umu.software.activityrecognition.sensors.accumulators.SensorAccumulator;

public class SensorDeleteFileAsyncTask extends DeleteFileAsyncTask<SensorAccumulator>
{

    private final String folderName;

    public SensorDeleteFileAsyncTask(String folderName)
    {
        this.folderName = folderName;
    }

    @Override
    protected String getFilePath(SensorAccumulator sensor)
    {
        String filename = sensor.getSensor().getName().replace(" ", "_") + ".csv";
        return String.format("%s%s%s", folderName, File.separator, filename);
    }
}
