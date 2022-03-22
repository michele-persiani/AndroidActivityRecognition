package umu.software.activityrecognition.persistence;

import android.os.AsyncTask;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

public abstract class FileWriterAsyncTask<T> extends AsyncTask<T, Boolean, Integer>
{

    protected abstract FileOutputStream getOutputStream(T input) throws IOException;


    public abstract String getFileContent(T input) throws IOException;


    private Boolean processInput(T input)
    {
        FileOutputStream outputStream;
        try {
            outputStream = getOutputStream(input);
            String content = getFileContent(input);
            OutputStreamWriter bw = new OutputStreamWriter(outputStream);
            bw.write(content);

            bw.close();
            return true;

        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }


    @Override
    protected Integer doInBackground(T... inputs)
    {
        int successes = 0;
        for (T x : inputs)
        {
            boolean result = processInput(x);
            publishProgress(result);
            successes += result ? 1 : 0;
        }
        return successes;
    }
}
