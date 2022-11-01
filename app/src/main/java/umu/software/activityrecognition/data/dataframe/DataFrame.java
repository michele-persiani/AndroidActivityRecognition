package umu.software.activityrecognition.data.dataframe;


import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;


/**
 * Class for dataframes
 */
public class DataFrame extends LinkedHashMap<String, Series>
{

    public static class Row extends LinkedHashMap<String, Object>
    {

        public Row clone()
        {
            Row shallowCopy = new Row();
            for (String key : keySet())
                shallowCopy.put(key, get(key));
            return shallowCopy;
        }

    }


    public static final String DEFAULT_NAME = "Unnamed";


    private String name;


    public DataFrame()
    {
        setName(null);
    }


    public DataFrame(String name)
    {
        setName(name);
    }


    public void setName(String name)
    {
        if (name == null)
            name = DEFAULT_NAME;
        this.name = name;
    }


    public String getName()
    {
        return name;
    }

    public synchronized boolean hasColumn(String column)
    {
        return Arrays.asList(columns()).contains(column);
    }

    /**
     *
     * @return ordered array of columns
     */
    public synchronized String[] columns()
    {
        return  keySet().toArray(new String[size()]);
    }


    /**
     * Order the columns with hte same order of the provided array. Columns not contained in the ordering
     * array are appended after those being ordered
     * @param columns columns that will be ordered by their position in the array
     */
    public synchronized void orderColumns(String... columns)
    {
        LinkedHashMap<String, Series> tmp = new LinkedHashMap<>(this);
        clear();
        for (String col : columns)
            if (tmp.containsKey(col))
            {
                put(col, tmp.get(col));
                tmp.remove(col);
            }
        for (String col : tmp.keySet())
            put(col, tmp.get(col));
    }

    /**
     * Compute mean columnwise
     * @return a dataframe with a single row
     */
    public synchronized DataFrame mean()
    {
        return transformByColumn((col, serie) -> {
            double mean = get(col).mean();
            serie.clear();
            serie.add(mean);
        });
    }

    /**
     * Compute standard deviation columnwise
     * @return a dataframe with a single row
     */
    public synchronized DataFrame std()
    {
        return transformByColumn((col, serie) -> {
            double std = get(col).std();
            serie.clear();
            serie.add(std);
        });
    }

    public synchronized int countRows()
    {
        if (size() == 0)
            return 0;
        Entry<String, Series> entry = this.entrySet().iterator().next();
        Series value = entry.getValue();
        return value.size();
    }


    /**
     *
     * @param row
     * @return index of the newly added row
     */
    public synchronized int appendRow(Row row)
    {
        if (row.size() > 0)
        {
            int size = size();
            for (String col : row.keySet())
                if (!hasColumn(col))
                    put(col, Series.fillSeries(size, this::nullElement));

            for (String col : columns())
                get(col).add(row.getOrDefault(col, nullElement()));
        }
        return countRows() - 1;
    }


    /**
     *
     * @param builder
     * @return index of the newly added row
     */
    public synchronized int appendRow(Consumer<Row> builder)
    {
        Row row = new Row();
        builder.accept(row);
        return appendRow(row);
    }






    /**
     * Default cell value used to fill blank cells
     * @return
     */
    public synchronized Object nullElement()
    {
        return "";
    }


    /**
     * Apply a function column-wise
     * @param fun function (column_name, column_serie) -> result
     * @param <R> class of the result
     * @return result of fun
     */
    public synchronized <R> List<R> forEachColumn(BiFunction<String, Series, R> fun)
    {
        ArrayList<R> result = new ArrayList<>();
        for (String column : columns())
        {
            Series s = get(column);
            result.add(fun.apply(column, s));
        }
        return result;
    }

    /**
     * Pops row with the given rowNum
     * @param rowNum
     * @return the popped row, or null if rowNum is outside of the range of valid indeces
     */
    public synchronized Row popRow(int rowNum)
    {
        if (rowNum < 0 || rowNum > countRows())
            return null;

        Row res = new Row();
        forEachColumn((name, serie) -> res.put(name, serie.remove(rowNum)));
        return res;
    }

    public synchronized Row popFirstRow()
    {
        return popRow(0);
    }


    public synchronized Row popLastRow()
    {
        return popRow(countRows()-1);
    }


    public synchronized Object[] getRowArray(int rowNum)
    {
        if (rowNum < 0 || rowNum > countRows())
            return null;
        List<Object> row = forEachColumn((name, serie) -> serie.get(rowNum));
        return row.toArray();
    }

    public synchronized Row getRow(int rowNum)
    {
        if (rowNum < 0 || rowNum > countRows())
            return null;
        Row row = new Row();
        forEachColumn((name, serie) -> row.put(name, serie.get(rowNum)));
        return row;
    }


    public synchronized <R> List<R> forEachRowArray(Function<Object[], R> fun)
    {
        ArrayList<R> result = new ArrayList<>();
        for (int i = 0; i < countRows(); i++)
        {
            Object[] row = getRowArray(i);
            result.add(fun.apply(row));
        }

        return result;
    }

    public synchronized <R> ArrayList<R> forEachRow(Function<Row, R> fun)
    {
        ArrayList<R> result = new ArrayList<>();
        for (int i = 0; i < countRows(); i++)
        {
            Row row = getRow(i);
            result.add(fun.apply(row));
        }

        return result;
    }

    /**
     * Transform the dataframe a row at a time and in-place
     * @param fun function manipulating rows
     * @return The transformed dataframe
     */
    public synchronized DataFrame transformByRow(Consumer<Row> fun)
    {
        DataFrame df = new DataFrame(getName());
        forEachRow((row) -> {
            Row r = row.clone();
            fun.accept(r);
            df.appendRow(r);
            return null;
        });
        return df;
    }

    /**
     * Transform the dataframe a column at a time in-place
     * @param fun function manipulating columns
     * @return The transformed dataframe
     */
    public synchronized DataFrame transformByColumn(BiConsumer<String, Series> fun)
    {
        DataFrame df = new DataFrame(getName());
        forEachColumn((name, col) -> {
            Series s = col.clone();
            fun.accept(name, s);
            df.put(name, s);
            return null;
        });
        return df;
    }

    @NonNull
    @Override
    public synchronized DataFrame clone()
    {
        DataFrame clone = new DataFrame();
        clone.setName(getName());
        forEachColumn((name, serie) -> clone.put(name, serie.clone()));
        return clone;
    }

}
