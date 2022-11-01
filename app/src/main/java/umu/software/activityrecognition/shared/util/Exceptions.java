package umu.software.activityrecognition.shared.util;

import androidx.annotation.NonNull;

import com.google.common.collect.Lists;

import java.io.OutputStream;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;


/**
 * Helper class to check when instructions throw an exception. Also provides logging functionalities
 */
public class Exceptions
{
    public static final List<Throwable> logs = Lists.newArrayList();

    public interface RunnableException
    {
        void run() throws Exception;
    }


    private Exceptions() {}


    private static void addLog(Throwable tw)
    {
        logs.add(tw);
        if (logs.size() > 100)
            logs.remove(0);
    }


    /**
     * Writes the logged exceptions to an OutputStream
     * @param os output stream to use
     * @param writer writer function
     * @param <T> subcalss of OutputStream
     */
    public static <T extends OutputStream> void writeLogToStream(T os, BiConsumer<T, Throwable> writer)
    {
        for (Throwable tw : logs)
            writer.accept(os, tw);
    }



    /**
     * Run the given code and return whether it threw an exception
     * @param run the code to run that could throw an exception
     * @param exceptionValue result to return in case the exception is threw
     * @return if 'run' was successful its result, otherwise 'exceptionValue'
     */
    public static <T> T runCatch(Callable<T> run, T exceptionValue)
    {
        try
        {
            return run.call();
        }
        catch (Exception e)
        {
            addLog(e);
            e.printStackTrace();
            return exceptionValue;
        }
    }




    /**
     * Run the given code and return whether it threw an exception
     * @param run the code to run that could throw an exception
     * @param catchRunnable what to execute in case the exception is threw
     * @return whether the code ran without throwing an exception
     */
    public static boolean runCatch(RunnableException run, Consumer<Exception> catchRunnable)
    {
        try
        {
            run.run();
            return true;
        }
        catch (Exception e)
        {
            addLog(e);
            e.printStackTrace();
            catchRunnable.accept(e);
            return false;
        }
    }


    /**
     * Run the given code and return whether it threw an exception
     * @param run the code to run that could throw an exception
     * @return whether the code ran without throwing an exception
     */
    public static boolean runCatch(RunnableException run)
    {
        return runCatch(() -> {
            run.run();
            return true;
        }, false);
    }

    /**
     * Try a number of times to run a command and execute some code each time it fails.
     * @param tries number of tries
     * @param command command that should succeed
     * @param onRetry code to run each time the command fails
     * @param <T> type of returned value
     * @return the command's result
     */
    public  static <T> T tryRetry(int tries, Callable<T> command, RunnableException onRetry) throws Exception
    {
        for (int i = 0; i < tries; i++)
        {
            try
            {
                return command.call();
            }
            catch (Throwable t)
            {
                t.printStackTrace();
                if (i == tries - 1)
                    throw new Exception("Maximum tries of "+tries+" reached.", t);
                onRetry.run();
            }
        }
        return null;
    }

    /**
     * Try a number of times to run a command and execute some code each time it fails.
     * @param tries number of tries
     * @param command command that should succeed
     * @param onRetry code to run each time the command fails
     * @return whether the command has been executed successfully or it threw an exception in the process
     * @throw e if maximum number of tries is exceeded
     */
    public static boolean tryRetry(int tries, RunnableException command, RunnableException onRetry)
    {
        try
        {
            return Boolean.TRUE.equals(
                    tryRetry(
                            tries,
                            () -> {
                                command.run();
                                return true;
                            },
                            onRetry
                    ));
        } catch (Exception e)
        {
            return false;
        }
    }
}
