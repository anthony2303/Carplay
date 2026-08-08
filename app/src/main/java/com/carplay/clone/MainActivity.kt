package com.carplay.clone

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.carplay.clone.databinding.ActivityMainBinding
import com.carplay.clone.services.BluetoothService
import com.carplay.clone.services.VoiceControlService
import com.carplay.clone.services.WebRTCService
import com.carplay.clone.ui.MapsFragment
import com.carplay.clone.ui.MessagesFragment
import com.carplay.clone.ui.MusicFragment
import com.carplay.clone.ui.PhoneFragment
import com.carplay.clone.utils.NightModeManager
import com.carplay.clone.utils.PermissionManager
import com.google.android.material.snackbar.Snackbar

class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    private lateinit var nightModeManager: NightModeManager
    private lateinit var permissionManager: PermissionManager
    
    private var bluetoothService: BluetoothService? = null
    private var webRTCService: WebRTCService? = null
    private var voiceControlService: VoiceControlService? = null
    
    private val currentFragment: Fragment?
        get() = supportFragmentManager.findFragmentById(R.id.fragmentContainer)
    
    companion object {
        private const val PERMISSION_REQUEST_CODE = 100
        private val REQUIRED_PERMISSIONS = mutableListOf(
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.READ_CONTACTS
        ).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
                add(Manifest.permission.READ_MEDIA_AUDIO)
            } else {
                add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        initializeComponents()
        setupUI()
        checkPermissions()
        setupListeners()
        
        // Mostrar fragmento inicial
        if (savedInstanceState == null) {
            navigateToMusic()
        }
    }
    
    private fun initializeComponents() {
        nightModeManager = NightModeManager(this)
        permissionManager = PermissionManager(this)
        
        bluetoothService = BluetoothService(this)
        webRTCService = WebRTCService(this)
        voiceControlService = VoiceControlService(this)
    }
    
    private fun setupUI() {
        // Configurar modo inmersivo
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.let { controller ->
                controller.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            )
        }
        
        // Actualizar hora
        updateTime()
        
        // Configurar modo noche
        nightModeManager.applyNightMode()
    }
    
    private fun checkPermissions() {
        val permissionsToRequest = REQUIRED_PERMISSIONS.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        
        if (permissionsToRequest.isNotEmpty()) {
            if (permissionsToRequest.any { 
                ActivityCompat.shouldShowRequestPermissionRationale(this, it) 
            }) {
                showPermissionRationale(permissionsToRequest.toTypedArray())
            } else {
                ActivityCompat.requestPermissions(
                    this,
                    permissionsToRequest.toTypedArray(),
                    PERMISSION_REQUEST_CODE
                )
            }
        } else {
            initializeServices()
        }
    }
    
    private fun showPermissionRationale(permissions: Array<String>) {
        val permissionNames = permissions.map { perm ->
            when {
                perm.contains("BLUETOOTH") -> "Bluetooth"
                perm.contains("LOCATION") -> "Ubicación"
                perm.contains("AUDIO") -> "Micrófono"
                perm.contains("PHONE") -> "Teléfono"
                perm.contains("CONTACTS") -> "Contactos"
                perm.contains("NOTIFICATION") -> "Notificaciones"
                perm.contains("STORAGE") || perm.contains("MEDIA") -> "Almacenamiento"
                else -> perm.split(".").last()
            }
        }
        
        Snackbar.make(
            binding.root,
            "Se necesitan permisos de: ${permissionNames.joinToString(", ")}",
            Snackbar.LENGTH_INDEFINITE
        ).setAction("Conceder") {
            ActivityCompat.requestPermissions(
                this,
                permissions,
                PERMISSION_REQUEST_CODE
            )
        }.show()
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        if (requestCode == PERMISSION_REQUEST_CODE) {
            val deniedPermissions = permissions.filterIndexed { index, _ ->
                grantResults[index] != PackageManager.PERMISSION_GRANTED
            }
            
            if (deniedPermissions.isEmpty()) {
                Toast.makeText(this, "Todos los permisos concedidos", Toast.LENGTH_SHORT).show()
                initializeServices()
            } else {
                Toast.makeText(
                    this,
                    "Algunos permisos fueron denegados. Funcionalidad limitada.",
                    Toast.LENGTH_LONG
                ).show()
                // Inicializar servicios con funcionalidad limitada
                initializeServices()
            }
        }
    }
    
    private fun initializeServices() {
        bluetoothService?.initialize()
        webRTCService?.initialize()
        voiceControlService?.initialize()
    }
    
    private fun setupListeners() {
        binding.apply {
            // Navegación inferior
            musicButton.setOnClickListener { navigateToMusic() }
            mapsButton.setOnClickListener { navigateToMaps() }
            phoneButton.setOnClickListener { navigateToPhone() }
            messagesButton.setOnClickListener { navigateToMessages() }
            
            // Botones de servicios
            bluetoothButton.setOnClickListener { toggleBluetooth() }
            voiceButton.setOnClickListener { toggleVoiceControl() }
            
            // Estado de servicios
            settingsButton.setOnClickListener { showSettings() }
        }
    }
    
    private fun navigateToMusic() {
        replaceFragment(MusicFragment())
        highlightNavButton(binding.musicButton)
        updateStatus("Reproduciendo música")
    }
    
    private fun navigateToMaps() {
        replaceFragment(MapsFragment())
        highlightNavButton(binding.mapsButton)
        updateStatus("Navegación activa")
    }
    
    private fun navigateToPhone() {
        replaceFragment(PhoneFragment())
        highlightNavButton(binding.phoneButton)
        updateStatus("Teléfono conectado")
    }
    
    private fun navigateToMessages() {
        replaceFragment(MessagesFragment())
        highlightNavButton(binding.messagesButton)
        updateStatus("Mensajes sincronizados")
    }
    
    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(
                android.R.anim.fade_in,
                android.R.anim.fade_out
            )
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }
    
    private fun highlightNavButton(selectedButton: View) {
        binding.apply {
            listOf(musicButton, mapsButton, phoneButton, messagesButton).forEach { button ->
                button.isSelected = (button == selectedButton)
            }
        }
    }
    
    private fun toggleBluetooth() {
        bluetoothService?.let { service ->
            if (service.isConnected()) {
                service.disconnect()
                binding.bluetoothButton.setImageResource(R.drawable.ic_bluetooth_disabled)
                updateStatus("Bluetooth desconectado")
            } else {
                service.startDiscovery()
                binding.bluetoothButton.setImageResource(R.drawable.ic_bluetooth)
                updateStatus("Buscando dispositivos...")
            }
        }
    }
    
    private fun toggleVoiceControl() {
        voiceControlService?.let { service ->
            if (service.isListening()) {
                service.stopListening()
                binding.voiceButton.setImageResource(R.drawable.ic_mic_off)
                updateStatus("Control de voz desactivado")
            } else {
                service.startListening()
                binding.voiceButton.setImageResource(R.drawable.ic_mic)
                updateStatus("Escuchando...")
            }
        }
    }
    
    private fun showSettings() {
        // Implementar pantalla de configuración
        Toast.makeText(this, "Configuración", Toast.LENGTH_SHORT).show()
    }
    
    private fun updateStatus(status: String) {
        binding.statusText.text = status
    }
    
    private fun updateTime() {
        val timeFormat = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
        val currentTime = timeFormat.format(java.util.Date())
        binding.timeText.text = currentTime
        
        // Actualizar cada minuto
        binding.timeText.postDelayed({ updateTime() }, 60000)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        bluetoothService?.destroy()
        webRTCService?.destroy()
        voiceControlService?.destroy()
    }
}
