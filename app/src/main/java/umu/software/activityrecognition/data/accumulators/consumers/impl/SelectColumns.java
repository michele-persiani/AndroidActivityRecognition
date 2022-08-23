package umu.software.activityrecognition.data.accumulators.consumers.impl;

import com.google.common.collect.Lists;

import java.util.List;
import java.util.function.BiConsumer;

import umu.software.activityrecognition.data.dataframe.DataFrame;

public class SelectColumns<T> implements BiConsumer<T, DataFrame.Row>
{

    List<String> columns;

    public SelectColumns(String... columns)
    {
        this.columns = Lists.newArrayList(columns);
    }

    @Override
    public void accept(T o, DataFrame.Row row)
    {
        DataFrame.Row r = new DataFrame.Row();
        r.putAll(row);
        row.clear();
        for (String col : r.keySet())
            if (columns.contains(col))
                row.put(col, r.get(col));
    }
}
