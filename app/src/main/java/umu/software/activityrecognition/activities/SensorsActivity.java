package umu.software.activityrecognition.activities;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import umu.software.activityrecognition.R;
import umu.software.activityrecognition.activities.shared.ListViewActivity;
import umu.software.activityrecognition.shared.util.AndroidUtils;



public class SensorsActivity extends ListViewActivity
{
    private static class SensorViewHolder extends ViewHolder implements SensorEventListener
    {

        private final TextView textView;
        private int numEvents = 0;

        public SensorViewHolder(@NonNull View itemView)
        {
            super(itemView);
            textView = itemView.findViewById(R.id.textView_summary);
        }
        @Override
        public void onSensorChanged(SensorEvent sensorEvent)
        {
            numEvents += 1;
            String values = Arrays.toString(sensorEvent.values);
            String str = String.format("%s", values.substring(1, values.length()-2));
            textView.setText(str);
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i)
        {

        }
    }


    private List<Sensor> sensors;
    private List<Boolean> recording;
    private SensorManager sensorManager;



    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        sensorManager = AndroidUtils.getSensorManager(this);
        sensors = sensorManager.getSensorList(Sensor.TYPE_ALL);
        recording = sensors.stream().map((s) -> Boolean.FALSE).collect(Collectors.toList());
    }

    @Override
    protected View createListEntryView()
    {
        return getLayoutInflater().inflate(R.layout.holder_sensor, null, false);
    }

    @Override
    protected ViewHolder createViewHolder(View view)
    {
        return new SensorViewHolder(view);
    }


    @Override
    protected int getItemCount()
    {
        return sensors.size();
    }


    @Override
    protected void bindElementView(@NonNull ViewHolder holder, int position)
    {
        SensorViewHolder sensorHolder = (SensorViewHolder) holder;
        View view = sensorHolder.getView();
        Button button = view.findViewById(R.id.button_select);
        TextView textView = view.findViewById(R.id.textView_title);
        textView.setText(sensors.get(position).getName());

        button.setOnClickListener( (btn) -> {
            boolean isRecording = this.recording.get(position);
            if (!isRecording)
                sensorManager.registerListener(sensorHolder, sensors.get(position),  SensorManager.SENSOR_DELAY_NORMAL);
            else
                sensorManager.unregisterListener(sensorHolder);
            recording.set(position, !isRecording);
        });
    }

    @Override
    protected void onViewRecycled(ViewHolder holder)
    {
        super.onViewRecycled(holder);
        sensorManager.unregisterListener((SensorEventListener) holder);
        ((TextView)holder.getView().findViewById(R.id.textView_summary)).setText("");
    }
}
