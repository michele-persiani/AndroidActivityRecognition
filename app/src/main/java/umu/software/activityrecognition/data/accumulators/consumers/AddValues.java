package umu.software.activityrecognition.data.accumulators.consumers;


import umu.software.activityrecognition.data.dataframe.DataFrame;


public class AddValues<T> extends ConstMathOperation<T>
{
    public AddValues(DataFrame.Row values)
    {
        super(values);
    }

    @Override
    public double compute(double cellValue, double constValue)
    {
        return cellValue + constValue;
    }
}
