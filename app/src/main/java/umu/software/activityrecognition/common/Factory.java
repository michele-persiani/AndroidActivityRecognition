package umu.software.activityrecognition.common;

/**
 * Interface for factories
 * @param <T> the type of produced objects
 */
public interface Factory<T>
{
    public T make();
}
