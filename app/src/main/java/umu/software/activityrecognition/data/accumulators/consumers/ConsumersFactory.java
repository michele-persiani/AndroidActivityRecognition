package umu.software.activityrecognition.data.accumulators.consumers;

import android.hardware.SensorEvent;

import java.util.function.BiConsumer;

import umu.software.activityrecognition.data.dataframe.DataFrame;

public class ConsumersFactory
{
    private ConsumersFactory(){ }




    public static <T> BiConsumer<T, DataFrame.Row> newAddValues(String colPrefix, double[] values)
    {
        DataFrame.Row row = new DataFrame.Row();
        for (int i = 0; i < values.length; i++)
            row.put(String.format("%s%s", colPrefix, i), values[i]);
        return newAddValues(row);
    }

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


    public static <T> BiConsumer<T, DataFrame.Row> newTimestamp()
    {
        TimestampConsumer<T> c = new TimestampConsumer<T>();
        c.reset();
        return c;
    }

    public static BiConsumer<SensorEvent, DataFrame.Row> newSensorTimestamp()
    {
        return new SensorEventTimestampConsumer();
    }

    public static <T> BiConsumer<T, DataFrame.Row> newSelectColumns(String... columns)
    {
        return new SelectColumns<>(columns);
    }
}
