package distance.agilevision.com.beacondistanceapp

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.widget.TextView
import butterknife.BindView
import butterknife.ButterKnife
import com.agilevision.navigator.*

class MainActivity : AppCompatActivity(), com.agilevision.navigator.OnScan, CoordinateTracker {


    val BT_ASK_CODE = 23

    private fun isPermissionGranted(c: Context, vararg permissions: String): Boolean {
        var granted = true
        for (permission in permissions) {
            granted = granted and (PackageManager.PERMISSION_GRANTED
                    == ContextCompat.checkSelfPermission(c, permission))
        }
        return granted
    }

    fun isBluetoothGranted(): Boolean {
        return isPermissionGranted(this,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.BLUETOOTH
        )
    }

    fun askBTPermissions() {
        ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.BLUETOOTH_ADMIN,
                        Manifest.permission.BLUETOOTH
                ),
                BT_ASK_CODE)
    }


    override fun onDistanceChange(i: Identifier, current: Double, medium: Double?) {
        mm[i]?.first?.setText(getString(R.string.beacon_distance, mm[i]?.second, current, medium))
    }

    val a = Identifier("00112233445566778899", "000000000000")
    val b = Identifier("00112233445566778899", "111111000000")
    val c = Identifier("00112233445566778899", "111111111111")
    val d = Identifier("00112233445566778899", "000000111111")

    var mm: MutableMap<Identifier, Pair<TextView, String>> = mutableMapOf()

    @BindView(R.id.text_x) lateinit var tx: TextView
    @BindView(R.id.text_y) lateinit var ty: TextView
    @BindView(R.id.text_error) lateinit var error: TextView
    @BindView(R.id.beacon_a) lateinit var ba: TextView
    @BindView(R.id.beacon_b) lateinit var bb: TextView
    @BindView(R.id.beacon_c) lateinit var bc: TextView
    @BindView(R.id.beacon_d) lateinit var bd: TextView


    override fun onCoordinateChange(x: Double, y: Double) {
        tx.setText(getString(R.string.x_coordd, x))
        ty.setText(getString(R.string.y_coord, y))
    }

    override fun startScan() {
        error.setText("Scan is in proggress")
    }

    override fun onError(description: String) {
        error.setText("Error scanning: $description")
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        ButterKnife.setDebug(BuildConfig.DEBUG)
        ButterKnife.bind(this)
        mm.put(a, Pair(ba, "A (AC:23:3F:23:C7:87)"));
        mm.put(b, Pair(bb, "B (AC:23:3F:23:C7:85)"));
        mm.put(c, Pair(bc, "C (AC:23:3F:23:C7:D2)"));
        mm.put(d, Pair(bd, "D (AC:23:3F:24:05:7D)"));

        val beacons: Map<Identifier, Point> = mapOf(
                a to Point(0.0, 0.0),
                b to Point(3.07, 0.0),
                c to Point(3.07, 5.66),
                d to Point(0.0, 5.66)
        )
        var cc = CoordinateCalculator(this, beacons)
        val bleUtil = BleUtil(this, cc)

        if (!isBluetoothGranted()) {
            askBTPermissions()
        } else {
            bleUtil.scanForDevices(this)
        }

    }
}
