package com.c_bata;

import android.annotation.SuppressLint;

import androidx.annotation.NonNull;

import java.util.ArrayList;

import com.google.common.primitives.Doubles;

import umu.software.activityrecognition.common.Factory;

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

    public double std() {
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

        return n == 0 ? 0 : sum / this.size();
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



    public void fill(int desiredSize, Factory<Object> factory)
    {
        int size = size();
        for (int i = 0; i < desiredSize - size; i++)
            add(factory.make());
    }

    public static Series fillSeries(int size, Factory<Object> factory)
    {
        Series s = new Series();
        s.fill(size, factory);
        return s;
    }
}
