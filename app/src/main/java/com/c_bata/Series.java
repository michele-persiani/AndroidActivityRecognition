package com.c_bata;

import androidx.annotation.NonNull;

import java.util.ArrayList;

import com.google.common.primitives.Doubles;

public class Series extends ArrayList<Object> {

    public double mean() {
        double sum = 0;
        for (Object item : this) {
            // TODO: Correspond missing value
            if (Doubles.tryParse(item.toString()) != null ) {
                sum += Double.parseDouble(item.toString());
            }
        }
        return this.size() == 0 ? 0 : sum / this.size();
    }

    public double std() {
        double sum = 0;
        double mean = this.mean();

        for (Object item: this) {
            if (Doubles.tryParse(item.toString()) != null) {
                sum += Math.pow(Double.parseDouble(item.toString()) - mean, 2);
            }
        }

        return this.size() == 0 ? 0 : sum / this.size();
    }


    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Object item : this) {
            sb.append(String.format("%d\t%.3f\n", this.indexOf(item), item));
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
}
