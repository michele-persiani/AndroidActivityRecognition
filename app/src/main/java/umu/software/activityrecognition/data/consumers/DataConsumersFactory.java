package umu.software.activityrecognition.data.consumers;

import android.os.SystemClock;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import umu.software.activityrecognition.data.dataframe.DataFrame;


/**
 * Factory to create event consumers to use in accumulators
 */
public class DataConsumersFactory
{
    private DataConsumersFactory(){ }

    /**
     * Creates a consumer that adds constant values to rows. The number of added values is the length
     * of 'values'.
     * @param colPrefix column prefix
     * @param values values to add
     * @return
     */
    public static Consumer<DataFrame.Row> newAddValues(String colPrefix, double[] values)
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
     * @return
     */
    public static Consumer<DataFrame.Row> newAddValues(DataFrame.Row values)
    {
        return new ConstCellwiseConsumer(values)
        {
            @Override
            public double compute(double cellValue, double constValue)
            {
                return cellValue + constValue;
            }
        };
    }


    /**
     * Creates a consumer that subtracts values 'values' to rows
     * @param values values to add
     * @return
     */
    public static Consumer<DataFrame.Row> newSubValues(String colPrefix, double[] values)
    {
        DataFrame.Row row = new DataFrame.Row();
        for (int i = 0; i < values.length; i++)
            row.put(String.format("%s%s", colPrefix, i), values[i]);
        return newSubValues(row);
    }


    /**
     * Creates a consumer that adds values 'values' to the row, columnwise
     * ie. column 'a' gets added to column 'a' from 'values'
     * @param values values to add
     * @return
     */
    public static Consumer<DataFrame.Row> newSubValues(DataFrame.Row values)
    {
        return new ConstCellwiseConsumer(values)
        {
            @Override
            public double compute(double cellValue, double constValue)
            {
                return cellValue - constValue;
            }
        };
    }


    public static Consumer<DataFrame.Row> newDivideByValues(String colPrefix, double[] values)
    {
        DataFrame.Row row = new DataFrame.Row();
        for (int i = 0; i < values.length; i++)
            row.put(String.format("%s%s", colPrefix, i), values[i]);
        return newDivideByValues(row);
    }


    public static Consumer<DataFrame.Row> newDivideByValues(DataFrame.Row values)
    {
        return new ConstCellwiseConsumer(values)
        {
            @Override
            public double compute(double cellValue, double constValue)
            {
                if (constValue == 0) return Double.NaN;
                return cellValue / constValue;
            }
        };
    }




    public static Consumer<DataFrame.Row> newMulByValues(String colPrefix, double[] values)
    {
        DataFrame.Row row = new DataFrame.Row();
        for (int i = 0; i < values.length; i++)
            row.put(String.format("%s%s", colPrefix, i), values[i]);
        return newMulByValues(row);
    }


    public static Consumer<DataFrame.Row> newMulByValues(DataFrame.Row values)
    {
        return new ConstCellwiseConsumer(values)
        {
            @Override
            public double compute(double cellValue, double constValue)
            {
                return cellValue * constValue;
            }
        };
    }


    /**
     * Consumer selecting a subset of columns and dropping the others
     * @param columns columns to select
     * @return
     */
    public static Consumer<DataFrame.Row> newSelectColumns(String... columns)
    {
        List<String> colList = Lists.newArrayList(columns);
        return row -> {
            Map<String, Object> tmpRow = Maps.newHashMap(row);
            row.clear();
            tmpRow.keySet().stream().filter(colList::contains).forEach(col -> row.put(col, tmpRow.get(col)));
        };
    }

    /**
     * Consumer that adds a timestamp and delta_timestamp generated through SystemClock.elapsedRealtimeNanos()
     * Added columns are 'columnPrefix_timestamp' and 'columnPrefix_delta_timestamp'
     * @return newly created consumer
     */
    public static Consumer<DataFrame.Row> newSystemClockTimestamp(String columnPrefix)
    {
        return new TimestampConsumer(columnPrefix)
        {
            @Override
            protected long currentTimeMillis()
            {
                return TimeUnit.MILLISECONDS.convert(SystemClock.elapsedRealtimeNanos(), TimeUnit.NANOSECONDS);
            }
        };
    }


    /**
     * Consumer that adds a timestamp and delta_timestamp generated through Instant.now().toEpochMilli()
     * Added columns are 'columnPrefix_timestamp' and 'columnPrefix_timestamp'
     * @return newly created consumer
     */
    public static Consumer<DataFrame.Row> newEpochTimestamp(String columnPrefix)
    {
        return new TimestampConsumer(columnPrefix)
        {
            @Override
            protected long currentTimeMillis()
            {
                return Instant.now().toEpochMilli();
            }
        };
    }
}
