package umu.software.activityrecognition.data.suppliers;

import java.util.function.Consumer;

import umu.software.activityrecognition.data.dataframe.DataFrame;


/**
 * Supplier to build pipes of consumers
 */
public class DataPipe implements DataSupplier
{
    /**
     * DataPipe builder
     */
    public static class Builder
    {
        DataPipe pipe;

        Builder(DataPipe pipe)
        {
            this.pipe = pipe;
        }

        public Builder then(DataSupplier supplier)
        {
            pipe = new DataPipe(pipe){
                @Override
                public void accept(DataFrame.Row row)
                {
                    super.accept(row);
                    supplier.accept(row);
                }

                @Override
                public void initialize()
                {
                    super.initialize();
                    supplier.initialize();
                }

                @Override
                public void dispose()
                {
                    super.dispose();
                    supplier.dispose();
                }

                @Override
                public boolean isReady()
                {
                    return super.isReady() && supplier.isReady();
                }
            };
            return this;
        }

        public Builder then(Consumer<DataFrame.Row> supplier)
        {
            pipe = new DataPipe(pipe){
                @Override
                public void accept(DataFrame.Row row)
                {
                    super.accept(row);
                    supplier.accept(row);
                }
            };
            return this;
        }

        public DataSupplier build()
        {
            return pipe;
        }
    }



    private final DataSupplier supplier;

    private DataPipe(DataSupplier supplier)
    {
        this.supplier = supplier;
    }

    @Override
    public String getName()
    {
        return supplier.getName();
    }

    @Override
    public void initialize()
    {
        supplier.initialize();
    }

    @Override
    public boolean isReady()
    {
        return supplier.isReady();
    }

    @Override
    public void dispose()
    {
        supplier.dispose();
    }

    @Override
    public void accept(DataFrame.Row row)
    {
        supplier.accept(row);
    }


    /**
     * Start constructing a DataPipe through a builder
     * @param supplier first base supplier of the pipe
     * @return a pipe builder
     */
    public static Builder startWith(DataSupplier supplier)
    {
        return new Builder(new DataPipe(supplier));
    }


}
