# Navigation lib
This lib allows you to track your coordinate calculated from distances to Bluetooth Low Energy (BLE) devices around. This lib only produces approximate coordinates, since RSSI of same beacon goes up and down all the time on same distance. Also you should note that if there's an obstacle between current device and beacon rssi signal will go down which will increase the distance to the beacon. The recommended amount of beacon is 4. Those beacon should be placed on the perimeter.
 
 It's also recommended to configure beacon's txPower constant for each device before using it. It can be done wit BeaconSet+. You need to measure beacon's rssi at 1m and sum it with 41 where 41 is typical signal loss at 1 meter. This value should be set as TxPower. For example if rssi at 1 meter is -56db, txPower should be set to -15. 

## Demos
Check  [MainActivity.kt](app/src/main/java/distance/agilevision/com/beacondistanceapp/MainActivity.kt) and [MainActivityJava.java](app/src/main/java/distance/agilevision/com/beacondistanceapp/MainActivityJava.java) as examples. 

## Minimal code usage

The lib contains of 2 main classes `BeaconsSearcher` that fires a callback whenever it finds a new ble device and `CoordinateCalculator` that accept ble device and if it matches the registered one - calculates the current coordinates based on distance to it.

So for the minimal use you want to register beacons with [x,y] position using `.addBeacon(new Beacon("00112233445566778899", "000000000000"), new XYPoint(0.0, 0.0))` where `00112233445566778899` is **namespace** of the beacon and `000000000000` is **instance**. `0.0` and `0.0` is [x,y] position on the map for the beacon.

```
    CoordinateCalculator cc = new CoordinateBuilder()
        .addBeacon(new Beacon("00112233445566778899", "000000000000"), new XYPoint(0.0, 0.0))
        .addBeacon(new Beacon("00112233445566778899", "111111111111"), new XYPoint(3.07, 0.0))
        .addBeacon(new Beacon("00112233445566778899", "000000111111"), new XYPoint(3.07, 5.66))
        .trackCoordinate(new CoordinateTracker() {
          @Override
          public void onCoordinateChange(double x, double y) {
            Log.d("BLE", String.format("{%.2f, %.2f}", x, y));
          }
        })
        .build();
    BeaconsSearcher bs = new BeaconsSearcher(new OnScanError() {
      @Override
      public void onError(@NotNull ErrorType description) {
        Toast.makeText(MainActivityJava.this, description.getDescription(), Toast.LENGTH_LONG).show();
      }
    }, cc);

    bs.scanForDevices(YourActivity.this);

``` 


## Bluetooth permissions
Permissions are already specified in library's [AndroidManifest](navigator/src/main/AndroidManifest.xml). This manifest will be merged with yours when apk is compiled. However for android6.0+ you also need to ask for bluetooth permissions at runtime:

```
if (!isBluetoothGranted()) {
  askBTPermissions()
} else {
 bs.scanForDevices(this);
}
   
```
Permissions methods:
```
 private Boolean isPermissionGranted(Context c, String... permissions) {
    Boolean granted = true;
    for (String permission : permissions) {
      granted = granted & (PackageManager.PERMISSION_GRANTED
          == ContextCompat.checkSelfPermission(c, permission));
    }
    return granted;
  }

  private Boolean isBluetoothGranted() {
    return isPermissionGranted(this,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.BLUETOOTH_ADMIN,
        Manifest.permission.BLUETOOTH
    );
  }

  private void askBTPermissions() {
    ActivityCompat.requestPermissions(
        this,
        new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.BLUETOOTH
        },
        444);
  }
```



## Enable logs
If your distance isn't accurate you can enable logs with   `builder.enableLogs()`
## Track change of distance to each beacon
```
builder
 .trackDistance(new DistanceTracker() {
    @Override
    public void onDistanceChange(@NotNull Beacon i, double current, double medium) {
      Log.d("BLE", String.format("Current distance to  %s  is %.2f (average is %.2f)", i, current, medium));
    }
})
```
## Cache beacons distance
By default library saves beacon rssi and then calculates the distance based on average rssi to each beacon. By default this time is 4 seconds. You can turn this feature off with `builder.cacheTime(null)` or configure this time with same method.

## Specify formula for distance calculation
By default library uses `Math.pow(10.0, (txPower - SIGNAL_LOSS_1M - rssi) / (10 * 2))` which seems to give more accurate result than others. If you want to use common formula which is `(coef1)*((rssi/power)^coef2) + coef3`, you can set coefficients with 
```
builder.withCalcMethod(coef1, coef2, coef3)
```