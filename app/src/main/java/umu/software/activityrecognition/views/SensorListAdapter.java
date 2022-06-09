package umu.software.activityrecognition.views;


import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Arrays;
import java.util.List;

import umu.software.activityrecognition.R;
import umu.software.activityrecognition.data.accumulators.SensorAccumulator;

public class SensorListAdapter extends RecyclerView.Adapter<SensorListAdapter.ViewHolder> {

    protected final SensorManager sensorManager;


    public class ViewHolder extends RecyclerView.ViewHolder implements SensorEventListener
    {
        TextView textView;
        Button button;
        SensorAccumulator accumulator;
        boolean registered = false;

        public ViewHolder(View itemView) {
            super(itemView);
            textView = this.itemView.findViewById(R.id.textView);
            button = itemView.findViewById(R.id.button);
        }

        public void register(int sensorNum)
        {
            Sensor sensor = sensorManager.getSensorList(Sensor.TYPE_ALL).get(sensorNum);
            accumulator = new SensorAccumulator(sensorManager, sensor);
            accumulator.setMinDelayMillis(50);
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
            sensorManager.registerListener(accumulator, sensor, SensorManager.SENSOR_DELAY_NORMAL);
            registered = true;
        }

        public void unregister()
        {
            sensorManager.unregisterListener(this);
            sensorManager.unregisterListener(accumulator);
            registered = false;
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


    public SensorListAdapter(SensorManager sensorManager)
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
        List<Sensor> sensors = sensorManager.getSensorList(Sensor.TYPE_ALL);
        String initialText = "No events received from" + sensors.get(position).getName();
        holder.setText(initialText);
        holder.setButtonCallback((e) -> {
            if (!holder.registered)
                holder.register(position);
            else
                holder.unregister();
        });
    }

    @Override
    public void onViewRecycled(@NonNull ViewHolder holder)
    {
        super.onViewRecycled(holder);
        holder.unregister();
        holder.setButtonCallback(null);
    }


    @Override
    public int getItemCount()
    {
        return sensorManager.getSensorList(Sensor.TYPE_ALL).size();
    }

    @Override
    public void onViewDetachedFromWindow(@NonNull ViewHolder holder)
    {
        super.onViewDetachedFromWindow(holder);
        holder.unregister();
    }
}
