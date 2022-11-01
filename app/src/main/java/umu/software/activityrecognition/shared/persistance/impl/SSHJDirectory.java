package umu.software.activityrecognition.shared.persistance.impl;

import androidx.annotation.Nullable;

import com.google.common.collect.Sets;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.sftp.FileMode;
import net.schmizz.sshj.sftp.OpenMode;
import net.schmizz.sshj.sftp.RemoteFile;
import net.schmizz.sshj.sftp.RemoteResourceInfo;
import net.schmizz.sshj.sftp.SFTPClient;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Paths;
import java.security.Security;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import umu.software.activityrecognition.shared.persistance.Authentication;
import umu.software.activityrecognition.shared.persistance.IDirectory;
import umu.software.activityrecognition.shared.util.Exceptions;


/**
 * IDirectory that uses SSH and SFTP to access remote directories
 */
public class SSHJDirectory implements IDirectory
{

    private Authentication auth;
    private final SSHClient ssh;
    private URI uri;
    private SFTPClient sftp;

    private final AtomicInteger connectionsCounter = new AtomicInteger(0);

    static {

        Security.removeProvider("BC");
        Security.insertProviderAt(new BouncyCastleProvider(), 1);
    }


    public SSHJDirectory()
    {
        auth = Authentication.noAuthenticationRequired();
        uri = null;
        ssh = new SSHClient();
        ssh.addHostKeyVerifier(new PromiscuousVerifier());
        connectionsCounter.set(1);
    }

    private SSHJDirectory(SSHClient ssh, Authentication auth, URI uri, int connectionsCounter)
    {
        this.ssh = ssh;
        this.auth = auth;
        this.uri = uri;
        this.connectionsCounter.set(connectionsCounter);
    }

    private void checkConnection() throws Exception
    {
        synchronized (ssh)
        {
            if (uri == null || auth == null) throw new Exception("uri == null || auth == null");

            if(!ssh.isConnected())
            {
                ssh.connect(uri.getHost());
                ssh.authPassword(auth.getUsername(), auth.getPassword());
            }
            if (sftp == null)
                sftp = ssh.newSFTPClient();
        }
    }


    private void disconnect() throws Exception
    {
        if (sftp != null) sftp.close();
        connectionsCounter.addAndGet(-1);
        if (connectionsCounter.get() == 0 && ssh.isConnected())
            ssh.disconnect();
    }

    @Override
    public void setAuthentication(@Nullable Authentication auth)
    {
        if (auth == null)
        {
            this.auth = Authentication.noAuthenticationRequired();
            return;
        }
        this.auth = auth.clone();
        if (uri != null && auth.getUsername() != null && !Objects.equals(auth.getUsername(), uri.getUserInfo()))
        {
            URI newUri = URI.create(String.format("sftp://%s@%s%s",
                    auth.getUsername(),
                    uri.getHost(),
                    uri.getPath()
            ));
            Exceptions.runCatch(this::disconnect);
            uri = newUri;
        }
    }

    @Override
    public void setURI(URI dirUri) throws Exception
    {
        if (dirUri == null)
        {
            if (ssh.isConnected()) disconnect();
            uri = null;
            return;
        }

        String scheme = dirUri.getScheme();
        String user = dirUri.getUserInfo();
        String host = dirUri.getHost();

        if (!Objects.equals(scheme, "sftp"))
            throw new IOException("Scheme must be 'sftp' but instead is " + scheme);

        synchronized (ssh)
        {
            if (uri != null && (!Objects.equals(user, auth.getUsername()) || !Objects.equals(host, uri.getHost())))
                disconnect();

            uri = dirUri;
            auth.setUsername(user);
        }
    }

    @Override
    public URI getURI()
    {
        if (uri == null) return null;
        return URI.create(uri.toString());
    }

    @Override
    public URI getURI(String filename)
    {
        if (uri == null)
            return null;
        String user = uri.getUserInfo();
        String path = Paths.get(uri.getPath(), filename).toString();
        String host = uri.getHost();
        return URI.create(String.format("sftp://%s@%s%s", user, host, path));
    }

    @Override
    public IDirectory getSubDirectory(String dirName) throws Exception
    {
        synchronized (ssh)
        {
            SSHJDirectory dir = new SSHJDirectory(ssh, auth, uri, connectionsCounter.addAndGet(1));
            dir.setURI(getURI(dirName));
            return dir;
        }
    }

    @Override
    public IDirectory getParentDirectory() throws Exception
    {
        synchronized (ssh)
        {
            SSHJDirectory dir = new SSHJDirectory(ssh, auth, uri, connectionsCounter.addAndGet(1));
            String user = uri.getUserInfo();
            String path = Paths.get(uri.getPath()).getParent().toString();
            String host = uri.getHost();
            URI parentUri = URI.create(String.format("sftp://%s@%s%s", user, host, path));
            dir.setURI(parentUri);
            return dir;
        }
    }

    @Override
    public List<String> list(@Nullable Predicate<String> filter) throws Exception
    {
        synchronized (ssh)
        {
            checkConnection();
            if (filter == null) filter = (r) -> true;
            return sftp.ls(getURI().getPath())
                    .stream()
                    .map(RemoteResourceInfo::getName)
                    .filter(filter)
                    .collect(Collectors.toList());
        }
    }

    @Override
    public boolean isFolder(String filename) throws Exception
    {
        synchronized (ssh)
        {
            checkConnection();
            return sftp.lstat(getURI(filename).getPath()).getType().equals(FileMode.Type.DIRECTORY);
        }
    }

    @Override
    public void writeToFile(String filename, CheckedFunction<OutputStream, Void> writer) throws Exception
    {
        synchronized (ssh)
        {
            checkConnection();
            String path = getURI(filename).getPath();
            sftp.mkdirs(Paths.get(path).getParent().toString());
            RemoteFile file = sftp.getSFTPEngine().open(path, Sets.newHashSet(OpenMode.WRITE, OpenMode.CREAT));

            try (OutputStream os = file.new RemoteFileOutputStream())
            {
                writer.apply(os);
            }
            file.close();
        }

    }

    @Override
    public <C> C readFromFile(String filename, CheckedFunction<InputStream, C> reader) throws Exception
    {
        synchronized (ssh)
        {
            checkConnection();
            RemoteFile file = sftp.getSFTPEngine().open(getURI(filename).getPath(), Sets.newHashSet(OpenMode.READ));

            try (InputStream is = file.new RemoteFileInputStream())
            {
                return reader.apply(is);
            }
        }
    }

    @Override
    public boolean createFolder(String dirname) throws Exception
    {
        synchronized (ssh)
        {
            checkConnection();
            sftp.mkdir(dirname);
            return true;
        }
    }

    @Override
    public int delete(Predicate<String> filter) throws Exception
    {
        synchronized (ssh)
        {
            checkConnection();
            return (int) list(filter)
                    .stream()
                    .peek(filename -> Exceptions.runCatch(() -> {
                        String path = getURI(filename).getPath();
                        if (isFolder(filename)) sftp.rmdir(path);
                        else sftp.rm(path);
                    })).count();
        }
    }
}
