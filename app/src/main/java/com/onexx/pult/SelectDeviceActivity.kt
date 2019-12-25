package com.onexx.pult

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import kotlinx.android.synthetic.main.select_device_layout.*


class SelectDeviceActivity : AppCompatActivity() {

    private var m_bluetoothAdapter: BluetoothAdapter? = null
    private lateinit var pairedDevices: Set<BluetoothDevice>
    private val REQUEST_ENABLE_BLUETOOTH = 1

    companion object {
        val EXTRA_ADRESS: String = "Device_address"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.select_device_layout)

        m_bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        if (m_bluetoothAdapter == null) {
            Toast.makeText(this, "This device doesn't support bluetooth",Toast.LENGTH_LONG).show()
            return
        }
        if (!m_bluetoothAdapter!!.isEnabled) {

            //request enable bluetooth
            val enableBluetoothIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBluetoothIntent, REQUEST_ENABLE_BLUETOOTH)
        }

        //on refresh button click
        selection_device_refresh.setOnClickListener {

            //update list of paired devices
            pairedDeviceList()
        }
    }

    private fun pairedDeviceList() {
        pairedDevices = m_bluetoothAdapter!!.bondedDevices
        val list: ArrayList<BluetoothDevice> = ArrayList()
        val namesList: ArrayList<String> = ArrayList()

        if (pairedDevices.isNotEmpty()) {
            for (device: BluetoothDevice in pairedDevices) {
                list.add(device)
                namesList.add(device.name)
                Log.i("device", device.name + " - " + device)
            }
        } else {
            Toast.makeText(this,"no paired bluetooth devices found",Toast.LENGTH_LONG).show()
        }

        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, namesList)
        selection_device_list.adapter = adapter
        selection_device_list.onItemClickListener =
            AdapterView.OnItemClickListener { _, _, position, _ ->
                val device: BluetoothDevice = list[position]
                val address: String = device.address

                val intent = Intent(this, ControlActivity::class.java)
                intent.putExtra(EXTRA_ADRESS, address)
                startActivity(intent)
            }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_ENABLE_BLUETOOTH) {
            if (resultCode == Activity.RESULT_OK) {
                if (m_bluetoothAdapter!!.isEnabled) {
                    Toast.makeText(this,"Bluetooth has been enabled",Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this,"Bluetooth has been disabled",Toast.LENGTH_LONG).show()
                }
            } else if (resultCode == Activity.RESULT_CANCELED) {
                Toast.makeText(this,"Bluetooth enabling has been cancelled",Toast.LENGTH_LONG).show()
            }
        }
    }
}
