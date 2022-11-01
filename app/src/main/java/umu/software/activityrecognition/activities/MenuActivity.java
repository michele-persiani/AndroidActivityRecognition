package umu.software.activityrecognition.activities;

import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;

import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.common.collect.Lists;

import java.util.List;
import java.util.function.Consumer;

import umu.software.activityrecognition.R;
import umu.software.activityrecognition.activities.shared.ListViewActivity;
import umu.software.activityrecognition.activities.shared.MultiItemListViewActivity;


/**
 * Activity that displays a simple menu as a list of buttons
 */
public abstract class MenuActivity extends MultiItemListViewActivity
{
    private boolean mBuilt = false;

    private final List<Consumer<ListViewActivity.ViewHolder>> mBuilders = Lists.newArrayList();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        getSupportActionBar().hide();
        refreshListView();
    }

    @Override
    protected int getMasterLayout()
    {
        return R.layout.holder_main_menu;
    }



    protected int getItemCount()
    {
        return mBuilders.size() + 1;
    }




    /**
     * Add an entry to the menu.
     * @param position index at which insterting the element
     * @param name the text of the button
     * @param buttonListener the listener
     */
    protected void addMenuEntry(int position, String name, View.OnClickListener buttonListener)
    {
        if (mBuilt)
            throw new IllegalStateException("addMenuEntry() must be called inside buildmenu()");
        mBuilders.add(position, (holder) -> {
            Button entryButton = holder.getView().findViewById(R.id.button_entry);
            entryButton.setText(name);
            entryButton.setOnClickListener(buttonListener);
        });
    }


    protected void addMenuEntry(String name, View.OnClickListener buttonListener)
    {
        addMenuEntry(mBuilders.size(), name, buttonListener);
    }


    @Override
    protected void registerBinders()
    {
        mBuilders.clear();
        mBuilt = false;
        buildMenu();
        registerBinding(0, R.id.linearLayout_header, ((viewHolder, integer) -> {
            TextView textView = viewHolder.getView().findViewById(R.id.textView_header);
            textView.setText("Menu");
        }));

        registerDefaultBinding(R.id.linearLayout_entry, (holder, position) -> {
            mBuilders.get(position - 1).accept(holder);
        });
        mBuilt = true;
    }


    /**
     * Build the menu of buttons. Use addMenuEntry() to add entries
     */
    protected abstract void buildMenu();

}
