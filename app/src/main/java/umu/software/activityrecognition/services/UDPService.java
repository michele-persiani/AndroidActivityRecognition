package umu.software.activityrecognition.services;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import androidx.annotation.Nullable;

import com.google.common.collect.Maps;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.util.Map;
import java.util.function.Consumer;

import umu.software.activityrecognition.shared.util.Exceptions;



/**
 * Bound service to utilize UDP sockets to send and receive packets.
 * Supports only bind()
 *
 *
 * !!! #TODO no testing has been performed !!!
 *
 *
 */
public class UDPService extends Service
{
    public static final int MAX_PACKET_SIZE = 1024;

    public static class Binder extends android.os.Binder
    {
        private final UDPService mService;
        private final Intent mIntent;

        public Binder(UDPService service, Intent intent)
        {
            mService = service;
            mIntent = intent;
        }

        public boolean registerSocket(String localhost, int port)
        {
            return mService.registerSocket(mIntent, localhost, port);
        }

        public void unregisterSocket()
        {
            mService.unregisterSocket(mIntent);
        }


        public boolean startListening(Consumer<DatagramPacket> listener)
        {
            return mService.startListening(mIntent, listener);
        }

        public void stopListening()
        {
            mService.stopListening(mIntent);
        }


        public boolean sendPacket(String ip, int port, byte[] message)
        {
            return mService.sendPacket(mIntent, ip, port, message);
        }
    }



    protected static class ListenerThread extends Thread
    {
        private final Consumer<DatagramPacket> mListener;
        private final DatagramSocket mSocket;

        public ListenerThread(DatagramSocket socket, Consumer<DatagramPacket> listener)
        {
            mSocket = socket;
            mListener = listener;
        }


        @Override
        public void run()
        {
            DatagramPacket packet = new DatagramPacket(new byte[MAX_PACKET_SIZE], MAX_PACKET_SIZE);
            while (true)
            {
                if (isInterrupted())
                    return;

                try
                {
                    mSocket.setSoTimeout(500);
                    mSocket.receive(packet);
                }
                catch (SocketTimeoutException timeout)
                {
                    continue;
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
                mListener.accept(packet);
            }
        }
    }



    private final Map<Intent, DatagramSocket> mSockets = Maps.newHashMap();
    private final Map<Intent, ListenerThread> mThreads = Maps.newHashMap();


    private DatagramSocket getSocket(Intent intent)
    {
        return mSockets.getOrDefault(intent, null);
    }

    /**
     * Register (bind) a socket for the given intent. Intents can have only one bound socket at any time.
     * @param intent the intent owning the socket to register
     * @param localhost the local address at which registering the socket
     * @param port the port at which registering the socket
     * @return whether the operation was successful
     */
    public boolean registerSocket(Intent intent, String localhost, int port)
    {
        unregisterSocket(intent);
        return Exceptions.runCatch(() -> {
            InetAddress addr = InetAddress.getByName(localhost);
            DatagramSocket socket = new DatagramSocket(port, addr);
            mSockets.put(intent, socket);
        });
    }


    /**
     * Unbind a registered socket
     * @param intent the intent owning the socket to unregister
     */
    public void unregisterSocket(Intent intent)
    {
        stopListening(intent);
        DatagramSocket socket = getSocket(intent);
        if (socket == null)
            return;
        socket.disconnect();
        socket.close();
        mSockets.remove(intent);
    }


    /**
     * Start listening on the registered socket. Requires to have priorly called bindSocket()
     * @param intent the intent owning the socket to start
     * @param listener the listener for the received packets
     * @return whether the operation was successful
     */
    public boolean startListening(Intent intent, Consumer<DatagramPacket> listener)
    {
        if (!mSockets.containsKey(intent))
            return false;

        stopListening(intent);

        ListenerThread thread = new ListenerThread(getSocket(intent), listener);
        mThreads.put(intent, thread);
        thread.start();
        return true;
    }

    /**
     * Stop listening on the registered socket.
     * @param intent the intent owning the socket to stop
     */
    public void stopListening(Intent intent)
    {
        if (!mThreads.containsKey(intent))
            return;
        ListenerThread thread = mThreads.get(intent);
        assert thread != null;
        thread.interrupt();
        mThreads.remove(intent);
    }

    /**
     * Send a UDP packet using the registered socket
     * @param intent the intent owning the socket to use
     * @param ip target host address name or IP address
     * @param port target host address port
     * @param message the message to send
     * @return whether the operation was successful
     */
    public boolean sendPacket(Intent intent, String ip, int port, byte[] message)
    {
        if(getSocket(intent) == null)
            return false;

        message = message.clone();

        byte[] finalMessage = message;
        return Exceptions.runCatch(() -> {
            DatagramPacket packet = new DatagramPacket(finalMessage, finalMessage.length);
            packet.setAddress(InetAddress.getByName(ip));
            packet.setPort(port);
            getSocket(intent).send(packet);
        });
    }


    @Override
    public boolean onUnbind(Intent intent)
    {
        super.onUnbind(intent);
        unregisterSocket(intent);
        return true;
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        for (Intent i : mSockets.keySet())
            unregisterSocket(i);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent)
    {
        return new Binder(this, intent);
    }
}
