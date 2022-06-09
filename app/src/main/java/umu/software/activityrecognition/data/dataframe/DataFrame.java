package umu.software.activityrecognition.data.dataframe;


import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

public class DataFrame extends LinkedHashMap<String, Series>
{
    public static final String DEFAULT_NAME = "Unnamed";

    public static class Row extends LinkedHashMap<String, Object>
    {
        public Row set(String column, Object value)
        {
            put(column, value);
            return this;
        }

        public Row clone()
        {
            Row shallowCopy = new Row();
            for (String key : keySet())
                shallowCopy.put(key, get(key));
            return shallowCopy;
        }
    }

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


    public synchronized String[] columns()
    {
        return  keySet().toArray(new String[size()]);
    }


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


    public synchronized DataFrame mean()
    {
        return transformByColumn((col, serie) -> {
            double mean = get(col).mean();
            serie.clear();
            serie.add(mean);
        });
    }

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

    public synchronized int appendRow(Map<String, Object> row)
    {

        int size = size();
        for (String col : row.keySet())
            if (!hasColumn(col))
                put(col, Series.fillSeries(size, this::nullElement));

        for (String col : row.keySet())
        {
            Object cell = row.getOrDefault(col, nullElement());
            cell = (cell == null)? nullElement() : cell;
            get(col).add(cell);
        }

        return countRows() - 1;

    }

    public synchronized int appendRow(Consumer<Row> builder)
    {
        Row row = new Row();
        builder.accept(row);
        return appendRow(row);
    }


    public synchronized Object nullElement()
    {
        return "";
    }



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
     * Transform the dataframe a row at a time in-place
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
     * Transform the dataframe a coumn at a time in-place
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



    public synchronized String toCSV(boolean printColumnNames)
    {
        StringBuilder builder = new StringBuilder();

        if (printColumnNames)
            addCSVStringsToBuilder(builder, columns());

        forEachRowArray(objects -> {
            addCSVStringsToBuilder(builder, objects);
            return null;
        });
        return builder.toString();
    }

    private static void addCSVStringsToBuilder(StringBuilder builder, Object[] objs)
    {
        if (objs.length == 0)
            return;
        for (int i = 0; i < objs.length - 1; i++)
            builder.append(objs[i].toString()).append(",");
        builder.append(objs[objs.length-1]);
        builder.append(System.lineSeparator());
    }
}
