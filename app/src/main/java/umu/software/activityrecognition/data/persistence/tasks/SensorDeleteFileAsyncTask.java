package umu.software.activityrecognition.data.persistence.tasks;

import java.io.File;

import umu.software.activityrecognition.shared.asynctasks.DeleteFileAsyncTask;
import umu.software.activityrecognition.data.accumulators.SensorAccumulator;

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
