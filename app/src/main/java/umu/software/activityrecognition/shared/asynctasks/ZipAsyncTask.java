package umu.software.activityrecognition.shared.asynctasks;

import android.os.AsyncTask;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;


/**
 * AsyncTask to zip files
 * @param <T>
 */
public abstract class ZipAsyncTask<T> extends AsyncTask<T, Boolean, Integer>
{

    private final int bufferSize;

    public ZipAsyncTask(int bufferSize)
    {
        this.bufferSize = bufferSize;
    }


    protected abstract String getInputFilePath(T input);

    /**
     * Returns whether the given input should be processed
     * @param input input to test
     * @return whether the given input should be processed
     */
    protected boolean filterInput(T input)
    {
        return true;
    }

    /**
     * Returns the file name of resulting zip
     * @return the file name of resulting zip
     */
    protected abstract String getOutputFileName();

    /**
     * Returns whether this asynctask should execute
     * @return whether this asynctask should execute
     */
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
        String fileName = getOutputFileName();
        try {
            Files.createDirectories(Paths.get(fileName).getParent().toAbsolutePath());
            out = new ZipOutputStream(new FileOutputStream(fileName));
        } catch (Exception e)
        {
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
