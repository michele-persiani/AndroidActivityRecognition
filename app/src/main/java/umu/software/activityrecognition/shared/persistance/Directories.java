package umu.software.activityrecognition.shared.persistance;

import androidx.annotation.Nullable;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import umu.software.activityrecognition.shared.persistance.impl.FileDirectory;
import umu.software.activityrecognition.shared.persistance.impl.SSHJDirectory;
import umu.software.activityrecognition.shared.util.Exceptions;


/**
 * Factory and utils for IDirectory objects
 */
public class Directories
{

    /**
     * Error resolution policy when processing multiple files
     */
    public enum ErrorPolicy
    {
        /**
         * Ignores current error and moves on to the next file
         */
        CONTINUE,

        /**
         * Cancels the remaining operations
         */
        CANCEL,

        /**
         * Throws an exception
         */
        THROW;
    }


    private Directories() {}


    /**
     * Creates a new writer that writes objects into files. Files are saved at the directory position
     * @param writer writer function to write objects into files
     * @param <T> type of the objects accepted by the writer
     * @return writer function. Returns false in case there is an error
     */
    public static <T> BiFunction<String, T, Boolean> newWriter(IDirectory dir, BiConsumer<OutputStream, T> writer)
    {
        return (filename, obj) -> Exceptions.runCatch(() -> dir.writeToFile(filename, fout -> {
            writer.accept(fout, obj);
            return null;
        }));
    }


    /**
     * Gets an object reader to read objects from their corresponding file names
     * @param reader reader transforming file content to objects
     * @param <T> type of returned objects
     * @return Reader function. Returns null in case there is an error
     */
    public static <T> Function<String, T> newReader(IDirectory dir, IDirectory.CheckedFunction<InputStream, T> reader)
    {
        return filename -> Exceptions.runCatch(() -> dir.readFromFile(filename, reader), (T) null);
    }




    /**
     * Executes a command on a newly created local file directory
     * @param dirpath local path of the directory
     * @param command command to execute
     * @return
     */
    public static boolean peformOnDirectory(String dirpath, @Nullable Authentication auth, IDirectory.CheckedFunction<IDirectory, Void> command)
    {
        return peformOnDirectory(new File(dirpath).toURI(), auth, command);
    }


    /**
     * Executes a command on a newly created directory. The type of directory is determined by the URI
     * @param diruri uri of the directory
     * @param auth optional authentication credentials
     * @param command command to execute
     * @return
     */
    public static boolean peformOnDirectory(URI diruri, @Nullable Authentication auth, IDirectory.CheckedFunction<IDirectory, Void> command)
    {
        IDirectory dir = Exceptions.runCatch( () -> Directories.getDirectoryForURI(diruri), (IDirectory)null);
        if (dir == null)
            return false;
        auth = (auth == null) ? new Authentication() : auth;
        dir.setAuthentication(auth);
        if (!Exceptions.runCatch(() -> dir.setURI(diruri)))
            return false;
        return Exceptions.runCatch(() -> command.apply(dir));
    }


    /**
     * Copy a directory to another
     * @param from source directory
     * @param to target directory
     * @param policy policy to apply when there is an error while transferring the file
     * @param callback callback that gets notified when a file is successfully processed.
     *                 If it returns false the copying process is interrupted
     * @return whether the operation was successful
     */
    public static boolean copyFromTo(IDirectory from, IDirectory to, ErrorPolicy policy, Function<String, Boolean> callback) throws Exception
    {
        boolean tmp;

        for(String filename : from.list(null))
        {
            if(!callback.apply(from.getFullPath(filename)))
                break;
            try
            {
                if (from.isFolder(filename))
                {
                    tmp = copyFromTo(from.getSubDirectory(filename), to.getSubDirectory(filename), policy, callback);
                    if (!tmp && policy.equals(ErrorPolicy.CANCEL))
                        return false;
                }
                else
                {
                    to.writeToFile(filename, os ->
                    {
                        from.readFromFile(filename, is ->
                        {
                            byte[] buf = new byte[8192];
                            int length;
                            while ((length = is.read(buf)) != -1)
                                os.write(buf, 0, length);
                            return null;
                        });
                        return null;
                    });
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
                switch (policy)
                {
                    case CONTINUE:
                        continue;
                    case CANCEL:
                        return false;
                    case THROW:
                        throw e;
                }
            }
        }
        return true;
    }

    /**
     * Copy a directory to another
     * @param from source directory
     * @param to target directory
     * @param policy policy to apply when there is an error while transferring the file
     * @param callback callback that gets notified when a file is successfully processed
     * @return whether the operation was successful
     */

    public static boolean copyFromTo(IDirectory from, IDirectory to, ErrorPolicy policy, Consumer<String> callback) throws Exception
    {
        return copyFromTo(from, to, policy, filename -> {
            callback.accept(filename);
            return true;
        });
    }


    /**
     * Copy a directory to another
     * @param from source directory
     * @param to target directory
     * @param policy policy to apply when there is an error while transferring the file
     * @return whether the operation was successful
     */
    public static boolean copyFromTo(IDirectory from, IDirectory to, ErrorPolicy policy) throws Exception
    {
        return copyFromTo(from, to, policy, filename -> true);
    }

    /**
     * Counts the number of files
     * @param dir directory
     * @param processSubdirectories whether to process all subdirestories in a nested fashion
     * @return number of files
     * @throws Exception
     */
    public static int countFiles(IDirectory dir, boolean processSubdirectories) throws Exception
    {
        int count = 0;
        for(String filename : dir.list(null))
        {
            count += (processSubdirectories && dir.isFolder(filename))? 1 + countFiles(dir.getSubDirectory(filename), true) : 1;
        }
        return count;
    }

    /**
     * Creates a zip file
     * @param dir source dir
     * @param zipFilename name of the zip file
     * @param files names of the files to zip
     * @return number of zipped files.
     * @throws Exception
     */
    public static int createZip(IDirectory dir, String zipFilename, Collection<String> files) throws Exception
    {
        int bufferSize = 16384;
        byte[] data = new byte[bufferSize];
        AtomicInteger result = new AtomicInteger();

        if (!zipFilename.endsWith(".zip")) zipFilename += ".zip";

        dir.writeToFile(zipFilename, os -> {
            ZipOutputStream zipOs = new ZipOutputStream(os);
            for (String file : files)
            {
                int zipped = dir.readFromFile(
                        file,
                        is -> {
                            ZipEntry entry = new ZipEntry(file);
                            zipOs.putNextEntry(entry);
                            boolean res = Exceptions.runCatch(() ->
                            {
                                int count;
                                while ((count = is.read(data, 0, bufferSize)) != -1)
                                    zipOs.write(data, 0, count);
                            });
                            return (res) ? 1 : 0;
                        });
                result.addAndGet(zipped);
            }

            zipOs.close();
            return null;
        });

        return result.get();
    }

    /**
     * Finds the correct IDirectory implementation from the given URI eg. from its scheme.
     *
     * At the moment only 'file:' and 'sftp:' directories are supported/ For all other schemes this method returns null
     * @param uri uri of the directory
     * @return a newly created directory for the specific type of URI, or null if the URI is not supported
     */
    public static IDirectory getDirectoryForURI(URI uri) throws Exception
    {
        String scheme = uri.getScheme();
        IDirectory dir = null;
        switch (scheme)
        {
            case "sftp":
                dir = new SSHJDirectory();
                break;
            case "file":
                dir = new FileDirectory();
                break;
        }
        if (dir != null)
        {
            dir.setURI(uri);
        }

        return dir;
    }
}
