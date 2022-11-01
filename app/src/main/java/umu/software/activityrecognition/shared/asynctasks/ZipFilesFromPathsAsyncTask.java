package umu.software.activityrecognition.shared.asynctasks;

/**
 * ZipAsynTask that takes lists of file paths to zip
 */
public class ZipFilesFromPathsAsyncTask extends ZipAsyncTask<String>
{
    private final String outputFilePath;


    public ZipFilesFromPathsAsyncTask(String outputFilePath, int bufferSize)
    {
        super(bufferSize);
        this.outputFilePath = outputFilePath;
    }

    @Override
    protected String getInputFilePath(String input)
    {
        return input;
    }

    @Override
    protected String getOutputFileName()
    {
        return outputFilePath;
    }

}
