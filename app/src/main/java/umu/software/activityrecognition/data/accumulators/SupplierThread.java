package umu.software.activityrecognition.data.accumulators;

import android.util.Log;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class SupplierThread<T> implements Supplier<T>
{
    private final Consumer<T> accumulator;
    private final Supplier<T> supplier;
    private boolean running;
    private Thread thread;
    private long delayMillis;

    protected SupplierThread(Consumer<T> accum, Supplier<T> supplier)
    {
        this.accumulator = accum;
        this.supplier = supplier;
        this.running = false;
    }

    @Override
    public T get()
    {
        return supplier.get();
    }

    public synchronized void start(long delayMillis)
    {
        if (running)
            return;
        SupplierThread.this.delayMillis = Math.max(0, delayMillis);
        thread = new Thread(this::run);
        thread.start();
        running = true;
    }


    public synchronized void stop()
    {
        if (!running)
            return;
        thread.interrupt();
        thread = null;
        running = false;
    }


    public synchronized boolean running()
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
            T event = supplier.get();
            if (event != null)
                accumulator.accept(event);
        }
    }
}
