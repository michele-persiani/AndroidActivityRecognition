package umu.software.activityrecognition.data.dataframe;

import com.google.common.primitives.Doubles;

import java.util.function.Function;

/**
 * Common operations performed on dataframes
 */
public class Operators
{
    /**
     * Subtract fixed values from each row.  Columns that have a missing or null value in 'values' will be ignored.
     * @param df input dataframe
     * @param values the values to subtract from the columns
     * @return transformed dataframe
     */
    public static DataFrame subtractColumnsValues(DataFrame df, DataFrame.Row values)
    {
        return df.transformByColumn((col, serie) -> {
            if (!values.containsKey(col) || values.get(col) == null)
                return;
            for (int i = 0; i < serie.size(); i++)
            {
                Object o = serie.get(i);
                Double parsed = Doubles.tryParse(o.toString());
                Double parsedValue = Doubles.tryParse(values.get(col).toString());
                serie.set(i, (parsed != null && parsedValue != null)? parsed - parsedValue : o);
            }
        });
    }

    /**
     * Divide each column by a fixed value. Columns that have a missing or null value in 'values' will be ignored.
     * @param df input dataframe
     * @param values the values to divide the column with
     * @return transformed dataframe
     */
    public static DataFrame divideColumnsValues(DataFrame df, DataFrame.Row values)
    {
        return df.transformByColumn((col, serie) -> {
            if (!values.containsKey(col) || values.get(col) == null)
                return;
            for (int i = 0; i < serie.size(); i++)
            {
                Object o = serie.get(i);
                Double parsed = Doubles.tryParse(o.toString());
                Double parsedValue = Doubles.tryParse(values.get(col).toString());
                serie.set(i, (parsed != null && parsedValue != null)? parsed / parsedValue : o);
            }
        });
    }

    /**
     * Subtract from each column its mean value
     * @param df input dataframe
     * @return transformed dataframe
     */
    public static DataFrame subtractMean(DataFrame df)
    {
        return df.transformByColumn((col, serie) -> {
            double mean = serie.mean();
            for (int i = 0; i < serie.size(); i++)
            {
                Object o = serie.get(i);
                Double parsed = Doubles.tryParse(o.toString());
                serie.set(i, (parsed != null)? parsed - mean : o);
            }
        });
    }



    /**
     * Divide each column by its standard deviation
     * @param df input dataframe
     * @return transformed dataframe
     */
    public static DataFrame divideByStd(DataFrame df)
    {
        return df.transformByColumn((col, serie) -> {
            double std = serie.std();
            for (int i = 0; i < serie.size(); i++)
            {
                Object o = serie.get(i);
                Double parsed = Doubles.tryParse(o.toString());
                serie.set(i, (parsed != null)? parsed / (std+1e-6) : o);
            }
        });
    }

    /**
     * Compute zscores columnwise
     * x_n = (x - mean) / std
     * @param df input dataframe
     * @return transformed dataframe
     */
    public static DataFrame zscore(DataFrame df)
    {
        return Operators.divideByStd(
                Operators.subtractMean(df)
        );
    }

    /**
     * Compute zscore columnwise using fixed values for mean and std
     * x_n = (x - mean) / std
     * @param df input dataframe
     * @param meanValues mean value
     * @param stdDeviations std deviation
     * @return transformed dataframe
     */
    public static DataFrame zscore(DataFrame df, DataFrame.Row meanValues, DataFrame.Row stdDeviations)
    {
        return Operators.divideColumnsValues(
                Operators.subtractColumnsValues(df, meanValues),
                stdDeviations
        );
    }


    public static DataFrame zscore(DataFrame df, Function<Integer, String> colFunction, double[] meanValues, double[] stdValues)
    {
        assert meanValues.length == stdValues.length;
        DataFrame.Row mean = new DataFrame.Row();
        DataFrame.Row std = new DataFrame.Row();
        for (int i = 0 ; i < meanValues.length; i++)
        {
            mean.put(colFunction.apply(i), meanValues[i]);
            std.put(colFunction.apply(i), stdValues[i]);
        }
        return Operators.zscore(df, mean, std);
    }

    /**
     * Normalize columnwise the values, setting them between 0 and 1
     * x_n = (x - min) / (max - min)
     * @param df
     * @return transformed dataframe
     */
    public static DataFrame minMaxNormalize(DataFrame df)
    {

        return df.transformByColumn((col, serie) -> {
            double min = serie.min();
            double max = serie.max();
            for (int i = 0; i < serie.size(); i++)
            {
                Object o = serie.get(i);
                Double parsed = Doubles.tryParse(o.toString());
                serie.set(i, (parsed != null) ? (parsed - min) / (max - min) : o);
            }
        });

    }

}
