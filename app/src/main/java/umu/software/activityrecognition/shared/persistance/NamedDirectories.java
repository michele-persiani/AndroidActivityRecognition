package umu.software.activityrecognition.shared.persistance;

import android.app.Activity;
import android.content.Context;
import android.os.Environment;

import java.net.URI;
import java.util.function.Function;

import umu.software.activityrecognition.shared.permissions.Permissions;
import umu.software.activityrecognition.shared.util.Exceptions;


/**
 * Useful named directories of the Android system
 */
public enum NamedDirectories
{
    EXTERNAL_DOCUMENTS(true, getEnvironmentDirectory(Environment.DIRECTORY_DOCUMENTS)),
    EXTERNAL_DOWNLOADS(true, getEnvironmentDirectory(Environment.DIRECTORY_DOWNLOADS)),
    EXTERNAL_PICTURES(true, getEnvironmentDirectory(Environment.DIRECTORY_PICTURES)),
    EXTERNAL_MUSIC(true, getEnvironmentDirectory(Environment.DIRECTORY_MUSIC)),
    EXTERNAL_MOVIES(true, getEnvironmentDirectory(Environment.DIRECTORY_MOVIES)),
    INTERNAL_CACHE(false, ctx -> {
        return getDirectoryFromURI(
                ctx.getCacheDir().toURI()
        );
    }),
    INTERNAL_FILES(false, ctx -> {
        return getDirectoryFromURI(
                ctx.getFilesDir().toURI()
        );
    });


    private final Function<Context, IDirectory> mDirBuilder;
    private final boolean mRequiresPermission;


    NamedDirectories(boolean requiresPermission, Function<Context, IDirectory> fcn)
    {
        mRequiresPermission = requiresPermission;
        mDirBuilder = fcn;
    }


    private static IDirectory getDirectoryFromURI(URI uri)
    {
        return Exceptions.runCatch(() -> {
            IDirectory dir0 = Directories.getDirectoryForURI(uri);
            dir0.setURI(uri);
            return dir0;
        }, null);
    }


    private static Function<Context, IDirectory> getEnvironmentDirectory(String envConstant)
    {
        return ctx -> getDirectoryFromURI(Environment.getExternalStoragePublicDirectory(envConstant).toURI());

    }

    /**
     * Ask permission to the user to read/write to files. Only if required, that is not for internal folders.
     * @param activity
     */
    public void askPermissions(Activity activity)
    {
        if (!mRequiresPermission) return;
        Permissions.READ_EXTERNAL_STORAGE.askPermission(activity);
        Permissions.WRITE_EXTERNAL_STORAGE.askPermission(activity);
    }


    /**
     * Gets an instance of the directory
     * @param context calling context
     * @return a new instance of IdIRECTORY
     */
    public IDirectory getInstance(Context context)
    {
        return mDirBuilder.apply(context);
    }
}
