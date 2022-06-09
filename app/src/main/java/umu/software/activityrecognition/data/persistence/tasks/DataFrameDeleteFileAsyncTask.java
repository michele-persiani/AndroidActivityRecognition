package umu.software.activityrecognition.data.persistence.tasks;

import java.io.File;

import umu.software.activityrecognition.shared.asynctasks.DeleteFileAsyncTask;
import umu.software.activityrecognition.data.dataframe.DataFrame;

public class DataFrameDeleteFileAsyncTask extends DeleteFileAsyncTask<DataFrame>
{

    private final String folderName;

    public DataFrameDeleteFileAsyncTask(String folderName)
    {
        this.folderName = folderName;
    }

    @Override
    protected String getFilePath(DataFrame df)
    {
        String filename = df.getName() + ".csv";
        return String.format("%s%s%s", folderName, File.separator, filename);
    }
}
