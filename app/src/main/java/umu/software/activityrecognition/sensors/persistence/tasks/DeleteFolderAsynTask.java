package umu.software.activityrecognition.sensors.persistence.tasks;

import android.os.AsyncTask;

import java.io.File;

public class DeleteFolderAsynTask extends AsyncTask<String, String, Integer>
{

    @Override
    protected Integer doInBackground(String... paths)
    {
        int deleted = 0;
        for (String path : paths) {
            File file = new File(path);
            deleted += helperDeleteDir(file);
        }
        return deleted;
    }


    private int helperDeleteDir(File file) {
        File[] contents = file.listFiles();
        int deleted = 0;
        if (contents != null) {
            for (File f : contents) {
                deleted += helperDeleteDir(f);
            }
        }
        deleted += file.isFile() ? 1 : 0;
        file.delete();
        publishProgress(file.getAbsolutePath());
        return deleted;
    }

}
