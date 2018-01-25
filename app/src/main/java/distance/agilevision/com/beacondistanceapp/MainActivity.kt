package distance.agilevision.com.beacondistanceapp

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.TextView
import butterknife.BindView
import butterknife.ButterKnife

class MainActivity : AppCompatActivity(), OnScan, CoordinateTracker {
    override fun onDistanceChange(distances: Map<Identifier, Double>) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    @BindView(R.id.text_x) lateinit var tx: TextView
    @BindView(R.id.text_y) lateinit var ty: TextView
    @BindView(R.id.text_error) lateinit var error: TextView


    override fun onCoordinateChange(x: Double, y: Double) {
        tx.setText(x.toString())
        ty.setText(y.toString())
    }

    override fun startScan() {
        error.setText("Scanning")
    }

    override fun onError(description: String) {
        error.setText("Error scanning: $description")
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        ButterKnife.setDebug(BuildConfig.DEBUG)
        ButterKnife.bind(this)
        tx.setText("detecting...")
        ty.setText("detecting...")
        val beacons: Map<Identifier, Point> = mapOf(
                Identifier("00112233445566778899", "000000000000") to Point(0.0, 0.0),
                Identifier("00112233445566778899", "111111111111") to Point(1.535, 5.66),
                Identifier("00112233445566778899", "111111000000") to Point(0.0, 3.07)
        )
        var c = CoordinateCalculator(this, beacons)
        val bleUtil = BleUtil(this, c)
        bleUtil.scanForDevices(this)

    }
}
