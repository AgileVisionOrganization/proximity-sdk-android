package io.agilevision.proximity.indoor.demo;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.widget.Button;
import android.widget.TextView;

import io.agilevision.priximity.indoor.Beacon;
import io.agilevision.priximity.indoor.BeaconsSearcher;
import io.agilevision.priximity.indoor.CoordinateBuilder;
import io.agilevision.priximity.indoor.CoordinateCalculator;
import io.agilevision.priximity.indoor.CoordinateTracker;
import io.agilevision.priximity.indoor.DistanceTracker;
import io.agilevision.priximity.indoor.OnScanError;
import io.agilevision.priximity.indoor.XYPoint;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

public class MainActivityJava extends AppCompatActivity implements OnScanError, CoordinateTracker, DistanceTracker {
  @BindView(R.id.text_x) TextView tx;
  @BindView(R.id.text_y) TextView ty;
  @BindView(R.id.text_error) TextView error;
  @BindView(R.id.beacon_a) TextView ba;
  @BindView(R.id.beacon_b) TextView bb;
  @BindView(R.id.beacon_c) TextView bc;
  @BindView(R.id.beacon_d) TextView bd;
  @BindView(R.id.beacon_scan_btn) Button btnScan;
  BeaconsSearcher bs;
  private Map<Beacon, Holder> mm = new HashMap<>();

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setContentView(R.layout.activity_main);
    ButterKnife.setDebug(BuildConfig.DEBUG);
    ButterKnife.bind(this);
    Beacon a = new Beacon("00112233445566778899", "000000000000");
    Beacon b = new Beacon("00112233445566778899", "111111000000");
    Beacon c = new Beacon("00112233445566778899", "111111111111");
    Beacon d = new Beacon("00112233445566778899", "000000111111");
    mm.put(a, new Holder(ba, "A (AC:23:3F:23:C7:87)"));
    mm.put(b, new Holder(bb, "B (AC:23:3F:23:C7:85)"));
    mm.put(c, new Holder(bc, "C (AC:23:3F:23:C7:D2)"));
    mm.put(d, new Holder(bd, "D (AC:23:3F:24:05:7D)"));

    CoordinateCalculator cc = new CoordinateBuilder()
        .addBeacon(a, new XYPoint(0.0, 0.0))
        .addBeacon(b, new XYPoint(3.07, 0.0))
        .addBeacon(c, new XYPoint(3.07, 5.66))
        .addBeacon(d, new XYPoint(0.0, 5.66))
        .trackCoordinate(this)
        .trackDistance(this)
        .build();
    bs = new BeaconsSearcher(this, cc);
    onScanClick(null);

  }

  @OnClick(R.id.beacon_scan_btn)
  void onScanClick(Button btn) {
    if (!isBluetoothGranted()) {
      askBTPermissions();
    } else {
      error.setVisibility(GONE);
      if (bs.getScanRunning()) {
        bs.stopScan();
        btnScan.setText("Start scan");
      } else {
        bs.scanForDevices(this);
        btnScan.setText("Stop scann");
      }
    }
  }

  @Override
  public void onCoordinateChange(double x, double y) {
    tx.setText(getString(R.string.x_coordd, x));
    ty.setText(getString(R.string.y_coord, y));
  }

  @Override
  public void onError(OnScanError.ErrorType description) {
    btnScan.setText("Start scan");
    error.setVisibility(VISIBLE);
    error.setText(getString(R.string.err_scan, description.getDescription()));
  }

  @Override
  public void onDistanceChange(@NotNull Beacon i, double current, double medium) {
    mm.get(i).tv.setText(getString(R.string.beacon_distance, mm.get(i).text, current, medium));
  }

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

  class Holder {
    private TextView tv;
    private String text;
    Holder(TextView tv, String text) {
      this.tv = tv;
      this.text = text;
    }
  }
}
