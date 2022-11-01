package umu.software.activityrecognition.activities.shared;

import android.content.Context;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.jcraft.jsch.ChannelSftp;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

import org.jetbrains.annotations.NotNull;
import umu.software.activityrecognition.R;

public abstract class MultiItemListViewActivity extends ListViewActivity
{
    private int mMasterLayoutId;
    private int mDefaultLayout = -1;
    private Map<Integer, Integer> mLayouts = Maps.newHashMap();
    private Map<Integer, BiConsumer<ViewHolder, Integer>> mBinders = Maps.newHashMap();
    private BiConsumer<ViewHolder, Integer> mDefaultBinder;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        mMasterLayoutId = getMasterLayout();
        registerBinders();
        if (mDefaultLayout < 0)
            throw new IllegalStateException("Default layout must be set");
    }


    /**
     * Gets the master layout resource id. The master layout contains all other elements layouts.
     *
     * @return the master layout resource id
     */
    protected abstract int getMasterLayout();


    /**
     * Calls to registerBinding() should be made here
     */
    protected abstract void registerBinders();


    @Override
    protected void refreshListView()
    {
        mLayouts = Maps.newHashMap();
        mBinders = Maps.newHashMap();
        super.refreshListView();
        registerBinders();
    }


    /**
     *
     * @param position
     * @param subLayoutId sublayout of the master layout from getMasterLayout()
     * @param isDefault
     * @param binder
     */
    protected void registerBinding(int position, int subLayoutId, boolean isDefault, BiConsumer<ViewHolder, Integer> binder)
    {
        mLayouts.put(position, subLayoutId);
        mBinders.put(position, binder);
        if(isDefault)
            registerDefaultBinding(subLayoutId, binder);
    }


    protected void registerBinding(int position, int subLayoutId,BiConsumer<ViewHolder, Integer> binder)
    {
        mLayouts.put(position, subLayoutId);
        mBinders.put(position, binder);
    }


    protected void registerDefaultBinding(int subLayoutId, BiConsumer<ViewHolder, Integer> binder)
    {
        mDefaultLayout = subLayoutId;
        mDefaultBinder = binder;
        mBinders.put(-1, binder);
    }


    @Override
    protected View createListEntryView()
    {
        return getLayoutInflater().inflate(mMasterLayoutId, null);
    }


    @Override
    protected void bindElementView(@NonNull ViewHolder holder, int position)
    {
        Set<Integer> layouts = Sets.newHashSet(mLayouts.values());
        layouts.add(mDefaultLayout);
        layouts.forEach( id -> {
            holder.getView().findViewById(id).setVisibility(View.GONE);
        });

        if (mLayouts.containsKey(position))
        {
            int layout = mLayouts.get(position);
            holder.getView().findViewById(layout).setVisibility(View.VISIBLE);
            mBinders.get(position).accept(holder, position);
        }
        else
        {
            holder.getView().findViewById(mDefaultLayout).setVisibility(View.VISIBLE);
            mDefaultBinder.accept(holder, position);
        }


    }
}
