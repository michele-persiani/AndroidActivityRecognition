package umu.software.activityrecognition.shared.resourceaccess;


/**
 * Common exclusive resources constants
 */
public class ExclusiveResource
{
    /**
     * The device's audio input such as microphone
     */
    public static final String AUDIO_INPUT = "AUDIO_INPUT";

    /**
     * The device's audio output such as speakers
     */
    public static final String AUDIO_OUTPUT = "AUDIO_OUTPUT";


    public static final int PRIORITY_LOW = 0;
    public static final int PRIORITY_NORMAL = 50;
    public static final int PRIORITY_HIGH = 100;
}
