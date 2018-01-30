    
## Code samples

### Minimal example example

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


## Additional methods

| Tables        | Are           | Cool  |
| ------------- |:-------------:| -----:|
| col 3 is      | right-aligned | $1600 |
| col 2 is      | centered      |   $12 |
| zebra stripes | are neat      |    $1 |