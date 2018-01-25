package distance.agilevision.com.beacondistanceapp;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

public class PermissionUtils {

    public static final int BT_ASK_CODE = 23;

    private static boolean isPermissionGranted(Context c, String... permissions) {
        boolean granted = true;
        for (String permission : permissions) {
            granted &= PackageManager.PERMISSION_GRANTED ==
                    ContextCompat.checkSelfPermission(c, permission);
        }
        return granted;
    }

    public static boolean isBluetoothGranted(Context c) {
        return isPermissionGranted(c,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.BLUETOOTH
        );
    }

    public static void askBTPermissions(Activity a) {
        ActivityCompat.requestPermissions(
                a,
                new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.BLUETOOTH_ADMIN,
                        Manifest.permission.BLUETOOTH
                },
                BT_ASK_CODE);
    }


    public static boolean isEverythingGranted(@NonNull int[] grantResults) {
        boolean granted;
        if (grantResults.length == 0) {
            granted = false;
        } else {
            granted = true;
            for (int grantResult : grantResults) {
                if (grantResult != PackageManager.PERMISSION_GRANTED) {
                    granted = false;
                    break;
                }
            }
        }
        return granted;
    }


}
