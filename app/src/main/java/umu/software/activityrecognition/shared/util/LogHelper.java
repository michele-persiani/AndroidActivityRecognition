package umu.software.activityrecognition.shared.util;

import android.util.Log;


import java.util.function.Supplier;


/**
 * Convenience class for logging
 */
public class LogHelper
{
    private final Supplier<String> tagFunction;

    private LogHelper(Supplier<String> tagFunction)
    {
        this.tagFunction = tagFunction;
    }

    public void d(String message, Object... args)
    {
        Log.d(tagFunction.get(), String.format(message, args));
    }

    public void i(String message, Object... args)
    {
        Log.i(tagFunction.get(), String.format(message, args));
    }

    public void w(String message, Object... args)
    {
        Log.w(tagFunction.get(), String.format(message, args));
    }

    public void e(String message, Object... args)
    {
        Log.w(tagFunction.get(), String.format(message, args));
    }

    public static LogHelper newClassTag(Object caller)
    {
        return new LogHelper(() -> caller.getClass().getSimpleName());
    }

    public static LogHelper newInstance(String tag)
    {
        return new LogHelper(() -> tag);
    }


    public static LogHelper newInstance(Supplier<String> tagSupplier)
    {
        return new LogHelper(tagSupplier);
    }
}
