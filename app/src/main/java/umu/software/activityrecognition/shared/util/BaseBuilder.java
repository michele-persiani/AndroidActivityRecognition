package umu.software.activityrecognition.shared.util;

import java.util.function.Consumer;

public abstract class BaseBuilder<T>
{
    protected T object;

    public BaseBuilder()
    {
        object = newInstance();
    }

    protected abstract T newInstance();

    protected <C extends BaseBuilder<T>> C setFields(C builder, Consumer<T> setter)
    {
        setter.accept(object);
        return builder;
    }


    public T build()
    {
        return object;
    }

}
