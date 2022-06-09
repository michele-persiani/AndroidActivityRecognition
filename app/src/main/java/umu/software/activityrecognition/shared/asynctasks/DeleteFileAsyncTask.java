package umu.software.activityrecognition.shared.asynctasks;

import android.os.AsyncTask;

import java.io.File;

public abstract class DeleteFileAsyncTask<T> extends AsyncTask<T, Boolean, Integer>
{

    protected abstract String getFilePath(T input);

    @Override
    protected Integer doInBackground(T... inputs)
    {
        int deleted = 0;
        for (T input : inputs)
        {
            File file = new File(getFilePath(input));

            boolean success = file.exists() && file.delete();
            deleted += success? 1 : 0;
        }

        return deleted;
    }
}
