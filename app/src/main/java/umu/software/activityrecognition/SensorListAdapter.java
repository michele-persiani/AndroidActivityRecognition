package umu.software.activityrecognition;


import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Arrays;
import java.util.List;

import umu.software.activityrecognition.sensors.accumulators.Accumulators;
import umu.software.activityrecognition.sensors.accumulators.SensorAccumulator;

public class SensorListAdapter extends RecyclerView.Adapter<SensorListAdapter.ViewHolder> {

    protected SensorManager sensorManager;
    private List<Sensor> sensors;

    Handler callbacksHandler = new Handler();

    public class ViewHolder extends RecyclerView.ViewHolder implements SensorEventListener {


        TextView textView;
        SensorAccumulator accumulator;


        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            textView = this.itemView.findViewById(R.id.textView);
            resetAccumulator();
        }

        public void resetAccumulator()
        {
            SensorListAdapter.this.sensorManager.unregisterListener(this);
            accumulator = Accumulators.newFactory().make();
        }

        public void setText(String text) {
            textView.setText(text);
        }


        @Override
        public void onSensorChanged(SensorEvent sensorEvent)
        {
            accumulator.onSensorChanged(sensorEvent);
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
        this.sensors = sensorManager.getSensorList(Sensor.TYPE_ALL);
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
        String initialText = "No events received from" + this.sensors.get(position).getName();
        holder.setText(initialText);

        sensorManager.registerListener(holder, this.sensors.get(position), SensorManager.SENSOR_DELAY_GAME, callbacksHandler);
    }

    @Override
    public void onViewRecycled(@NonNull ViewHolder holder)
    {
        super.onViewRecycled(holder);
        sensorManager.unregisterListener(holder);
        holder.resetAccumulator();

    }

    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView)
    {
        super.onDetachedFromRecyclerView(recyclerView);

    }


    @Override
    public int getItemCount()
    {
        return this.sensors.size();
    }

    @Override
    public void onViewDetachedFromWindow(@NonNull ViewHolder holder)
    {
        super.onViewDetachedFromWindow(holder);
        sensorManager.unregisterListener(holder);
    }
}
