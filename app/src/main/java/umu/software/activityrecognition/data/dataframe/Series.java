package umu.software.activityrecognition.data.dataframe;

import android.annotation.SuppressLint;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.function.Supplier;

import com.google.common.primitives.Doubles;


public class Series extends ArrayList<Object> {

    public double mean()
    {
        double sum = 0;
        int n = 0;
        for (Object item : this)
        {
            Double value = Doubles.tryParse(item.toString());
            if (value != null ) {
                sum += value;
                n += 1;
            }
        }
        return n == 0 ? 0 : sum / this.size();
    }

    public double variance() {
        double sum = 0;
        double mean = this.mean();
        int n = 0;
        for (Object item: this)
        {
            Double value = Doubles.tryParse(item.toString());
            if (value != null)
            {
                sum += Math.pow(value - mean, 2);
                n += 1;
            }
        }

        return n == 0 ? 0 : sum / (this.size() + 1e-6);
    }

    /**
     *
     * @return standard deviation of this series
     */
    public double std()
    {
        return Math.sqrt(variance());
    }

    public double min()
    {
        double max = Double.MAX_VALUE;
        for (Object item: this)
        {
            Double value = Doubles.tryParse(item.toString());
            if (value != null && value < max)
               max = value;
        }
        return (max == Double.MAX_VALUE)? null : max;
    }


    public double max()
    {
        double min = Double.MIN_VALUE;
        for (Object item: this)
        {
            Double value = Doubles.tryParse(item.toString());
            if (value != null && value > min)
                min = value;
        }
        return (min == Double.MIN_VALUE)? null : min;
    }


    @SuppressLint("DefaultLocale")
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Object item : this) {
            sb.append(String.format("%d\t%s\n", this.indexOf(item), item.toString()));
        }
        return new String(sb);
    }

    @NonNull
    @Override
    public Series clone()
    {
        Series clone = new Series();
        clone.addAll(this);
        return clone;
    }



    public void fill(int desiredSize, Supplier<Object> factory)
    {
        int size = size();
        for (int i = 0; i < desiredSize - size; i++)
            add(factory.get());
    }

    public static Series fillSeries(int size, Supplier<Object> factory)
    {
        Series s = new Series();
        s.fill(size, factory);
        return s;
    }
}
