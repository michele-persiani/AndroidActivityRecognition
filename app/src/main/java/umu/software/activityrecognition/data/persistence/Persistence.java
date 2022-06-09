package umu.software.activityrecognition.data.persistence;

import android.app.Activity;
import android.os.Environment;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.Callable;

import umu.software.activityrecognition.shared.permissions.Permissions;
import umu.software.activityrecognition.data.dataframe.DataFrame;
import umu.software.activityrecognition.data.persistence.tasks.DataFrameDeleteFileAsyncTask;
import umu.software.activityrecognition.data.persistence.tasks.DataFrameFileWriterAsyncTask;
import umu.software.activityrecognition.data.persistence.tasks.DataFrameIncrementalZipAsyncTask;
import umu.software.activityrecognition.data.persistence.tasks.DeleteFolderAsyncTask;


public enum Persistence
{
    SENSORS_FOLDER(Paths.get(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).getAbsolutePath(),
            "Sensor Readings"
    ).toString()
    ),

    ACTIVITY_FOLDER(Paths.get(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).getAbsolutePath(),
            "Activity Recordings"
    ).toString()
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


    public Callable<Integer> saveToFile(Collection<DataFrame> dataframes)
    {
        DataFrameFileWriterAsyncTask writer = new DataFrameFileWriterAsyncTask(sensorFolder);
        writer.execute(filterInitialized(dataframes));
        return writer::get;
    }


    public Callable<Integer> createIncrementalZip(Collection<DataFrame> dataframes)
    {
        DataFrameIncrementalZipAsyncTask writer = new DataFrameIncrementalZipAsyncTask(sensorFolder, 16384);
        writer.execute(filterInitialized(dataframes));

        return writer::get;
    }



    public Callable<Integer> createIncrementalZip(String zipPrefix, Collection<DataFrame> dataframes)
    {
        DataFrameIncrementalZipAsyncTask writer = new DataFrameIncrementalZipAsyncTask(sensorFolder, zipPrefix, 16384);
        writer.execute(filterInitialized(dataframes));

        return writer::get;
    }

    public Callable<Integer> deleteFiles(Collection<DataFrame> dataframes)
    {
        DataFrameDeleteFileAsyncTask task = new DataFrameDeleteFileAsyncTask(sensorFolder);
        task.execute(filterInitialized(dataframes));
        return task::get;
    }


    public Callable<Integer> deleteSaveFolder()
    {
        DeleteFolderAsyncTask task = new DeleteFolderAsyncTask();
        task.execute(sensorFolder);
        return task::get;

    }

    private DataFrame[] filterInitialized(Collection<DataFrame> dataframes)
    {
        Object[] dfArray = dataframes
                .stream()
                .filter((df) -> df.countRows() > 0)
                .toArray();

        return Arrays.copyOf(
                dfArray,
                dfArray.length,
                DataFrame[].class
        );
    }

}
