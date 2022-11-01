package umu.software.activityrecognition.shared.persistance.impl;

import androidx.annotation.Nullable;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import umu.software.activityrecognition.shared.persistance.Authentication;
import umu.software.activityrecognition.shared.persistance.IDirectory;


/**
 * Directory for local files
 */
public class FileDirectory implements IDirectory
{
    private File directory = null;

    private URI uri;


    private void checkDirectory() throws Exception
    {
        if (directory == null)
            throw new IOException("directory == null || !directory.isDirectory()");
        if (directory.exists() && !directory.isDirectory())
            throw new IOException("directory.exists() && !directory.isDirectory()");
        if (!directory.exists() && !directory.mkdirs())
            throw new IOException("!directory.exists() && !directory.mkdirs()");
    }

    @Override
    public void setAuthentication(Authentication auth)
    {
        // Authentication not required
    }


    public void setURI(URI dirUri) throws Exception
    {
        if (!dirUri.getScheme().equals("file")) throw new Exception(" Scheme must be 'file' but instead is " + dirUri.getScheme());
        directory = new File(dirUri);
        uri = dirUri;
    }

    @Override
    public URI getURI() throws Exception
    {
        checkDirectory();
        return uri;
    }

    public URI getURI(String filename) throws Exception
    {
        checkDirectory();
        return new File(directory.getAbsolutePath(), filename).toURI();
    }


    public FileDirectory getSubDirectory(String dirName) throws Exception
    {
        checkDirectory();
        URI uri = getURI(dirName);
        FileDirectory dir = new FileDirectory();
        dir.setURI(uri);
        dir.checkDirectory();
        return dir;
    }


    public FileDirectory getParentDirectory() throws Exception
    {
        checkDirectory();
        FileDirectory dir = new FileDirectory();
        dir.directory = this.directory.getParentFile();
        dir.checkDirectory();
        return dir;
    }


    public List<String> list(@Nullable Predicate<String> filter) throws Exception
    {
        checkDirectory();
        filter =  (filter != null)? filter : file -> true;
        String[] files = directory.list();

        if(files == null) throw new Exception("files == null");

        return  Arrays.stream(files)
                .filter(filename -> !filename.startsWith("."))
                .filter(filter)
                .collect(Collectors.toList());
    }


    @Override
    public boolean isFolder(String filename) throws Exception
    {
        checkDirectory();
        return getFile(filename).isDirectory();
    }


    public void writeToFile(String filename, CheckedFunction<OutputStream, Void> writer) throws Exception
    {
        checkDirectory();
        if (!directory.exists())
            Files.createDirectories(Paths.get(directory.toURI()));
        File file = getFile(filename);
        try (FileOutputStream os = new FileOutputStream(file))
        {
            writer.apply(os);
            os.flush();
        }
    }

    public <C> C readFromFile(String filename, CheckedFunction<InputStream, C> reader) throws Exception
    {
        checkDirectory();
        File file = getFile(filename);
        if (!file.exists()) throw new IOException(String.format("File %s does not exists", filename));
        C result;
        try (FileInputStream is = new FileInputStream(file))
        {
            result = reader.apply(is);
        }
        return result;
    }

    @Override
    public boolean createFolder(String dirname) throws Exception
    {
        checkDirectory();
        return getFile(dirname).mkdir();
    }


    public int delete(Predicate<String> filter) throws Exception
    {
        checkDirectory();
        int i = 0;
        for (String filename : list(filter))
        {
            Path path = Paths.get(directory.getAbsolutePath(), filename);
            File file = new File(path.toString());
            if (!file.exists()) continue;
            if (file.isDirectory() && file.list() != null)
                i += getSubDirectory(filename).delete(ff -> true);
            i += file.delete()? 1 : 0;
        }
        return i;
    }



    private File getFile(String filename) throws Exception
    {
        return new File(getURI(filename));
    }





}
