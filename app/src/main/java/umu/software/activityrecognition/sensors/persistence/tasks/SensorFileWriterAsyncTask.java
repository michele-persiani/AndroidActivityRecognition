package umu.software.activityrecognition.sensors.persistence.tasks;

import com.c_bata.DataFrame;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import umu.software.activityrecognition.persistence.FileWriterAsyncTask;
import umu.software.activityrecognition.sensors.accumulators.SensorAccumulator;

public class SensorFileWriterAsyncTask extends FileWriterAsyncTask<SensorAccumulator>
{

    private final String folderName;

    public SensorFileWriterAsyncTask(String folderName)
    {
        this.folderName = folderName;
    }

    @Override
    FileOutputStream getOutputStream(SensorAccumulator sensor) throws IOException
    {
        File file = getOutputFile(sensor);
        return new FileOutputStream(file, true);
    }

    @Override
    String getFileContent(SensorAccumulator sensor) throws IOException
    {
        File file = getOutputFile(sensor);
        DataFrame df = sensor.getDataFrame();
        sensor.reset();
        boolean exists = file.exists();
        return df.toCSV(exists);
    }


    private File getOutputFile(SensorAccumulator sensor) throws IOException
    {
        File folder = new File(folderName);

        if (!folder.exists() && !folder.mkdirs()) {
            throw new IOException("Couldn't create folder: " + folderName);
        }

        String filename = getFileName(sensor);
        return new File(folder, filename);
    }


    protected String getFileName(SensorAccumulator sensor)
    {
        return sensor.getSensor().getName().replace(" ", "_") + ".csv";
    }
}
