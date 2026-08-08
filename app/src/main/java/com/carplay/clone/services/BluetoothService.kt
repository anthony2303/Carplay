package com.carplay.clone.services

import android.Manifest
import android.annotation.SuppressLint
import android.app.Service
import android.bluetooth.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import java.util.UUID

class BluetoothService(private val context: Context) {
    
    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var bluetoothSocket: BluetoothSocket? = null
    private val audioManager: AudioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    
    companion object {
        private const val TAG = "BluetoothService"
        private val MY_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        const val NOTIFICATION_ID = 1001
    }
    
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device: BluetoothDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    }
                    device?.let { 
                        Log.d(TAG, "Dispositivo encontrado: ${it.name} - ${it.address}")
                        onDeviceFound?.invoke(it)
                    }
                }
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    Log.d(TAG, "Descubrimiento finalizado")
                    onDiscoveryFinished?.invoke()
                }
                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                    when (state) {
                        BluetoothAdapter.STATE_OFF -> Log.d(TAG, "Bluetooth apagado")
                        BluetoothAdapter.STATE_ON -> Log.d(TAG, "Bluetooth encendido")
                    }
                }
            }
        }
    }
    
    var onDeviceFound: ((BluetoothDevice) -> Unit)? = null
    var onDiscoveryFinished: (() -> Unit)? = null
    
    fun initialize() {
        if (checkPermission()) {
            registerReceiver()
            setupAudioManager()
        }
    }
    
    private fun registerReceiver() {
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
        }
        context.registerReceiver(receiver, filter)
    }
    
    fun startDiscovery(): Boolean {
        return if (checkPermission()) {
            if (bluetoothAdapter?.isDiscovering == true) {
                bluetoothAdapter.cancelDiscovery()
            }
            bluetoothAdapter?.startDiscovery() ?: false
        } else {
            false
        }
    }
    
    @SuppressLint("MissingPermission")
    fun connectToDevice(device: BluetoothDevice): Boolean {
        return try {
            bluetoothSocket = device.createRfcommSocketToServiceRecord(MY_UUID)
            bluetoothAdapter?.cancelDiscovery()
            bluetoothSocket?.connect()
            
            // Cambiar a modo A2DP
            audioManager.apply {
                isBluetoothA2dpOn = true
                mode = AudioManager.MODE_IN_COMMUNICATION
            }
            
            Log.d(TAG, "Conectado a ${device.name}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error conectando: ${e.message}")
            try {
                bluetoothSocket?.close()
            } catch (closeException: Exception) {
                Log.e(TAG, "Error cerrando socket: ${closeException.message}")
            }
            false
        }
    }
    
    fun isConnected(): Boolean {
        return bluetoothSocket?.isConnected == true
    }
    
    fun disconnect() {
        try {
            bluetoothSocket?.close()
            audioManager.apply {
                isBluetoothA2dpOn = false
                mode = AudioManager.MODE_NORMAL
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error desconectando: ${e.message}")
        }
    }
    
    private fun setupAudioManager() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            audioManager.isBluetoothScoOn = true
        }
    }
    
    private fun checkPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }
    
    fun destroy() {
        disconnect()
        try {
            context.unregisterReceiver(receiver)
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering receiver: ${e.message}")
        }
    }
}
