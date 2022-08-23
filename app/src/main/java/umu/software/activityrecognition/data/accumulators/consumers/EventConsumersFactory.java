package umu.software.activityrecognition.data.accumulators.consumers;

import android.hardware.SensorEvent;
import android.os.SystemClock;

import java.time.Clock;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

import umu.software.activityrecognition.data.accumulators.consumers.impl.AddValues;
import umu.software.activityrecognition.data.accumulators.consumers.impl.DivideByValues;
import umu.software.activityrecognition.data.accumulators.consumers.impl.MulByValues;
import umu.software.activityrecognition.data.accumulators.consumers.impl.SelectColumns;
import umu.software.activityrecognition.data.accumulators.consumers.impl.SubValues;
import umu.software.activityrecognition.data.accumulators.consumers.impl.TimestampConsumer;
import umu.software.activityrecognition.data.dataframe.DataFrame;


/**
 * Factory to create event consumers to use in accumulators
 */
public class EventConsumersFactory
{
    private EventConsumersFactory(){ }

    /**
     * Creates a consumer that adds constant values to rows. The number of added values is the length
     * of 'values'.
     * @param colPrefix column prefix
     * @param values values to add
     * @param <T> type of processed event. this consumers works for any type of event
     * @return
     */
    public static <T> BiConsumer<T, DataFrame.Row> newAddValues(String colPrefix, double[] values)
    {
        DataFrame.Row row = new DataFrame.Row();
        for (int i = 0; i < values.length; i++)
            row.put(String.format("%s%s", colPrefix, i), values[i]);
        return newAddValues(row);
    }


    /**
     * Creates a consumer that adds values 'values' to the row, columnwise
     * ie. column 'a' gets added to column 'a' from 'values'
     * @param values values to add
     * @param <T> type of processed event
     * @return
     */
    public static <T> BiConsumer<T, DataFrame.Row> newAddValues(DataFrame.Row values)
    {
        return new AddValues<>(values);
    }


    public static <T> BiConsumer<T, DataFrame.Row> newSubValues(String colPrefix, double[] values)
    {
        DataFrame.Row row = new DataFrame.Row();
        for (int i = 0; i < values.length; i++)
            row.put(String.format("%s%s", colPrefix, i), values[i]);
        return newSubValues(row);
    }


    public static <T> BiConsumer<T, DataFrame.Row> newSubValues(DataFrame.Row values)
    {
        return new SubValues<>(values);
    }


    public static <T> BiConsumer<T, DataFrame.Row> newDivideByValues(String colPrefix, double[] values)
    {
        DataFrame.Row row = new DataFrame.Row();
        for (int i = 0; i < values.length; i++)
            row.put(String.format("%s%s", colPrefix, i), values[i]);
        return newDivideByValues(row);
    }


    public static <T> BiConsumer<T, DataFrame.Row> newDivideByValues(DataFrame.Row values)
    {
        return new DivideByValues<>(values);
    }




    public static <T> BiConsumer<T, DataFrame.Row> newMulByValues(String colPrefix, double[] values)
    {
        DataFrame.Row row = new DataFrame.Row();
        for (int i = 0; i < values.length; i++)
            row.put(String.format("%s%s", colPrefix, i), values[i]);
        return newMulByValues(row);
    }


    public static <T> BiConsumer<T, DataFrame.Row> newMulByValues(DataFrame.Row values)
    {
        return new MulByValues<>(values);
    }



    public static <T> BiConsumer<T, DataFrame.Row> newSelectColumns(String... columns)
    {
        return new SelectColumns<>(columns);
    }

    /**
     * Consumer that adds a timestamp and delta_timestamp generated through SystemClock.elapsedRealtimeNanos()
     * Added columns are 'systemclock_timestamp' and 'systemclock_delta_timestamp'
     * @param <T> class of processed events
     * @return newly created consumer
     */
    public static <T> BiConsumer<T, DataFrame.Row> newSystemClockTimestamp()
    {
        return new TimestampConsumer<T>("systemclock")
        {
            @Override
            protected long currentTimeMillis(T e)
            {
                return TimeUnit.MILLISECONDS.convert(SystemClock.elapsedRealtimeNanos(), TimeUnit.NANOSECONDS);
            }
        };
    }

    /**
     * Consumer that adds a timestamp and delta_timestamp generated through SensorEvent().timestamp
     * Added columns are 'sensor_timestamp' and 'sensor_delta_timestamp'
     * @return newly created consumer
     */
    public static BiConsumer<SensorEvent, DataFrame.Row> newSensorTimestamp()
    {
        return new TimestampConsumer<SensorEvent>("sensor")
        {
            @Override
            protected long currentTimeMillis(SensorEvent e)
            {
                return TimeUnit.MILLISECONDS.convert(e.timestamp, TimeUnit.NANOSECONDS);
            }
        };
    }

    /**
     * Consumer that adds a timestamp and delta_timestamp generated through Instant.now().toEpochMilli()
     * Added columns are 'epoch_timestamp' and 'epoch_delta_timestamp'
     * @param <T> class of processed events
     * @return newly created consumer
     */
    public static <T> BiConsumer<T, DataFrame.Row> newEpochTimestamp()
    {
        return new TimestampConsumer<T>("epoch")
        {
            @Override
            protected long currentTimeMillis(T e)
            {
                return Instant.now().toEpochMilli();
            }
        };
    }
}
