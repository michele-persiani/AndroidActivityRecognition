package umu.software.activityrecognition.shared.util;

import java.util.UUID;

/**
 * Unique int id generator
 */
public class UniqueId
{
    private UniqueId() {}

    public static int uniqueInt()
    {
        UUID idOne = UUID.randomUUID();
        String str = "" + idOne;
        int uid = str.hashCode();
        String filterStr = "" + uid;
        str = filterStr.replaceAll("-", "");
        return Integer.parseInt(str);
    }
}
