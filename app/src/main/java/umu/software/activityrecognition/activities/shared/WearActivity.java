package umu.software.activityrecognition.activities.shared;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import umu.software.activityrecognition.R;


/**
 * Base class for activities
 */
public class WearActivity extends AppCompatActivity
{
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        if(getSupportActionBar() != null)
            getSupportActionBar().hide();
        if(getActionBar() != null)
            getActionBar().hide();
    }

    protected boolean isOnWearable()
    {
        return getResources().getBoolean(R.bool.iswearable);
    }
}
