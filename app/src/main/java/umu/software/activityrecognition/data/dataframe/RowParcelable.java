package umu.software.activityrecognition.data.dataframe;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Pair;

import com.google.common.collect.Maps;

import java.util.Map;

/**
 * Class implementing the Parcelable interface for Dataframe.Row objects.
 * Supports values of type boolean, double, float, integer, string
 */
public class RowParcelable implements Parcelable
{
    public static final String BOOLEAN  = "BOOLEAN";
    public static final String DOUBLE   = "DOUBLE";
    public static final String FLOAT    = "FLOAT";
    public static final String INTEGER  = "INTEGER";
    public static final String STRING   = "STRING";


    private final Map<String, Pair<String, Object>> values = Maps.newLinkedHashMap();

    /**
     * Returns a dataframe row with the previously inserted values
     * @return row with the previously inserted values
     */
    public DataFrame.Row getRow()
    {
        DataFrame.Row row = new DataFrame.Row();
        for (String column : values.keySet())
        {
            Pair<String, Object> value = values.get(column);
            row.put(value.first, value.second);
        }
        return row;
    }

    /**
     * Puts a boolean with the given column and value
     * @param column value's column
     * @param value value
     * @return this instance
     */
    public RowParcelable put(String column, boolean value)
    {
        values.put(column, Pair.create(BOOLEAN, value));
        return this;
    }


    /**
     * Puts a double with the given column and value
     * @param column value's column
     * @param value value
     * @return this instance
     */
    public RowParcelable put(String column, double value)
    {
        values.put(column, Pair.create(DOUBLE, value));
        return this;
    }


    /**
     * Puts a float with the given column and value
     * @param column value's column
     * @param value value
     * @return this instance
     */
    public RowParcelable put(String column, float value)
    {
        values.put(column, Pair.create(FLOAT, value));
        return this;
    }


    /**
     * Puts a integer with the given column and value
     * @param column value's column
     * @param value value
     * @return this instance
     */
    public RowParcelable put(String column, int value)
    {
        values.put(column, Pair.create(INTEGER, value));
        return this;
    }


    /**
     * Puts a string with the given column and value
     * @param column value's column
     * @param value value
     * @return this instance
     */
    public RowParcelable put(String column, String value)
    {
        values.put(column, Pair.create(STRING, value));
        return this;
    }


    /* Implementation of Parcelable */

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i)
    {
        parcel.writeInt(values.size());
        for (String column : values.keySet())
        {
            Pair<String, Object> value = values.get(column);
            writeValueToParcel(parcel, column, value.first ,value.second);
        }
    }


    private void writeValueToParcel(Parcel parcel, String column, String type, Object value)
    {
        parcel.writeString(column);
        parcel.writeString(type);
        switch (type)
        {
            case BOOLEAN:
                parcel.writeInt(((boolean)value)? 1 : 0);
                break;
            case DOUBLE:
                parcel.writeDouble((double) value);
                break;
            case FLOAT:
                parcel.writeFloat((float) value);
                break;
            case INTEGER:
                parcel.writeInt((int) value);
                break;
            case STRING:
                parcel.writeString((String) value);
                break;
        }
    }


    private void readValueFromParcel(Parcel parcel)
    {
        String column = parcel.readString();
        String type = parcel.readString();
        switch (type)
        {
            case BOOLEAN:
                values.put(column, Pair.create(type, parcel.readInt() != 0));
                break;
            case DOUBLE:
                values.put(column, Pair.create(type, parcel.readDouble()));
                break;
            case FLOAT:
                values.put(column, Pair.create(type, parcel.readFloat()));
                break;
            case INTEGER:
                values.put(column, Pair.create(type, parcel.readInt()));
                break;
            case STRING:
                values.put(column, Pair.create(type, parcel.readString()));
                break;
        }
    }

    public static final Creator<RowParcelable> CREATOR = new Creator<RowParcelable>()
    {
        @Override
        public RowParcelable createFromParcel(Parcel in)
        {
            RowParcelable row = new RowParcelable();
            int size = in.readInt();
            for (int i = 0; i < size; i++)
                row.readValueFromParcel(in);
            return row;
        }

        @Override
        public RowParcelable[] newArray(int size)
        {
            return new RowParcelable[size];
        }
    };
}
