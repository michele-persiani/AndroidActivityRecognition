package umu.software.activityrecognition.sensors.persistence;

import android.app.Activity;
import android.os.Environment;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.Callable;

import umu.software.activityrecognition.permissions.Permissions;
import umu.software.activityrecognition.sensors.persistence.tasks.DeleteFolderAsynTask;
import umu.software.activityrecognition.sensors.persistence.tasks.SensorDeleteFileAsyncTask;
import umu.software.activityrecognition.sensors.persistence.tasks.SensorFileWriterAsyncTask;
import umu.software.activityrecognition.sensors.persistence.tasks.SensorIncrementalZipAsyncTask;
import umu.software.activityrecognition.sensors.accumulators.SensorAccumulator;

public enum Persistence
{
    INSTANCE (
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


    public Callable<Integer> saveToFile(SensorAccumulator... accums)
    {
        return saveToFile(Arrays.asList(accums));
    }


    public Callable<Integer> saveToFile(Collection<SensorAccumulator> sensors)
    {
        SensorFileWriterAsyncTask writer = new SensorFileWriterAsyncTask(sensorFolder);
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
