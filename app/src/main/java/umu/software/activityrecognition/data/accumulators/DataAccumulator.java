package umu.software.activityrecognition.data.accumulators;

import androidx.annotation.Nullable;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import umu.software.activityrecognition.data.dataframe.DataFrame;
import umu.software.activityrecognition.data.suppliers.DataSupplier;
import umu.software.activityrecognition.shared.util.FunctionLock;


/**
 * Class to accumulate rows into a dataframe using a supplier
 */
public class DataAccumulator
{
    public static final int DEFAULT_NUM_THREADS = 4;

    private static final ThreadFactory THREAD_FACTORY = new ThreadFactory()
    {
        final AtomicInteger n = new AtomicInteger(0);
        @Override
        public Thread newThread(Runnable runnable)
        {
            Thread th = new Thread(runnable);
            th.setName(DataAccumulator.class.getName() + "_" + n.getAndIncrement());
            return th;
        }
    };

    private static ScheduledExecutorService EXECUTOR = Executors.newScheduledThreadPool(DEFAULT_NUM_THREADS, THREAD_FACTORY);
    private static boolean executorStarted = false;

    private ScheduledFuture<?> schedule;
    private final FunctionLock lock = FunctionLock.newInstance();
    private final DataFrame dataframe = new DataFrame();
    private DataSupplier supplier;


    private int windowSize = -1;
    private long delayMillis = 0L;


    public DataAccumulator() {}

    public DataAccumulator(DataSupplier supplier)
    {
        setSupplier(supplier);
    }


    /**
     * Returns whether the accumulator is currently recording from a supplier
     * @return whether the accumulator is currently recording from a supplier
     */
    public boolean isRecording()
    {
        return schedule != null;
    }


    /**
     * Sets the supplier that will provide the data to this accumulator
     * @param sup supplier to set
     */
    public void setSupplier(@Nullable DataSupplier sup)
    {
        boolean running = isRecording();
        if (running)
            stopRecording();
        supplier = sup;
        if (sup != null)
            dataframe.setName(sup.getName());
        if (running && sup != null)
            startRecording();

    }

    /**
     * Gets the task that appends a row to the dataframe
     * @return the task that appends a row to the dataframe
     */
    private Runnable getAppendRowTask()
    {
        return () -> {
            DataFrame.Row row = new DataFrame.Row();
            if (supplier == null || !supplier.isReady())
                return;
            supplier.accept(row);
            lock.lock();
            if (row.size() == 1)
                dataframe.appendRow(row);
            if (row.size() > 0)
                dataframe.appendRow(row);

            if (windowSize > 0)
                while (dataframe.countRows() > windowSize)
                    dataframe.popFirstRow();
                lock.unlock();
        };
    }


    /**
     * Start the event recordings. Has no effects if supplier is null
     */
    public void startRecording()
    {
        if (isRecording() || supplier == null)
            return;
        supplier.initialize();
        executorStarted = true;

        schedule = EXECUTOR.scheduleAtFixedRate(
                getAppendRowTask(),
                0,
                delayMillis,
                TimeUnit.MILLISECONDS
        );
    }

    /**
     * Stop the event recordings
     */
    public void stopRecording()
    {
        if (!isRecording())
            return;
        schedule.cancel(false);
        schedule = null;
        supplier.dispose();
    }


    /**
     * Set the minimum delay between a reading and the successive.
     * @param millis the new minimum delay between readings
     */
    public void setDelayMillis(long millis)
    {
        delayMillis = Math.max(0, millis);
        if (isRecording())
        {
            schedule.cancel(false);
            schedule = EXECUTOR.scheduleAtFixedRate(
                    getAppendRowTask(),
                    0,
                    delayMillis,
                    TimeUnit.MILLISECONDS
            );
        }
    }



    /**
     * The window size is the maximum number of rows in the dataframe. If set to a negative number
     * the dataframe will be unbounded
     * @param size window size or negative number
     */
    public void setWindowSize(int size)
    {
        windowSize = size;
    }


    /**
     * Resets the dataframe, clearing all of its rows
     */
    public synchronized void clearDataFrame()
    {
        lock.lock();
        dataframe.clear();
        lock.unlock();
    }


    /**
     * Count dataframe rows
     * @return the number of accumulated rows
     */
    public synchronized int countReadings()
    {
        return lock.withLock(dataframe::countRows);
    }


    /**
     * Get a cloned version of the accumulated dataframe
     * @return a cloned version of the accumulated dataframe
     */
    public synchronized DataFrame getDataFrame()
    {
        return lock.withLock(dataframe::clone);
    }



    /**
     * Set number of thread that will be available for accumulating data. Default is DataAccumulator.DEFAULT_NUM_THREADS
     * This method must be called before any invocation of startRecording()
     * @param defaultNumThreads number of threads to use
     */
    public static void setNumThreads(int defaultNumThreads)
    {
        if (executorStarted)
            throw new IllegalStateException("setNumThreads() can be called only before any other operation.");
        EXECUTOR.shutdownNow();
        EXECUTOR = Executors.newScheduledThreadPool(defaultNumThreads, THREAD_FACTORY);
    }
}
