package umu.software.activityrecognition.shared.audio;



import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

import umu.software.activityrecognition.shared.util.Exceptions;


/**
 * Class to record wave (.wav) files from the system's microphone
 */
public class Wave
{
    private final int LONGINT = 4;
    private final int SMALLINT = 2;
    private final int INTEGER = 4;
    private final int ID_STRING_SIZE = 4;
    private final int WAV_RIFF_SIZE = LONGINT + ID_STRING_SIZE;
    private final int WAV_FMT_SIZE = (4 * SMALLINT) + (INTEGER * 2) + LONGINT + ID_STRING_SIZE;
    private final int WAV_DATA_SIZE = ID_STRING_SIZE + LONGINT;
    private final int WAV_HDR_SIZE = WAV_RIFF_SIZE + ID_STRING_SIZE + WAV_FMT_SIZE + WAV_DATA_SIZE;
    private final short PCM = 1;
    private final int SAMPLE_SIZE = 2;
    private int cursor;
    private final int nSamples;
    private final byte[] output;



    public Wave(int sampleRate, int nChannels, short[] data, int start, int end)
    {
        nSamples = end - start + 1;
        cursor = 0;
        output = new byte[nSamples * SMALLINT + WAV_HDR_SIZE];
        buildHeader(sampleRate, (short)nChannels);
        writeData(data, start, end);
    }



    /**
     * Write to .wav file
     * @param dirname full path of the target directory
     * @param filename target file name
     * @return whether the operation was successful
     */
    public boolean writeToFile(String dirname, String filename)
    {
        return Exceptions.runCatch( () -> {
            String fname = filename;
            Files.createDirectories(Paths.get(dirname));
            if (!fname.endsWith(".wav"))
                fname += ".wav";
            File path = new File(dirname, fname);
            FileOutputStream outFile = new FileOutputStream(path);
            outFile.write(output);
            outFile.close();
            return true;
        }, false);
    }

    /**
     * Write to an output stream
     * @param os output stream
     * @return whether the operation was successful
     */
    public boolean writeToOutputStream(OutputStream os)
    {
        return Exceptions.runCatch( () -> {
            os.write(output);
            os.flush();
        });
    }



    // ------------------------------------------------------------
    private void buildHeader(int sampleRate, short nChannels)
    {
        write("RIFF");
        write(output.length);
        write("WAVE");
        writeFormat(sampleRate, nChannels);
    }


    // ------------------------------------------------------------
    private void writeFormat(int sampleRate, short nChannels)
    {
        write("fmt ");
        write(WAV_FMT_SIZE - WAV_DATA_SIZE);
        write(PCM);
        write(nChannels);
        write(sampleRate);
        write(nChannels * sampleRate * SAMPLE_SIZE);
        write((short) (nChannels * SAMPLE_SIZE));
        write((short) 16);
    }

    // ------------------------------------------------------------
    private void writeData(short[] data, int start, int end)
    {
        write("data");
        write(nSamples * SMALLINT);
        for (int i = start; i < end; i++)
            write(data[i]);
    }

    // ------------------------------------------------------------
    private void write(byte b)
    {
        output[cursor++] = b;
    }

    // ------------------------------------------------------------
    private void write(String id)
    {
        if (id.length() != ID_STRING_SIZE)
            throw new RuntimeException("String (" + id + ") must have four characters.");
        else {
            for (int i = 0; i < ID_STRING_SIZE; ++i) write((byte) id.charAt(i));
        }
    }

    // ------------------------------------------------------------
    private void write(int i)
    {
        write((byte) (i & 0xFF));
        i >>= 8;
        write((byte) (i & 0xFF));
        i >>= 8;
        write((byte) (i & 0xFF));
        i >>= 8;
        write((byte) (i & 0xFF));
    }

    // ------------------------------------------------------------
    private void write(short i)
    {
        write((byte) (i & 0xFF));
        i >>= 8;
        write((byte) (i & 0xFF));
    }
}