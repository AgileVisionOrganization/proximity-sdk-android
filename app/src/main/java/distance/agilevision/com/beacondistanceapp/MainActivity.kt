package distance.agilevision.com.beacondistanceapp

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.TextView
import butterknife.BindView
import butterknife.ButterKnife

class MainActivity : AppCompatActivity(), OnScan, CoordinateTracker {
    override fun onDistanceChange(i: Identifier, d: Double) {
        mm[i]?.first?.setText(getString(R.string.beacon_distance, mm[i]?.second, d))
    }

    val a = Identifier("00112233445566778899", "000000000000")
    val c = Identifier("00112233445566778899", "111111111111")
    val b = Identifier("00112233445566778899", "111111000000")

    var mm :Map<Identifier, Pair<TextView, String>> = mutableMapOf()

    @BindView(R.id.text_x) lateinit var tx: TextView
    @BindView(R.id.text_y) lateinit var ty: TextView
    @BindView(R.id.text_error) lateinit var error: TextView
    @BindView(R.id.beacon_a) lateinit var ba: TextView
    @BindView(R.id.beacon_b) lateinit var bb: TextView
    @BindView(R.id.beacon_c) lateinit var bc: TextView


    override fun onCoordinateChange(x: Double, y: Double) {
        tx.setText(getString(R.string.x_coordd, x))
        ty.setText(getString(R.string.y_coord,y))
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
        mm += a to Pair(ba, "A");
        mm += b to Pair(bb, "B");
        mm += c to Pair(bc, "C");
        tx.setText("detecting...")
        ty.setText("detecting...")

        val beacons: Map<Identifier, Point> = mapOf(
                a to Point(0.0, 0.0),
                c to Point(1.535, 5.66),
                b to Point(0.0, 3.07)
        )
        var cc = CoordinateCalculator(this, beacons)
        val bleUtil = BleUtil(this, cc)
        bleUtil.scanForDevices(this)

    }
}
