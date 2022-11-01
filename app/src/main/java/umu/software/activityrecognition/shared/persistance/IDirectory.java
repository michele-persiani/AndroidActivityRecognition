package umu.software.activityrecognition.shared.persistance;

import androidx.annotation.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import umu.software.activityrecognition.shared.util.Exceptions;

public interface IDirectory
{
    interface CheckedFunction<T, R>
    {
        R apply(T dir) throws Exception;
    }

    /**
     * Sets the authentication credentials to access the directory
     * @param auth authentication credentials
     */
    void setAuthentication(Authentication auth);

    /**
     * Sets the directory's URI
     * @param dirUri the new uri of the directory
     * @throws IOException
     */
    void setURI(URI dirUri) throws Exception;

    /**
     * Gets the directory's URI
     * @return the directory's URI
     * @throws Exception
     */
    URI getURI() throws Exception;

    /**
     *Gets the uri of a directory's file
     * @param filename filename relative to the directory's path
     * @return path object
     * @throws IOException
     */
    URI getURI(String filename) throws Exception;


    /**
     * Gets a subdirectory
     * @param dirName name of the subdirectory
     * @return a new instance of FileDirectory
     * @throws IOException if this directory is not set or if dirName points to a bad directory
     */
    IDirectory getSubDirectory(String dirName) throws Exception;


    /**
     * Gets the parent directory
     * @return a new instance of FileDirectory for the parent directory
     * @throws IOException if this directory is not set or if the parent directory is a bad directory
     */
    IDirectory getParentDirectory() throws Exception;


    /**
     * Gets the list of files contained in this directory
     * @return  the list of files contained in this directory
     * @throws IOException
     */
    default List<String> listFileNames() throws Exception
    {
        return  list(file -> !Exceptions.runCatch(() -> isFolder(file), false));
    }


    /**
     * Gets the full path name of a file
     * @param filename name of the file
     * @return full path name
     * @throws Exception
     */
    default String getFullPath(String filename) throws Exception
    {
        return Paths.get(getURI().getPath(), filename).toString();
    }


    /**
     * Gets the list of files contained in this directory
     * @param filter filter
     * @return  the list of files contained in this directory
     * @throws IOException
     */
    default List<String> listFileNames(Predicate<String> filter) throws Exception
    {
        return  list(file -> !Exceptions.runCatch(() -> isFolder(file), false))
                .stream()
                .filter(filter)
                .collect(Collectors.toList());
    }


    /**
     * Gets the list of folders in this directory
     * @return  the list of folders contained in this directory
     */
    default List<String> listFolderNames() throws Exception
    {
        return  list(file -> Exceptions.runCatch(() -> isFolder(file), false));
    }


    /**
     * Gets the list of folders in this directory
     * @param filter filter
     * @return  the list of folders contained in this directory
     */
    default List<String> listFolderNames(Predicate<String> filter) throws Exception
    {
        return  list(file -> Exceptions.runCatch(() -> isFolder(file), false))
                .stream()
                .filter(filter)
                .collect(Collectors.toList());
    }

    /**
     * List all files in the directory, whether they're files or folders
     * @param filter filter function
     * @return list of file names
     * @throws Exception
     */
    List<String> list(@Nullable Predicate<String> filter) throws Exception;


    /**
     * Checks whether the given file is a directory
     * @param filename name of the file to check
     * @return whether the file is a directory
     * @throws Exception
     */
    boolean isFolder(String filename) throws Exception;


    /**
     * Writes data to a file. This operation creates a file if necessary and overrides previous files
     * @param filename name of the file to write
     * @param writer consumer that will do the writing
     * @throws IOException
     */
    void writeToFile(String filename, CheckedFunction<OutputStream, Void> writer) throws Exception;


    /**
     * Reads from a file.
     * @param <C> type of the returned object
     * @param filename name of the file to read
     * @param reader consumer that will do thereading
     * @return a result object
     * @throws IOException
     */
    <C> C readFromFile(String filename, CheckedFunction<InputStream, C> reader) throws Exception;


    /**
     * Creates a new subdirectory
     * @param dirname name of the folder
     * @return is a new folder was created
     */
    boolean createFolder(String dirname) throws Exception;



    /**
     * Deletes the files with the given names
     * @param filter files to remove
     * @return number of deleted files
     * @throws IOException
     */
    int delete(Predicate<String> filter) throws Exception;


}
