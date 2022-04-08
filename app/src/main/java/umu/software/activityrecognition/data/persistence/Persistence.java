package umu.software.activityrecognition.data.persistence;

import android.app.Activity;
import android.os.Environment;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.Callable;

import umu.software.activityrecognition.common.permissions.Permissions;
import umu.software.activityrecognition.data.persistence.tasks.DeleteFolderAsynTask;
import umu.software.activityrecognition.data.persistence.tasks.SensorDeleteFileAsyncTask;
import umu.software.activityrecognition.data.persistence.tasks.SensorFileWriterAsyncTask;
import umu.software.activityrecognition.data.persistence.tasks.SensorIncrementalZipAsyncTask;
import umu.software.activityrecognition.data.accumulators.SensorAccumulator;

public enum Persistence
{
    DOCUMENTS_FOLDER(
            String.format(
                    "%s%s%s",
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).getAbsolutePath(),
                    File.separator,
                    "Sensor Readings"
            )
    );


    private final String sensorFolder;


    Persistence(String sensorFolder)
    {
        this.sensorFolder = sensorFolder;
    }


    public void askPermissions(Activity activity)
    {
        Permissions.READ_EXTERNAL_STORAGE.askPermission(activity);
        Permissions.WRITE_EXTERNAL_STORAGE.askPermission(activity);
    }


    public Callable<Integer> saveToFile(Collection<SensorAccumulator> sensors, boolean resetSensors)
    {
        SensorFileWriterAsyncTask writer = new SensorFileWriterAsyncTask(sensorFolder, resetSensors);
        writer.execute(filterInitialized(sensors));

        return writer::get;
    }


    public Callable<Integer> createIncrementalZip(Collection<SensorAccumulator> sensors)
    {
        SensorIncrementalZipAsyncTask writer = new SensorIncrementalZipAsyncTask(sensorFolder, 16384);
        writer.execute(filterInitialized(sensors));

        return writer::get;
    }


    public Callable<Integer> deleteFiles(Collection<SensorAccumulator> sensors)
    {
        SensorDeleteFileAsyncTask task = new SensorDeleteFileAsyncTask(sensorFolder);
        task.execute(filterInitialized(sensors));
        return task::get;

    }


    public Callable<Integer> deleteSaveFolder()
    {
        DeleteFolderAsynTask task = new DeleteFolderAsynTask();
        task.execute(sensorFolder);
        return task::get;

    }

    private SensorAccumulator[] filterInitialized(Collection<SensorAccumulator> sensors)
    {
        Object[] accumsArray = sensors
                .stream()
                .filter((s) -> s.getDataFrame().countRows() > 0)
                .toArray();

        return Arrays.copyOf(
                accumsArray,
                accumsArray.length,
                SensorAccumulator[].class
        );
    }

}
