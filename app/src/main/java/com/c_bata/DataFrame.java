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

    private ReentrantLock semaphore;

    public DataFrame(String... columns)
    {
        semaphore = new ReentrantLock();
        setColumns(columns);
    }

    private void setColumns(String[] columns)
    {
        for (String col : columns)
            this.put(col, new Series());
    }

    public String[] columns()
    {
        return keySet().toArray(new String[size()]);
    }


    public void orderColumns(String... columns)
    {
        withLock(dataFrame -> {
            LinkedHashMap<String, Series> tmp = new LinkedHashMap<>(this);
            clear();
            for (String col : columns) {
                put(col, tmp.get(tmp));
                tmp.remove(col);
            }
            for (String col : tmp.keySet())
                put(col, tmp.get(col));
            return null;
        });
    }


    public Map<String, Double> mean ()
    {
        return withLock(dataFrame -> {
            Map<String, Double> meanDataFrame = new LinkedHashMap<>();
            for (String key : this.keySet())
                meanDataFrame.put(key, this.get(key).mean());

            return meanDataFrame;
        });
    }

    public Map<String, Double> std ()
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
            if (entrySet().size() == 0)
                return 0;
            Entry<String, Series> entry = this.entrySet().iterator().next();
            Series value = entry.getValue();
            return value.size();
        });
    }

    public int appendRow(Map<String, Object> row)
    {
        return withLock(dataFrame -> {
            if (columns().length == 0)
            {
                String[] cols = Arrays.copyOf(row.keySet().toArray(), row.keySet().size(), String[].class);
                setColumns(cols);
            }

            if (row.size() != columns().length)
                throw new RuntimeException("DataFrame: row.size() != columns.length");

            for (Entry<String, Object> e : row.entrySet())
                get(e.getKey()).add(e.getValue());

            return countRows() - 1;

        });
    }

    public Map<String, Object> popRow(int rowNum)
    {
        return withLock(dataFrame -> {
            if (rowNum < 0 || rowNum > countRows())
                return null;

            Map<String, Object> res = new LinkedHashMap<>();
            for (Entry<String, Series> e : entrySet())
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


    public Object[] getRow(int n)
    {
        return withLock(dataFrame -> {
            Object[] row = new Object[columns().length];
            int i = 0;
            for (String col : columns())
            {
                Object value = this.get(col).get(n);
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
                Object[] row = getRow(i);
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
            DataFrame clone = new DataFrame(columns().clone());
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
        for (int i = 0; i < objs.length - 1; i++)
            builder.append(objs[i].toString()).append(",");

        builder.append(objs[objs.length-1]);
        builder.append(System.lineSeparator());
    }
}
