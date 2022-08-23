package umu.software.activityrecognition.shared.util;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;


/**
 * Lock to execute functions in a thread-safe way
 */
public class FunctionLock
{
    private final Lock lock;

    private FunctionLock(Lock lock)
    {
        this.lock = lock;
    }



    /**
     * Locks the internal lock
     */
    public void lock()
    {
        lock.lock();
    }

    /**
     * Unlocks the internal lock
     */
    public void unlock()
    {
        lock.unlock();
    }


    /**
     * Perform the given operation with lock
     * @param fun the function to execute with lock
     * @param <T> type of the input
     * @param <R> type of the result
     * @return the result of the function
     */
    public <T, R> R withLock(T input, Function<T, R> fun)
    {
        lock();
        R result = fun.apply(input);
        unlock();
        return result;
    }


    /**
     * Perform the given operation with lock
     * @param fun the function to execute with lock
     * @param <R> type of the result
     * @return the result of the function
     */
    public <R> R withLock(Supplier<R> fun)
    {
        lock();
        R result = fun.get();
        unlock();
        return result;
    }

    /**
     * Perform the given operation with lock
     * @param fun the function to execute with lock
     * @param <T> type of the input
     * @return the result of the function
     */
    public <T> void withLock(T input, Consumer<T> fun)
    {
        lock();
        fun.accept(input);
        unlock();
    }



    /**
     * Perform the given operation with lock
     * @param fun the function to execute with lock
     * @return the result of the function
     */
    public void withLock(Runnable fun)
    {
        lock();
        fun.run();
        unlock();
    }



    public static FunctionLock newInstance(Lock lock)
    {
        return new FunctionLock(lock);
    }


    public static FunctionLock newInstance()
    {
        Lock lock = new ReentrantLock();
        return new FunctionLock(lock);
    }
}
