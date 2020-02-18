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

class ControlActivity : AppCompatActivity() {

    companion object {
        var myUUID: UUID = UUID.fromString("aad0b172-c0a3-4839-9281-6b15deb5d24f")
        var m_bluetoothSocket: BluetoothSocket? = null
        lateinit var progress: ProgressDialog
        lateinit var m_bluetoothAdapter: BluetoothAdapter
        var isConnected: Boolean = false
        lateinit var address: String
    }
    private var mSensorManager: SensorManager? = null
    private var mAccelerometer: Sensor? = null
    private var countBeforeUpdate = 20
    var xValCur = 0
    var yValCur = 0
    var zValCur = 0


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.control_layout)
        //address = intent.getStringExtra(SelectDeviceActivity.EXTRA_ADRESS)!!

        //ConnectToDevice(this).execute()

        //sendOneButton.setOnClickListener { sendCommand("1") }
        //sendZeroButton.setOnClickListener { sendCommand("0") }
        //disconnectButton.setOnClickListener { disconnect() }


        // get reference of the service
        mSensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        // focus in accelerometer
        mAccelerometer = mSensorManager!!.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)





        sensorView.text = 1.toString()

    }
    var state = false

    fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    }


    override fun onResume() {
        super.onResume()
        mSensorManager!!.registerListener(this,mAccelerometer,
            SensorManager.SENSOR_DELAY_GAME)
    }

    override fun onPause() {
        super.onPause()
        mSensorManager!!.unregisterListener(this)
    }


    fun onSensorChanged(event: SensorEvent?) {

        if (event != null) {
            countBeforeUpdate--
            val xVal = event.values[0].roundToInt()
            val yVal = event.values[1].roundToInt()
            val zVal = event.values[2].roundToInt()
            if (abs(xVal) > abs(xValCur)) {
                ValueX.text = xVal.toString()
                xValCur = xVal
            }
            if (abs(yVal) > abs(yValCur)) {
                ValueY.text = yVal.toString()
                yValCur = yVal
            }
            if (abs(zVal) > abs(zValCur)) {
                ValueZ.text = zVal.toString()
                zValCur = zVal
                //if(state)
                //    sendCommand("Off")
                //else
                //    sendCommand("On")
                sensorView.text = state.toString();
                state = !state

            }
            if (countBeforeUpdate == 0) {
                countBeforeUpdate = 20
                ValueX.text = "0"
                ValueY.text = "0"
                ValueZ.text = "0"
                xValCur = 0
                yValCur = 0
                zValCur = 0
            }
        }

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
        sendCommand("${'0' - 1}")
        if (m_bluetoothSocket != null) {
            try {
                m_bluetoothSocket!!.close()
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
        private val context: Context

        init {
            this.context = c
        }

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
}