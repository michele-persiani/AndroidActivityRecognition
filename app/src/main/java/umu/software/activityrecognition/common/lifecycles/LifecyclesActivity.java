package umu.software.activityrecognition.common.lifecycles;

import android.app.Activity;
import android.os.Bundle;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public class LifecyclesActivity extends Activity
{
    private LifecycleComposite mLifecycles = new LifecycleComposite();


    protected void addLifecycleElement(LifecycleElement elem)
    {
        mLifecycles.addLifecycleElement(this, elem);
    }


    protected void removeLifecycleElement(LifecycleElement elem)
    {
        mLifecycles.removeLifecycleElement(this, elem);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        mLifecycles.onCreate(this);
    }

    @Override
    protected void onStart()
    {
        super.onStart();
        mLifecycles.onStart(this);
        List<LifecycleElement> elements = new ArrayList<>();
        populateLifecycles(elements);
        for(LifecycleElement e : elements)
            addLifecycleElement(e);
    }

    @Override
    protected void onStop()
    {
        super.onStop();
        mLifecycles.onStop(this);
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        mLifecycles.onDestroy(this);
    }

    protected void populateLifecycles(List<LifecycleElement> elements)
    {

    }
}
