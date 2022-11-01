package umu.software.activityrecognition.services.recordings;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.NetworkType;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URI;
import java.util.concurrent.atomic.AtomicInteger;

import umu.software.activityrecognition.shared.persistance.Authentication;
import umu.software.activityrecognition.shared.persistance.Directories;
import umu.software.activityrecognition.shared.persistance.IDirectory;
import umu.software.activityrecognition.shared.util.Exceptions;
import umu.software.activityrecognition.shared.util.LogHelper;


/**
 * Worker to transfer files between URIs.
 * Requires two URIs in its InputData URI_FROM and URI_TO.
 * Optionally gets two corresponding authentications:
 */
public class TransferWorker extends Worker
{
    protected static final int NUM_TRIES = 5;

    protected static final String URI_FROM = "URI_FROM";
    protected static final String URI_TO = "URI_TO";

    protected static final String AUTH_FROM = "AUTH_FROM";
    protected static final String AUTH_TO = "AUTH_TO";

    public static final String IS_PROGRESS_DATA = "IS_PROGRESS_DATA";
    public static final String PROGRESS = "PROGRESS";
    public static final String MAX_PROGRESS = "MAX_PROGRESS";
    public static final String TRANSFERRED_FILENAME = "TRANSFERRED_FILENAME";

    private boolean mRunning;

    private final LogHelper mLog = LogHelper.newClassTag(this);


    public TransferWorker(Context context, WorkerParameters params)
    {
        super(context, params);
        mRunning = true;
    }


    @Override
    public void onStopped()
    {
        super.onStopped();
        mRunning = false;
        mLog.i("STOPPED");
    }

    @NonNull
    @Override
    public Result doWork()
    {
        mLog.i("TRANSFER STARTED WITH INPUT DATA: %s.", getInputData());

        URI from = URI.create(getInputData().getString(URI_FROM));
        URI to   = URI.create(getInputData().getString(URI_TO));

        Authentication authFrom = getAuthFromData(AUTH_FROM);
        Authentication authTo = getAuthFromData(AUTH_TO);

        publishProgress(0, 0, from.getPath());

        boolean success = Exceptions.tryRetry(NUM_TRIES, () ->
        {
            IDirectory dirFrom = Directories.getDirectoryForURI(from);
            IDirectory dirTo = Directories.getDirectoryForURI(to);
            dirFrom.setURI(from);
            dirTo.setURI(to);

            dirFrom.setAuthentication(authFrom);
            dirTo.setAuthentication(authTo);



            int numFiles = Directories.countFiles(dirFrom, true);

            publishProgress(0, numFiles, dirFrom.getURI().getPath());

            AtomicInteger progress = new AtomicInteger();


            Directories.copyFromTo(dirFrom, dirTo, Directories.ErrorPolicy.THROW, filename -> {
                mLog.i(" RUNNING: %s TRANSFERRED: %s", mRunning, filename);
                publishProgress(progress.addAndGet(1), numFiles, filename);
                return mRunning;
            });


        }, () -> {
            mLog.i(" TRANSFER FAILED. RETRYING...");
        });


        mLog.i("END OF TRANSFER %s -> %s. SUCCESS: %s", from, to, success);
        synchronized (this)
        {
            return Result.success();
        }
    }


    private void publishProgress(int progress, int maxProgress, String filename)
    {
        synchronized (this)
        {
            setProgressAsync(
                    new Data.Builder()
                            .putBoolean(IS_PROGRESS_DATA, true)
                            .putString(TRANSFERRED_FILENAME, filename)
                            .putInt(PROGRESS, progress)
                            .putInt(MAX_PROGRESS, maxProgress)
                            .build()
            );
        }
    }


    private Authentication getAuthFromData(String key)
    {
        byte[] serialized = getInputData().getByteArray(key);
        if (serialized == null)
            return null;

        return Exceptions.runCatch(() -> {
            ByteArrayInputStream bis = new ByteArrayInputStream(serialized);
            ObjectInputStream ois = new ObjectInputStream(bis);
            Authentication o = (Authentication)ois.readObject();
            ois.close();
            bis.close();
            return o;
        }, null);

    }


    public static Data createInputData(URI from, URI to, @Nullable Authentication authFrom, @Nullable Authentication authTo)
    {
        Data.Builder data = new Data.Builder();

        data.putString(URI_FROM, from.toString()).putString(URI_TO, to.toString());

        if (authFrom != null)
            Exceptions.runCatch(() -> {
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                ObjectOutputStream out = new ObjectOutputStream(bos);
                out.writeObject(authFrom);
                out.flush();
                data.putByteArray(AUTH_FROM, bos.toByteArray());
                out.close();
                bos.close();
            });

        if (authTo != null)
            Exceptions.runCatch(() -> {
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                ObjectOutputStream out = new ObjectOutputStream(bos);
                out.writeObject(authTo);
                out.flush();
                data.putByteArray(AUTH_TO, bos.toByteArray());
                out.close();
                bos.close();
            });
        return data.build();
    }


    public static Constraints createConstraints(URI from, URI to, @Nullable Authentication authFrom, @Nullable Authentication authTo)
    {
        Constraints.Builder constraints = new Constraints.Builder();

        if (from.getScheme().equals("sftp") || to.getScheme().equals("sftp"))
            constraints.setRequiredNetworkType(NetworkType.CONNECTED);

        return constraints.build();
    }

}
