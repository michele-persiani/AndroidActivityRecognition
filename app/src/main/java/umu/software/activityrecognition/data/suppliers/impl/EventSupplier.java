package umu.software.activityrecognition.data.suppliers.impl;

import java.util.function.BiConsumer;

import umu.software.activityrecognition.data.dataframe.DataFrame;
import umu.software.activityrecognition.data.suppliers.DataSupplier;



public class EventSupplier<T> implements DataSupplier
{

    private final String name;
    private final BiConsumer<T, DataFrame.Row> rowBuilder;
    private final boolean resetOnRead;
    private T event;

    public EventSupplier(String name, BiConsumer<T, DataFrame.Row> rowBuilder, boolean resetOnRead)
    {
        this.name = name;
        this.rowBuilder = rowBuilder;
        this.resetOnRead = resetOnRead;
    }

    @Override
    public String getName()
    {
        return name;
    }


    public void setEvent(T event)
    {
        this.event = event;
    }


    @Override
    public void initialize()
    {

    }

    @Override
    public boolean isReady()
    {
        return event != null;
    }

    @Override
    public void dispose()
    {

    }

    @Override
    public void accept(DataFrame.Row row)
    {
        if (event != null)
            rowBuilder.accept(event, row);
        if(resetOnRead)
            event = null;
    }

}
