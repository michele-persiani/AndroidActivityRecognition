package umu.software.activityrecognition.shared.services;

import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;

import androidx.annotation.NonNull;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleEventObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LifecycleRegistry;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.common.collect.Maps;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Predicate;

import org.jetbrains.annotations.NotNull;
import umu.software.activityrecognition.shared.util.LogHelper;

/**
 * Started service with an associated lifecycle.
 * Executes commands on a separate thread
 * onCreate() will invoke start event
 * onStartCommand() the first call will invoke start, resume events. Subsequent calls won't throw lifecycle events
 * onDestroy() will invoke pause, stop, destroy events
 */
public abstract class LifecycleService extends Service implements LifecycleOwner
{
    public static final String ACTION_STATE_CHANGED = "umu.software.activityrecognition.ACTION_STATE_CHANGED";
    public static final String EXTRA_CURRENT_STATE  = "EXTRA_CURRENT_STATE";
    public static final String EXTRA_EVENT          = "EXTRA_EVENT";
    public static final String EXTRA_SERVICE        = "EXTRA_SERVICE";

    private LogHelper mLog;
    private LifecycleRegistry mLifecycle;

    private final Map<Predicate<Intent>, Consumer<Intent>> mActions = Maps.newHashMap();

    private ExecutorService mExecutor;


    @NonNull
    public LifecycleRegistry getLifecycle()
    {
        return mLifecycle;
    }



    @Override
    public void onCreate()
    {
        super.onCreate();
        mLifecycle = LifecycleRegistry.createUnsafe(this);
        mLog = LogHelper.newClassTag(this);
        mExecutor = Executors.newSingleThreadExecutor(runnable -> {
            Thread th = new Thread(runnable);
            th.setName("Thread-"+LifecycleService.this.getClass().getSimpleName());
            th.setUncaughtExceptionHandler( (th1, e) -> {
                logger().e("Service thread [%s] got an uncaught exception. Service [%s] will now shut down.", th1, LifecycleService.this.getClass().getSimpleName());
                e.printStackTrace();
                stopSelf();
            });
            return th;
        });

        mLifecycle.addObserver(new LifecycleEventObserver()
        {
            @Override
            public void onStateChanged(@NonNull @NotNull LifecycleOwner source, @NonNull @NotNull Lifecycle.Event event)
            {
                Intent intent = new Intent();
                intent.setAction(ACTION_STATE_CHANGED);
                intent.putExtra(EXTRA_CURRENT_STATE, source.getLifecycle().getCurrentState());
                intent.putExtra(EXTRA_EVENT, event);
                intent.putExtra(EXTRA_SERVICE, getClass());
                LocalBroadcastManager.getInstance(LifecycleService.this).sendBroadcast(intent);
            }
        });
        mLifecycle.handleLifecycleEvent(Lifecycle.Event.ON_CREATE);
    }




    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        logger().i("onStartCommand() -> %s", intent);
        int result = super.onStartCommand(intent, flags, startId);

        if (mLifecycle.getCurrentState().equals(Lifecycle.State.CREATED))
            mLifecycle.handleLifecycleEvent(Lifecycle.Event.ON_START);

        mLifecycle.handleLifecycleEvent(Lifecycle.Event.ON_RESUME);
        mActions.entrySet()
                .stream()
                .filter(e -> e.getKey().test(intent))
                .map(Map.Entry::getValue)
                .forEach(cmd -> cmd.accept(intent));
        mLifecycle.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE);

        return result;
    }


    @Override
    public void onDestroy()
    {
        mLifecycle.handleLifecycleEvent(Lifecycle.Event.ON_STOP);
        mLifecycle.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY);
        super.onDestroy();
        mExecutor.shutdownNow();
    }


    protected LogHelper logger()
    {
        return mLog;
    }


    /**
     * Register a service action to execute code from intents
     * @param command function executed for each intent with action 'action' and having extras 'requiredExtras'
     * @param action action of the processed intents
     * @param requiredExtras extra required for the intent to be processed
     */
    protected void registerAction(@NonNull Consumer<Intent> command, @NonNull String action, String... requiredExtras)
    {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(action);
        registerAction(command, intent -> {
            if (intent == null || intent.getAction() == null || !intent.getAction().equals(action)) return false;
            for (String extra : requiredExtras)
                if (!intent.hasExtra(extra)) return false;
            return true;
        });
    }


    /**
     * Register a service action to execute code from intents
     * @param command function executed for each intent passing 'intentFilter'
     * @param intentFilter function to filter the intents being processed by 'command'
     */
    protected void registerAction(@NonNull Consumer<Intent> command, @NonNull Predicate<Intent> intentFilter)
    {
        mActions.put(intentFilter, command);
    }


    /**
     * Run a command asynchronously on the service's private executor
     * @param cmd command to execute
     */
    protected void runAsync(Runnable cmd)
    {
        mExecutor.submit(cmd);
    }


    /**
     * Checks whether the given event describes a lifecycle event for the given service and event.
     * ie. the intent's action is ACTION_STATE_CHANGED, and its extras EXTRA_CURRENT_STATE, EXTRA_EVENT, EXTRA_SERVICE
     * matches with the provided function parameters
     * @param intent intent to test
     * @param serviceClass tested class of the LifecycleStartedService
     * @param event tested lifecycle event
     * @param <S> subclass of LifecycleStartedService
     * @return true or false
     */
    public static <S extends LifecycleService> boolean isLifecycleBroadcast(Intent intent, Class<S> serviceClass, Lifecycle.Event event)
    {
        if (!intent.getAction().equals(ACTION_STATE_CHANGED) || !intent.hasExtra(EXTRA_SERVICE) || !intent.hasExtra(EXTRA_EVENT))
            return false;
        return intent.getSerializableExtra(EXTRA_EVENT) == event &&
                intent.getSerializableExtra(EXTRA_SERVICE).equals(serviceClass);
    }
}
