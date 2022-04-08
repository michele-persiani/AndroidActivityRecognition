package umu.software.activityrecognition.data.accumulators;

import android.util.Log;

import java.util.concurrent.Callable;

import umu.software.activityrecognition.common.FunctionLock;

public class AccumulatorSupplier<T> implements Callable<T>
{
    private final FunctionLock lock = FunctionLock.make();
    private final Accumulator<T> accumulator;
    private final Callable<T> supplier;
    private boolean running;
    private Thread thread;
    private long delayMillis;

    public AccumulatorSupplier(Accumulator<T> accum, Callable<T> supplier)
    {
        this.accumulator = accum;
        this.supplier = supplier;
        this.running = false;
    }

    @Override
    public T call() throws Exception
    {
        return supplier.call();
    }

    public void start(long delayMillis)
    {
        lock.withLock(() -> {
            if (running)
                return;
            AccumulatorSupplier.this.delayMillis = Math.max(0, delayMillis);
            thread = new Thread(this::run);
            thread.start();
            running = true;
        });
    }


    public void stop()
    {
        lock.withLock(() -> {
            if (!running)
                return;
            thread.interrupt();
            thread = null;
            running = false;
        });
    }


    public boolean running()
    {
        return running;
    }


    private void run()
    {
        while(running())
        {
            try {
                Thread.sleep(delayMillis);
            } catch (InterruptedException e) {
                Log.i(getClass().getSimpleName(), "Supplier interrupted");
                return;
            }
            T event;
            try {
                event = call();
            } catch (Exception e) {
                e.printStackTrace();
                Log.w(getClass().getSimpleName(), "Supplier threw exception:");
                Log.w(getClass().getSimpleName(), e.toString());
                continue;
            }
            accumulator.accept(event);
        }
    }
}
