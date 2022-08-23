package umu.software.activityrecognition.activities;

import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.common.collect.Lists;

import java.util.List;
import java.util.function.Consumer;

import umu.software.activityrecognition.R;


/**
 * Activity that displays a simple menu as a list of buttons
 */
public abstract class MenuActivity extends ListViewActivity
{
    private static final int BUTTON_ID = 12005;
    private boolean mBuilt = false;

    private final List<Consumer<ListViewActivity.ViewHolder>> mBuilders = Lists.newArrayList();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.view_listview);
        buildMenu();
        mBuilt = true;
        RecyclerView recyclerView = findViewById(R.id.listview);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(new MenuActivity.Adapter());
        getSupportActionBar().hide();
    }

    @Override
    protected View createListEntryView()
    {
        LinearLayout layout = new LinearLayout(this);
        LinearLayout.LayoutParams attributLayoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        layout.setLayoutParams(attributLayoutParams);
        layout.setPadding(0, 5, 0, 5);
        layout.setGravity(Gravity.CENTER);
        Button btn = makeButton();
        btn.setId(BUTTON_ID);
        layout.addView(btn);
        return layout;
    }

    /**
     * Builds a button of the menu. Can set color, font, etc
     */
    protected Button makeButton()
    {
        return new Button(this);
    }


    protected int getItemCount()
    {
        return mBuilders.size();
    }


    @Override
    protected void bindElementView(@NonNull ListViewActivity.ViewHolder holder, int position)
    {
        mBuilders.get(position).accept(holder);
    }

    /**
     * Add an entry to the menu. Can only be used inside buildMenu()
     * @param name the text of the button
     * @param buttonListener the listener
     */
    protected void addMenuEntry(String name, View.OnClickListener buttonListener)
    {
        if (mBuilt)
            return;
        mBuilders.add((holder) -> {
            Button btn = holder.getView().findViewById(BUTTON_ID);
            btn.setText(name);
            btn.setOnClickListener(buttonListener);
        });
    }

    protected void addMenuEntry(int stringResId, View.OnClickListener buttonListener)
    {
        addMenuEntry(getString(stringResId), buttonListener);
    }


    /**
     * Build the menu of buttons. Use addMenuEntry() to add entries
     */
    protected abstract void buildMenu();

}
