package umu.software.activityrecognition.shared.util;

import java.util.concurrent.Callable;
import java.util.function.Consumer;


/**
 * Helper class to check when instructions throw an exception
 */
public class Exceptions
{
    public interface RunnableException
    {
        void run() throws Exception;
    }


    private Exceptions() {}




    /**
     * Run the given code and return whether it threw an exception
     * @param run the code to run that could throw an exception
     * @param exceptionValue what to return in case the exception is threw
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
            e.printStackTrace();
            if (catchRunnable != null)
                catchRunnable.accept(e);
            return false;
        }
    }


    /**
     * Run the given code and return whether it threw an exception
     * @param run the code to run that could throw an exception
     * @param catchRunnable what to execute in case the exception is threw
     * @return whether the code ran without throwing an exception
     */
    public static boolean runCatch(RunnableException run, Runnable catchRunnable)
    {
        return runCatch(run, (e) -> catchRunnable.run());
    }


    /**
     * Run the given code and return whether it threw an exception
     * @param run the code to run that could throw an exception
     * @return whether the code ran without throwing an exception
     */
    public static boolean runCatch(RunnableException run)
    {
        return runCatch(run, (Runnable) null);
    }
}
