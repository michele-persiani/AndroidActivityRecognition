package umu.software.activityrecognition.activities;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import umu.software.activityrecognition.R;

public abstract class ButtonsActivity extends Activity
{

    List<Integer> buttonIds = Arrays.asList(
            R.id.button1,
            R.id.button2,
            R.id.button3,
            R.id.button4,
            R.id.button5,
            R.id.button6
            );

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.buttons_gui);
        buildButtons();
        for (Integer id : buttonIds)
            if (id > 0)
                findViewById(id).setVisibility(View.GONE);
    }

    protected abstract void buildButtons();


    protected void buildButton(int buttonId, CharSequence text, View.OnClickListener callback)
    {
        Button btn = findViewById(buttonId);
        btn.setText(text);
        btn.setOnClickListener(callback);
        buttonIds.set(buttonIds.indexOf(btn.getId()), -1);
    }
}
