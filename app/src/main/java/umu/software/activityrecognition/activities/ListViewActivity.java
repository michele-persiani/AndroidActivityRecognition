package umu.software.activityrecognition.activities;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import umu.software.activityrecognition.R;

/**
 * Activity showing a list of views
 */
public abstract class ListViewActivity extends AppCompatActivity
{

    protected class Adapter extends RecyclerView.Adapter<ViewHolder>
    {

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
        {
            View itemView = ListViewActivity.this.createListEntryView();
            return ListViewActivity.this.createViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position)
        {
            ListViewActivity.this.bindElementView(holder, position);
        }

        @Override
        public void onViewRecycled(@NonNull ViewHolder holder)
        {
            super.onViewRecycled(holder);
            ListViewActivity.this.onViewRecycled(holder);
        }

        @Override
        public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView)
        {
            super.onDetachedFromRecyclerView(recyclerView);
        }

        @Override
        public int getItemCount()
        {
            return ListViewActivity.this.getItemCount();
        }
    }


    protected static class ViewHolder extends RecyclerView.ViewHolder
    {

        public ViewHolder(@NonNull View itemView)
        {
            super(itemView);
        }

        public View getView()
        {
            return itemView;
        }

    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.view_listview);
        RecyclerView recyclerView = findViewById(R.id.listview);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(new ListViewActivity.Adapter());
    }

    /**
     * Creates a ViewHolder for the given view
     * @param view
     * @return
     */
    protected ViewHolder createViewHolder(View view)
    {
        return new ViewHolder(view);
    }

    /**
     * Creates a view for an entry. All entries get their view created throuh this method.
     * @return an element's view
     */
    protected abstract View createListEntryView();

    /**
     * Gets the number of elements to show
     * @return the number of elements to show
     */
    protected abstract int getItemCount();

    /**
     * Binds the ViewHolder to the element of the given position.
     * @param holder the ViewHolder to bind. Previously created through createViewHolder().
     *               getView() returns the element's view
     * @param position the position of the element to bind
     */
    protected abstract void bindElementView(@NonNull ViewHolder holder, int position);


    /**
     * Called when a ViewHolder gets recycled
     * @param holder the holder being recycled
     */
    protected void onViewRecycled(ViewHolder holder)
    {

    }
}
