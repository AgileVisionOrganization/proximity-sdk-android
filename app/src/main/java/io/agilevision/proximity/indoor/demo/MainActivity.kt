package io.agilevision.proximity.indoor.demo

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.Button
import android.widget.TextView
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnClick
import com.agilevision.navigator.*

class MainActivity : AppCompatActivity(), com.agilevision.navigator.OnScanError, CoordinateTracker, DistanceTracker {

    @BindView(R.id.text_x) lateinit var tx: TextView
    @BindView(R.id.text_y) lateinit var ty: TextView
    @BindView(R.id.text_error) lateinit var error: TextView
    @BindView(R.id.beacon_a) lateinit var ba: TextView
    @BindView(R.id.beacon_b) lateinit var bb: TextView
    @BindView(R.id.beacon_c) lateinit var bc: TextView
    @BindView(R.id.beacon_d) lateinit var bd: TextView
    @BindView(R.id.beacon_scan_btn) lateinit var btnScan: Button

    var mm: MutableMap<Beacon, Pair<TextView, String>> = mutableMapOf()
    lateinit var bs: BeaconsSearcher

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        ButterKnife.setDebug(BuildConfig.DEBUG)
        ButterKnife.bind(this)
        val a = Beacon("00112233445566778899", "000000000000")
        val b = Beacon("00112233445566778899", "111111000000")
        val c = Beacon("00112233445566778899", "111111111111")
        val d = Beacon("00112233445566778899", "000000111111")
        mm.put(a, Pair(ba, "A (AC:23:3F:23:C7:87)"));
        mm.put(b, Pair(bb, "B (AC:23:3F:23:C7:85)"));
        mm.put(c, Pair(bc, "C (AC:23:3F:23:C7:D2)"));
        mm.put(d, Pair(bd, "D (AC:23:3F:24:05:7D)"));

        val cc = CoordinateBuilder()
                .addBeacon(a, XYPoint(0.0, 0.0))
                .addBeacon(b, XYPoint(3.07, 0.0))
                .addBeacon(c, XYPoint(3.07, 5.66))
                .addBeacon(d, XYPoint(0.0, 5.66))
                .trackCoordinate(this)
                .trackDistance(this)
                .build()
        bs = BeaconsSearcher(this, cc)
        onScanClick(null)

    }

    @OnClick(R.id.beacon_scan_btn)
    fun onScanClick(btn: Button?) {
        if (!isBluetoothGranted()) {
            askBTPermissions()
        } else {
            error.visibility = GONE
            if (bs.scanRunning) {
                bs.stopScan()
                btnScan.setText("Start scan")
            } else {
                bs.scanForDevices(this)
                btnScan.setText("Stop scann")
            }
        }
    }

    override fun onCoordinateChange(x: Double, y: Double) {
        tx.setText(getString(R.string.x_coordd, x))
        ty.setText(getString(R.string.y_coord, y))
    }

    override fun onError(description: OnScanError.ErrorType) {
        btnScan.setText("Start scan")
        error.visibility = VISIBLE
        error.setText(getString(R.string.err_scan, description.description))
    }

    override fun onDistanceChange(i: Beacon, current: Double, medium: Double) {
        mm[i]?.first?.setText(getString(R.string.beacon_distance, mm[i]?.second, current, medium))
    }

    private fun isPermissionGranted(c: Context, vararg permissions: String): Boolean {
        var granted = true
        for (permission in permissions) {
            granted = granted and (PackageManager.PERMISSION_GRANTED
                    == ContextCompat.checkSelfPermission(c, permission))
        }
        return granted
    }

    private fun isBluetoothGranted(): Boolean {
        return isPermissionGranted(this,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.BLUETOOTH
        )
    }

    private fun askBTPermissions() {
        ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.BLUETOOTH_ADMIN,
                        Manifest.permission.BLUETOOTH
                ),
                444)
    }
}
