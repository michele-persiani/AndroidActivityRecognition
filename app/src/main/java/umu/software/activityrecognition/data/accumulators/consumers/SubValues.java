package umu.software.activityrecognition.data.accumulators.consumers;

import umu.software.activityrecognition.data.dataframe.DataFrame;

public class SubValues<T> extends ConstMathOperation<T>
{
    public SubValues(DataFrame.Row values)
    {
        super(values);
    }


    @Override
    public double compute(double cellValue, double constValue)
    {
        return cellValue - constValue;
    }
}
