package umu.software.activityrecognition.persistence;

import android.os.AsyncTask;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public abstract class ZipAsyncTask<T> extends AsyncTask<T, Boolean, Integer>
{

    private final int bufferSize;

    public ZipAsyncTask(int bufferSize)
    {
        this.bufferSize = bufferSize;
    }


    protected abstract String getInputFilePath(T input);

    protected boolean filterInput(T input)
    {
        return true;
    }

    protected abstract String getOutputFileName();

    protected boolean checkConditions()
    {
        return true;
    }

    public boolean processFile(ZipOutputStream out, String fileName)
    {
        FileInputStream fi;
        BufferedInputStream origin;
        byte[] data = new byte[bufferSize];

        try
        {
            fi = new FileInputStream(fileName);
            origin = new BufferedInputStream(fi, bufferSize);

            String entryName = fileName.substring(fileName.lastIndexOf(File.separator) + 1);
            ZipEntry entry = new ZipEntry(entryName);
            out.putNextEntry(entry);
            int count;
            while ((count = origin.read(data, 0, bufferSize)) != -1)
            {
                out.write(data, 0, count);
            }

            origin.close();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    @Override
    protected Integer doInBackground(T... inputs)
    {
        if (!checkConditions())
        {
            return 0;
        }

        ZipOutputStream out;
        try {
            out = new ZipOutputStream(new FileOutputStream(getOutputFileName()));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return -1;
        }

        int result = 0;
        for (T input : inputs)
        {
            String file = getInputFilePath(input);
            boolean success = filterInput(input) && processFile(out, file);
            result += success? 1 : 0;
        }

        try {
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }
}
