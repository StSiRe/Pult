@file:Suppress("DEPRECATION")

package com.onexx.pult

import android.app.ProgressDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.control_layout.*
import java.io.IOException
import java.util.*
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorManager
import android.hardware.SensorEventListener
import kotlin.math.abs
import kotlin.math.roundToInt

class ControlActivity : AppCompatActivity(), SensorEventListener {
    companion object {
        var myUUID: UUID = UUID.fromString("aad0b172-c0a3-4839-9281-6b15deb5d24f")
        var m_bluetoothSocket: BluetoothSocket? = null
        lateinit var progress: ProgressDialog
        lateinit var m_bluetoothAdapter: BluetoothAdapter
        var isConnected: Boolean = false
        lateinit var address: String
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.control_layout)
        address = intent.getStringExtra(SelectDeviceActivity.extraAddress)!!

        //ConnectToDevice(this).execute()

        sendOneButton.setOnClickListener { sendCommand("1") }
        sendZeroButton.setOnClickListener { sendCommand("0") }
        disconnectButton.setOnClickListener { disconnect() }

        mSensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        mAccelerometer = mSensorManager!!.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
        sensorView.text = "NaN"
    }

    private fun sendCommand(input: String) {
        if (m_bluetoothSocket != null) {
            try {
                m_bluetoothSocket!!.outputStream.write(input.toByteArray())
                Log.i("Send command", "successfully sent $input")
            } catch (e: IOException) {
                e.printStackTrace()
            }
        } else {
            Log.i("Send command", "Couldn't send a command: socket is null")
        }
    }

    private fun disconnect() {
        if (isConnected) {
            sendCommand("${'0' - 1}")
        }
        if (m_bluetoothSocket != null) {
            try {
                if (m_bluetoothSocket!!.isConnected) {
                    m_bluetoothSocket!!.close()
                }
                m_bluetoothSocket = null
                isConnected = false
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        finish()
    }

    inner class ConnectToDevice(c: Context) : AsyncTask<Void, Void, String>() {

        private var connectSuccess: Boolean = true
        private val context: Context = c

        override fun onPreExecute() {
            super.onPreExecute()
            progress = ProgressDialog.show(context, "Connecting...", "Please wait")
        }

        override fun doInBackground(vararg params: Void?): String? {
            while (m_bluetoothSocket == null || !m_bluetoothSocket!!.isConnected) {
                m_bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
                try {
                    val device = m_bluetoothAdapter.getRemoteDevice(address)
                    m_bluetoothSocket =
                        device!!.createInsecureRfcommSocketToServiceRecord(myUUID)
                    m_bluetoothAdapter.cancelDiscovery()
                    m_bluetoothSocket!!.connect()
                    connectSuccess = true
                } catch (e: IOException) {
                    connectSuccess = false
                    Log.i("Pult_debug", "Couldn't connect")
                    break
                }
            }
            return null
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            if (!connectSuccess) {
                Log.i("Pult_debug", "Couldn't connect")
                disconnect()
                Toast.makeText(context, "Couldn't connect", Toast.LENGTH_LONG).show()
            } else {
                isConnected = true
                Toast.makeText(context, "Connected", Toast.LENGTH_LONG).show()
            }
            progress.dismiss()
        }
    }

    private var mSensorManager: SensorManager? = null
    private var mAccelerometer: Sensor? = null
    private val delay = 10
    private val gestureMinValue = 5
    private var stateValues = SensorValues(0, 0, 0)
    private var countBeforeUpdate = delay

    override fun onResume() {
        super.onResume()
        mSensorManager!!.registerListener(
            this,
            this.mAccelerometer,
            SensorManager.SENSOR_DELAY_NORMAL //SENSOR_DELAY_FASTEST
        )
    }


    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event != null) {
            countBeforeUpdate--
            val sensorValues = SensorValues(
                event.values[0].roundToInt(),
                event.values[1].roundToInt(),
                event.values[2].roundToInt()
            )

            if (abs(sensorValues.x) > abs(stateValues.x)) {
                ValueX.text = sensorValues.x.toString()
                stateValues.x = sensorValues.x
            }
            if (abs(sensorValues.y) > abs(stateValues.y)) {
                ValueY.text = sensorValues.y.toString()
                stateValues.y = sensorValues.y
            }
            if (abs(sensorValues.z) > abs(stateValues.z)) {
                ValueZ.text = sensorValues.z.toString()
                stateValues.z = sensorValues.z
            }
            if (countBeforeUpdate == 0) {
                //if (stateValues.y == 0) return
                if (stateValues.y >= gestureMinValue) //[Turn on] gesture was performed
                {
                    sensorView.text = "Turn on"
                    sendCommand("1")
                } else if (stateValues.y <= -gestureMinValue) //[Turn off] gesture was performed
                {
                    sensorView.text = "Turn off"
                    sendCommand("0")
                } else
                    sensorView.text = "NaN"


                countBeforeUpdate = delay
                ValueX.text = "0"
                ValueY.text = "0"
                ValueZ.text = "0"
                stateValues = SensorValues(0, 0, 0)
            }
        }

    }
}

data class SensorValues(var x: Int, var y: Int, var z: Int)