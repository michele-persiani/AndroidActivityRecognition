package com.c_bata;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;

public class DataFrame extends LinkedHashMap<String, Series>
{
    public static class Row extends LinkedHashMap<String, Object>
    {

    }

    private ReentrantLock semaphore;

    public DataFrame()
    {
        semaphore = new ReentrantLock();
    }

    public boolean hasColumn(String column)
    {
        return withLock(dataFrame -> Arrays.asList(columns()).contains(column));
    }


    public String[] columns()
    {
        return withLock(dataFrame -> keySet().toArray(new String[size()]));
    }


    public void orderColumns(String... columns)
    {
        withLock(dataFrame -> {
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
            return null;
        });
    }


    public Map<String, Double> mean()
    {
        return withLock(dataFrame -> {
            Map<String, Double> meanDataFrame = new LinkedHashMap<>();
            for (String key : this.keySet())
                meanDataFrame.put(key, this.get(key).mean());

            return meanDataFrame;
        });
    }

    public Map<String, Double> std()
    {
        return withLock(dataFrame -> {
            Map<String, Double> meanDataFrame = new LinkedHashMap<>();
            for (String key : this.keySet())
                meanDataFrame.put(key, this.get(key).std());

            return meanDataFrame;
        });
    }

    public int countRows()
    {
        return withLock(dataFrame -> {
            if (size() == 0)
                return 0;
            Entry<String, Series> entry = this.entrySet().iterator().next();
            Series value = entry.getValue();
            return value.size();
        });
    }

    public int appendRow(Map<String, Object> row)
    {
        return withLock(dataFrame -> {

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

        });
    }

    public Object nullElement()
    {
        return "";
    }

    public Map<String, Object> popRow(int rowNum)
    {
        return withLock(dataFrame -> {
            if (rowNum < 0 || rowNum > countRows())
                return null;

            Map<String, Object> res = new LinkedHashMap<>();
            for (Entry<String, Series> e : this.entrySet())
                res.put(e.getKey(), e.getValue().remove(rowNum));
            return res;
        });

    }

    public Map<String, Object> popFirstRow()
    {
        return popRow(0);
    }


    public Map<String, Object> popLastRow()
    {
        return popRow(countRows()-1);
    }


    public Object[] getRowArray(int rowNum)
    {
        return withLock(dataFrame -> {
            if (rowNum < 0 || rowNum > countRows())
                return null;
            Object[] row = new Object[columns().length];
            int i = 0;
            for (String col : columns())
            {
                Object value = this.get(col).get(rowNum);
                row[i] = value;
                i ++;
            }
            return row;
        });
    }

    public <R> ArrayList<R> forEachRow(Function<Object[], R> fun)
    {
        return withLock(dataFrame -> {
            ArrayList<R> result = new ArrayList<>();
            for (int i = 0; i < countRows(); i++)
            {
                Object[] row = getRowArray(i);
                result.add(fun.apply(row));
            }

            return result;
        });
    }


    public <R> R withLock(Function<DataFrame, R> fun)
    {
        semaphore.lock();
        R result = fun.apply(this);
        semaphore.unlock();
        return result;
    }

    @NonNull
    @Override
    public DataFrame clone()
    {
        return withLock(dataFrame -> {
            DataFrame clone = new DataFrame();
            forEach((k, v) -> clone.put(k, v.clone()));
            return clone;
        });
    }

    public String toCSV(boolean printColumnNames)
    {
        return withLock(dataFrame -> {
            StringBuilder builder = new StringBuilder();

            if (printColumnNames)
                addCSVStringsToBuilder(builder, columns());

            forEachRow(objects -> {
                addCSVStringsToBuilder(builder, objects);
                return null;
            });


            return builder.toString();
        });
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
