package umu.software.activityrecognition.shared.permissions;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;


public enum Permissions
{
    READ_EXTERNAL_STORAGE (android.Manifest.permission.READ_EXTERNAL_STORAGE),
    WRITE_EXTERNAL_STORAGE (android.Manifest.permission.WRITE_EXTERNAL_STORAGE),
    RECORD_AUDIO(Manifest.permission.RECORD_AUDIO),
    ACTIVITY_RECOGNITION(Manifest.permission.ACTIVITY_RECOGNITION),
    BODY_SENSORS(Manifest.permission.BODY_SENSORS),
    BLUETOOTH(Manifest.permission.BLUETOOTH),
    CALL_PHONE(Manifest.permission.CALL_PHONE),
    VIBRATE(Manifest.permission.VIBRATE),
    CAMERA(Manifest.permission.CAMERA),
    WAKE_LOCK(Manifest.permission.WAKE_LOCK),
    ACCESS_WIFI_STATE(Manifest.permission.ACCESS_WIFI_STATE),
    CHANGE_WIFI_STATE(Manifest.permission.CHANGE_WIFI_STATE);

    public static int REQUEST_CODE = 180000;
    private final String permissionId;


    Permissions(String permissionId)
    {
        this.permissionId = permissionId;
    }

    public boolean hasPermission(Context context)
    {
        return ContextCompat.checkSelfPermission(context, permissionId) == PackageManager.PERMISSION_GRANTED;
    }

    public void askPermission(Activity activity)
    {
        if(!hasPermission(activity))
            ActivityCompat.requestPermissions(activity, new String[] {permissionId}, Permissions.REQUEST_CODE);
    }

    public static void askPermissions(Activity activity)
    {
        Permissions[] permissions = Permissions.values();
        String[] permissionValues = new String[Permissions.values().length];
        for (int i = 0; i < permissions.length; i++)
            permissionValues[i] = permissions[i].permissionId;
        ActivityCompat.requestPermissions(activity, permissionValues, Permissions.REQUEST_CODE);
    }
}
