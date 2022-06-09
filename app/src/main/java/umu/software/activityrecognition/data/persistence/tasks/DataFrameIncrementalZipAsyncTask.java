package umu.software.activityrecognition.data.persistence.tasks;

import android.annotation.SuppressLint;

import java.io.File;
import java.util.Arrays;

import umu.software.activityrecognition.shared.asynctasks.ZipAsyncTask;
import umu.software.activityrecognition.data.dataframe.DataFrame;

public class DataFrameIncrementalZipAsyncTask extends ZipAsyncTask<DataFrame>
{
    private final String folderName;
    private final String filePrefix;

    public DataFrameIncrementalZipAsyncTask(String folderName, String filePrefix, int bufferSize)
    {
        super(bufferSize);
        this.folderName = folderName;
        this.filePrefix = filePrefix;
    }

    public DataFrameIncrementalZipAsyncTask(String folderName, int bufferSize)
    {
        super(bufferSize);
        this.folderName = folderName;
        this.filePrefix = null;
    }

    @Override
    protected String getInputFilePath(DataFrame df)
    {
        String fileName = df.getName().replace(" ", "_") + ".csv";
        return String.format("%s%s%s", folderName, File.separator, fileName);
    }

    @Override
    protected boolean filterInput(DataFrame df)
    {
        String fileName = getInputFilePath(df);
        File file = new File(fileName);
        return file.exists();
    }

    @Override
    protected boolean checkConditions()
    {
        File   file  = new File(folderName);
        return file.exists() && file.listFiles() != null;
    }

    private int findIncrementalValue()
    {
        File file = new File(folderName);
        File[] files = file.listFiles();

        assert files != null;

        Object[] filtered = Arrays.stream(files)
                .filter((f) -> f.getName().endsWith(".zip") && (filePrefix == null || f.getName().startsWith(filePrefix)))
                .toArray();

        files = Arrays.copyOf(filtered, filtered.length, File[].class);
        if (files.length == 0)
            return 0;

        int inc = 0;
        for (File f : files)
        {
            String fileName = f.getName();
            int substringStart = (filePrefix == null)? 0 : fileName.indexOf(".") + 1;
            int substringEnd = fileName.lastIndexOf(".");
            fileName = fileName.substring(substringStart, substringEnd);
            int finc = -1;
            try
            {
                finc = Integer.parseInt( fileName );
            } catch (NumberFormatException ignored){}
            inc = Math.max(inc, finc);

        }
        inc += 1;

        return inc;
    }

    @SuppressLint("DefaultLocale")
    @Override
    protected String getOutputFileName()
    {
        int incrementalValue = findIncrementalValue();

        if (filePrefix != null)
            return String.format("%s%s%s.%d.zip", folderName, File.separator, filePrefix, incrementalValue);
        else
            return String.format("%s%s%d.zip", folderName, File.separator, incrementalValue);

    }
}
