package umu.software.activityrecognition.data.persistence.tasks;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import umu.software.activityrecognition.shared.asynctasks.FileWriterAsyncTask;
import umu.software.activityrecognition.data.dataframe.DataFrame;

public class DataFrameFileWriterAsyncTask extends FileWriterAsyncTask<DataFrame>
{

    private final String folderName;

    public DataFrameFileWriterAsyncTask(String folderName)
    {
        this.folderName = folderName;
    }

    @Override
    public FileOutputStream getOutputStream(DataFrame sensor) throws IOException
    {
        File file = getOutputFile(sensor);
        return new FileOutputStream(file, true);
    }

    @Override
    public String getFileContent(DataFrame df) throws IOException
    {
        File file = getOutputFile(df);
        boolean exists = file.exists();
        return df.toCSV(exists);
    }


    private File getOutputFile(DataFrame df) throws IOException
    {
        File folder = new File(folderName);

        if (!folder.exists() && !folder.mkdirs()) {
            throw new IOException("Couldn't create folder: " + folderName);
        }

        String filename = getFileName(df);
        return new File(folder, filename);
    }


    protected String getFileName(DataFrame df)
    {
        return df.getName() + ".csv";
    }
}
