package umu.software.activityrecognition.shared.audio;

import android.annotation.SuppressLint;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.SystemClock;

import androidx.annotation.Nullable;

import java.nio.ShortBuffer;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import umu.software.activityrecognition.shared.util.FunctionLock;
import umu.software.activityrecognition.shared.util.LogHelper;


/**
 * Class to record Wave files by using AudioRecord
 */
public class WaveRecorder
{


    public interface Callback
    {
        default void onStart() {};
        default void onUpdate() {};
        default void onFinish() {};
    }


    private static final int RECORDER_SAMPLERATE = 8000;
    private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    int bufferElements2Rec = 1024; // want to play 2048 (2K) since 2 bytes we use only 1024
    int bytesPerElement = 2; // 2 bytes in 16bit format

    private final AudioRecord recorder;
    private boolean isRecording = false;

    private Thread recordingThread;

    private ShortBuffer buffer = ShortBuffer.allocate(bufferElements2Rec);
    private final FunctionLock bufferLock = FunctionLock.newInstance();

    private final LogHelper logger = LogHelper.newClassTag(this);

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private Callback listener;
    private long deltaNotifyMillis = 50;
    private boolean listenerLock = false;
    private double rmsdB = 0;
    private double silenceThreshold = 0;
    private double alpha = 0.8;


    long lastTimeTalking;
    long currentTime;
    long maxRecordingLength = 0;
    long recordingElapsedTime = 0;


    @SuppressLint("MissingPermission")
    public WaveRecorder()
    {
        recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                RECORDER_SAMPLERATE, RECORDER_CHANNELS,
                RECORDER_AUDIO_ENCODING, bufferElements2Rec * bytesPerElement);
    }

    /**
     * Returns whether the recordings are ongoing
     * @return whether the recordings are ongoing
     */
    public boolean isRecording()
    {
        return isRecording;
    }

    /**
     * Start recording
     */
    public void startRecording()
    {
        synchronized (recorder)
        {
            if (isRecording) return;
            isRecording = true;
            buffer = ShortBuffer.allocate(bufferElements2Rec);
            recordingThread = new Thread(this::readRecoderData, "AudioRecorder Thread");
            rmsdB = 0;
            lastTimeTalking = SystemClock.elapsedRealtime();
            currentTime = SystemClock.elapsedRealtime();
            recorder.startRecording();
            recordingThread.start();
        }
    }

    /**
     * Returns whether AudioRecord has been properly initialized and can record
     * @return  whether AudioRecord has been initialized
     */
    public boolean isInitialized()
    {
        return recorder.getState() == AudioRecord.STATE_INITIALIZED;
    }

    /**
     * Stop recording
     */
    public void stopRecording()
    {
        synchronized (recorder)
        {
            if (!isRecording) return;
            isRecording = false;
            recorder.stop();
            recordingThread = null;
        }
    }

    /**
     * Sets a callback that is notified through registration and at the end of it.
     * NB. the callback is invoked on an Executor thread
     * @param callback callback to use
     */
    public void setCallback(Callback callback)
    {
        this.listener = callback;
    }

    /**
     * Sets the threshold id dB below which the recording is classified as silence. See also getRMS()
     * @param silenceThreshold silence threshold in dB
     */
    public void setSilenceRMSThreshold(double silenceThreshold)
    {
        this.silenceThreshold = silenceThreshold;
    }

    /**
     * Sets every much millis the callback is notified while registering
     * @param millis milliseconds between a call to onUpdate() and the other
     */
    public void setNotifyIntervalMillis(long millis)
    {
        deltaNotifyMillis = Math.max(0, millis);
    }

    /**
     * Sets the maximum length of recordings, regardless of seconds of silence
     * @param millis maximum recordings length
     */
    public void setMaxRecordingLength(long millis)
    {
        maxRecordingLength = millis;
    }

    /**
     * Gets the current volume of the registration
     * @return current volume in dB
     */
    public double getRMS()
    {
        return rmsdB;
    }

    /**
     * Returns the milliseconds of silence that have been most recently passed. Resets each time there
     * is some noise above silence threshold
     * @return the elapsed milliseconds of silence
     */
    public long getElapsedSilenceMillis()
    {
        if (!isRecording()) return 0;
        return currentTime - lastTimeTalking;
    }

    /**
     * Returns a Wave with the current registered data
     * @return a Wave object
     */
    @Nullable
    public Wave getWave()
    {
        if (buffer == null) return null;
        short[] data = buffer.array();
        bufferLock.lock();
        data = Arrays.copyOf(data, data.length);
        bufferLock.unlock();

        return new Wave(
                RECORDER_SAMPLERATE,
                1,
                data,
                0,
                buffer.position()
        );
    }

    private void growData(int increase)
    {
        if (buffer.position() + increase < buffer.capacity()) return;

        ShortBuffer newBuffer = ShortBuffer.allocate(buffer.capacity() * 2);
        newBuffer.put(buffer.array(), 0, buffer.position());
        buffer = newBuffer;
    }


    private void readRecoderData()
    {
        short[] data = new short[bufferElements2Rec];

        double rms;
        double rmsSmoothed = 0;
        int result;
        long deltaTime;
        boolean neverTalked = true;

        if (listener != null)
            executor.submit( () -> listener.onStart() );

        lastTimeTalking = SystemClock.elapsedRealtime();
        currentTime = SystemClock.elapsedRealtime();
        recordingElapsedTime = 0;


        while (recordingThread != null)
        {
            deltaTime = SystemClock.elapsedRealtime() - currentTime;
            currentTime += deltaTime;
            recordingElapsedTime += deltaTime;


            // Append recording --------------------------------------------------------------------
            result = recorder.read(data, 0, bufferElements2Rec);

            if (result < 0)
            {
                // Error. Interrupt recordings
                recordingThread = null;
                logger.e("Error while recording (%s)", result);
                break;
            }
            growData(result);
            bufferLock.lock();
            buffer.put(data, 0, result);
            bufferLock.unlock();



            // Calculate RMS -----------------------------------------------------------------------
            rms = 0;
            for (int i = 0; i < result; i++)
                rms += data[i] * data[i];

            rms = Math.sqrt(rms / (result + 1));
            rmsSmoothed = rmsSmoothed * alpha + (1 - alpha) * rms;
            rmsdB = 20.0 * Math.log10(rmsSmoothed);


            // Notify listener ---------------------------------------------------------------------
            if (listener != null && deltaTime >= deltaNotifyMillis && !listenerLock)
            {
                listenerLock = true;
                executor.submit( () -> {
                    listener.onUpdate();
                    listenerLock = false;
                });
            }

            // Update silence time -----------------------------------------------------------------
            if (neverTalked)
                lastTimeTalking = currentTime;
            if (rmsdB > silenceThreshold)
            {
                lastTimeTalking = currentTime;
                neverTalked = false;
            }

            if (maxRecordingLength > 0 && recordingElapsedTime > maxRecordingLength)
                break;
        }

        if (listener != null)
            executor.submit( () -> listener.onFinish() );
        rmsdB = 0;
        isRecording = false;
        recordingThread = null;
    }

    @Override
    protected void finalize() throws Throwable
    {
        super.finalize();
        stopRecording();
        executor.shutdown();
    }
}
