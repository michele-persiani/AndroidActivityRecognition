package umu.software.activityrecognition.common;

import android.app.Activity;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.lifecycle.Lifecycle;

import java.util.HashSet;

public class LifecyclesActivity extends Activity
{
    private HashSet<Lifecycle> elements;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

    }
}
