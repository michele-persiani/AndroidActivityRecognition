package umu.software.activityrecognition.shared.util;

import android.os.Parcel;
import android.os.Parcelable;


/**
 * Utils to marshall/unmarshall parcelable objects
 */
public class ParcelableUtil
{

    private ParcelableUtil() {}

    public static byte[] marshall(Parcelable parceable)
    {
        Parcel parcel = Parcel.obtain();
        parceable.writeToParcel(parcel, 0);
        byte[] bytes = parcel.marshall();
        parcel.recycle();
        return bytes;
    }

    public static Parcel unmarshall(byte[] bytes)
    {
        Parcel parcel = Parcel.obtain();
        parcel.unmarshall(bytes, 0, bytes.length);
        parcel.setDataPosition(0); // This is extremely important!
        return parcel;
    }

    public static <T> T unmarshall(byte[] bytes, Parcelable.Creator<T> creator)
    {
        Parcel parcel = unmarshall(bytes);
        T result = creator.createFromParcel(parcel);
        parcel.recycle();
        return result;
    }
}