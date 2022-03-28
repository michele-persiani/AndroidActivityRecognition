package umu.software.activityrecognition.sensors.persistence.tasks;

import android.annotation.SuppressLint;

import java.io.File;
import java.util.Arrays;

import umu.software.activityrecognition.common.persistence.ZipAsyncTask;
import umu.software.activityrecognition.sensors.accumulators.SensorAccumulator;

public class SensorIncrementalZipAsyncTask extends ZipAsyncTask<SensorAccumulator>
{
    private final String folderName;


    public SensorIncrementalZipAsyncTask(String folderName, int bufferSize)
    {
        super(bufferSize);
        this.folderName = folderName;
    }

    @Override
    protected String getInputFilePath(SensorAccumulator sensor)
    {
        String fileName = sensor.getSensor().getName().replace(" ", "_") + ".csv";
        return String.format("%s%s%s", folderName, File.separator, fileName);
    }

    @Override
    protected boolean filterInput(SensorAccumulator sensor)
    {
        String fileName = getInputFilePath(sensor);
        File file = new File(fileName);
        return file.exists();
    }

    @Override
    protected boolean checkConditions()
    {
        File   file  = new File(folderName);
        return file.exists() && file.listFiles() != null;
    }

    @SuppressLint("DefaultLocale")
    @Override
    protected String getOutputFileName()
    {
        File file = new File(folderName);
        File[] files = file.listFiles();

        assert files != null;

        Object[] filtered = Arrays.stream(files)
                .filter((f) -> f.getName().endsWith(".zip"))
                .toArray();

        files = Arrays.copyOf(filtered, filtered.length, File[].class);
        if (files.length == 0)
        {
            return String.format("%s%s%d.zip", folderName, File.separator, 0);
        }

        int inc = 0;
        for (File f : files)
        {
            String fileName = f.getName();
            fileName = fileName.substring(0, fileName.lastIndexOf("."));
            int finc = -1;
            try
            {
                finc = Integer.parseInt( fileName );
            } catch (NumberFormatException ignored){}
            inc = Math.max(inc, finc);

        }
        inc += 1;

        return String.format("%s%s%d.zip", folderName, File.separator, inc);
    }
}
