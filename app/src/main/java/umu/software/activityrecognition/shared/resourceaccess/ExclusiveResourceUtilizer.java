package umu.software.activityrecognition.shared.resourceaccess;


/**
 * Base class for objects utilizing exclusive resources
 */
public abstract class ExclusiveResourceUtilizer
{
    private final ExclusiveAccessToken mToken;


    public ExclusiveResourceUtilizer()
    {
        mToken = ExclusiveResourceAccess.getInstance().createAccessToken(
                getExclusiveResourcesPriority(),
                getExclusiveResourcesIds()
        );

        mToken.setResourceAcquiredCallback(this::onResourcesAcquired);
        mToken.setResourceReleasedCallback(this::onResourcesReleased);
        mToken.setHigherPriorityAcquisitionCallback(this::onHigherPriorityAcquisition);
    }


    protected boolean acquireResources(boolean await)
    {
        return mToken.acquire(await);
    }


    protected void releaseResources()
    {
        mToken.release();
    }


    protected void onResourcesAcquired()
    {

    }


    protected void onResourcesReleased()
    {

    }


    protected void onHigherPriorityAcquisition()
    {

    }

    protected abstract int getExclusiveResourcesPriority();


    protected abstract String[] getExclusiveResourcesIds();
}
