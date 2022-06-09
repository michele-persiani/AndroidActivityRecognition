package umu.software.activityrecognition.activities;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreferenceCompat;

import com.google.api.client.util.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import umu.software.activityrecognition.R;
import umu.software.activityrecognition.shared.AndroidUtils;




public class PreferenceActivity extends AppCompatActivity
{

    public static String getSensorKey(Sensor sensor)
    {
        return String.format("RecordSensor_%s", sensor.getName().replace(" ", "_"));
    }



    public static class SensorRecordingsFragment extends PreferenceFragmentCompat
    {


        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey)
        {
            Context context = getActivity();
            PreferenceScreen screen = getPreferenceManager().createPreferenceScreen(context);


            Map<String, Set<Sensor>> sensorsByCategory = groupSensorsByType();
            List<String> sortedTypes = Lists.newArrayList(sensorsByCategory.keySet());
            Collections.sort(sortedTypes);


            for (String type : sortedTypes)
            {
                PreferenceCategory category = new PreferenceCategory(context);
                screen.addPreference(category);
                category.setTitle(type);
                for (Sensor s : sensorsByCategory.get(type))
                {
                    SwitchPreferenceCompat sensorPref = new SwitchPreferenceCompat(context);
                    sensorPref.setKey(getSensorKey(s));
                    sensorPref.setTitle(getTypeString(s));
                    sensorPref.setSummary(s.getName().toUpperCase());
                    category.addPreference(sensorPref);
                }
            }


            setPreferenceScreen(screen);

        }


        private Map<String, Set<Sensor>> groupSensorsByType()
        {
            Context context = getActivity();
            SensorManager sensorManager = AndroidUtils.getSensorManager(context);
            Map<String, Set<Sensor>> result = Maps.newHashMap();

            for (Sensor s : sensorManager.getSensorList(Sensor.TYPE_ALL))
            {
                String type = getTypeString(s);
                if (!result.containsKey(type)) {
                    Set<Sensor> set = Sets.newHashSet();
                    result.put(type, set);
                }
                result.get(type).add(s);
            }
            return result;
        }


        private static String getTypeString(Sensor sensor)
        {
            Map<Integer, String> types = Maps.newHashMap();

            types.put(Sensor.TYPE_ACCELEROMETER, "Acceleration");
            types.put(Sensor.TYPE_ACCELEROMETER_UNCALIBRATED, "Uncalibrated acceleration");
            types.put(Sensor.TYPE_LINEAR_ACCELERATION, "Acceleration");
            types.put(Sensor.TYPE_GYROSCOPE, "Gyroscope");
            types.put(Sensor.TYPE_GYROSCOPE_UNCALIBRATED, "Uncalibrated gyroscope");
            types.put(Sensor.TYPE_GRAVITY, "Gravity");
            types.put(Sensor.TYPE_MAGNETIC_FIELD, "Magnetic field");
            types.put(Sensor.TYPE_POSE_6DOF, "Pose 6DOF");
            types.put(Sensor.TYPE_SIGNIFICANT_MOTION, "Significant motion");
            types.put(Sensor.TYPE_HEART_BEAT, "Heart beat");
            types.put(Sensor.TYPE_HEART_RATE, "Heart rate");
            types.put(Sensor.TYPE_GAME_ROTATION_VECTOR, "Game rotation vector");
            types.put(Sensor.TYPE_PROXIMITY, "Proximity");
            types.put(Sensor.TYPE_PRESSURE, "Pressure");
            types.put(Sensor.TYPE_STEP_COUNTER, "Step counter");
            types.put(Sensor.TYPE_STEP_DETECTOR, "Step detector");
            types.put(Sensor.TYPE_AMBIENT_TEMPERATURE, "Temperature");
            types.put(Sensor.TYPE_LIGHT, "Light");
            types.put(Sensor.TYPE_RELATIVE_HUMIDITY, "Humidity");
            types.put(Sensor.TYPE_ORIENTATION, "Orientation");
            types.put(Sensor.TYPE_STATIONARY_DETECT, "Stationary detector");
            types.put(Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR, "Geomagnetic");
            return types.getOrDefault(sensor.getType(), "Unknown");
        }

    }



    public static class MainPreferencesFragment extends PreferenceFragmentCompat
    {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey)
        {
            setPreferencesFromResource(R.xml.preferences, rootKey);
        }

        @Override
        public void onStart()
        {
            super.onStart();

            findPreference(getString(R.string.recorded_sensors))
                    .setOnPreferenceClickListener((preference -> {
                        requireActivity().getSupportFragmentManager()
                                .beginTransaction()
                                .replace(R.id.fragment_settings, new SensorRecordingsFragment())
                                .addToBackStack("sensors")
                                .commit();
                        return true;
                    }));
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
    }
}
