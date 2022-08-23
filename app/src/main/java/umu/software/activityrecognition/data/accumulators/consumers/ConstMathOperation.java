package umu.software.activityrecognition.data.accumulators.consumers;

import com.google.common.primitives.Doubles;

import java.util.function.BiConsumer;

import umu.software.activityrecognition.data.dataframe.DataFrame;

public abstract class ConstMathOperation<T> implements BiConsumer<T, DataFrame.Row>
{

    DataFrame.Row values;

    public ConstMathOperation(DataFrame.Row values)
    {
        this.values = values;
    }

    @Override
    public void accept(T o, DataFrame.Row row)
    {
        for (String key : values.keySet())
            if (row.containsKey(key))
            {
                Double vRow   = Doubles.tryParse(row.get(key).toString());
                Double vConst = Doubles.tryParse(values.get(key).toString());
                if (vRow != null && vConst != null)
                    row.put(key, compute(vRow, vConst));
            }
    }

    /**
     * Compute cell-wise operation on the row
     * @param cellValue cell value
     * @param constValue constant value
     * @return
     */
    public abstract double compute(double cellValue, double constValue);
}
