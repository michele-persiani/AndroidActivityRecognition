package umu.software.activityrecognition.views;


import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import umu.software.activityrecognition.R;
import umu.software.activityrecognition.data.accumulators.SensorAccumulators;
import umu.software.activityrecognition.data.accumulators.SensorAccumulator;
import umu.software.activityrecognition.data.accumulators.SensorAccumulatorManager;

public class SensorListAdapter extends RecyclerView.Adapter<SensorListAdapter.ViewHolder> {

    protected final SensorAccumulatorManager sensorManager;


    public class ViewHolder extends RecyclerView.ViewHolder implements SensorEventListener
    {
        TextView textView;
        Button button;
        SensorAccumulator accumulator;


        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            textView = this.itemView.findViewById(R.id.textView);
            button = itemView.findViewById(R.id.button);
            accumulator = SensorAccumulators.newFactory().make();
        }

        public void setButtonCallback(View.OnClickListener l)
        {
            button.setOnClickListener(l);
        }

        public void setText(String text) {
            textView.setText(text);
        }


        @Override
        public void onSensorChanged(SensorEvent sensorEvent)
        {
            accumulator.onSensorChanged(sensorEvent);
            //Log.i(sensorEvent.sensor.getName(), "Event!");
            String str = accumulator.countReadings() + ' ' + Arrays.toString(sensorEvent.values) +
                    '\n' + sensorEvent.sensor.getName();
            setText(str);
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i)
        {
            accumulator.onAccuracyChanged(sensor, i);
        }

    }


    public SensorListAdapter(SensorAccumulatorManager sensorManager)
    {
        this.sensorManager = sensorManager;

    }


    @NonNull
    @Override
    public SensorListAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
    {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.sensor_holder, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SensorListAdapter.ViewHolder holder, int position)
    {
        List<Sensor> sensors = new ArrayList<>(sensorManager.getSensors().values());
        String initialText = "No events received from" + sensors.get(position).getName();
        holder.setText(initialText);
        holder.setButtonCallback((e) -> {
            String str = sensorManager.getAccumulator(position).countReadings() +
                '\n' + sensorManager.getSensor(position).getName();
            holder.setText(str);
        });
    }

    @Override
    public void onViewRecycled(@NonNull ViewHolder holder)
    {
        super.onViewRecycled(holder);
        holder.setButtonCallback(null);
    }


    @Override
    public int getItemCount()
    {
        return sensorManager.getSensors().size();
    }

    @Override
    public void onViewDetachedFromWindow(@NonNull ViewHolder holder)
    {
        super.onViewDetachedFromWindow(holder);
    }
}
