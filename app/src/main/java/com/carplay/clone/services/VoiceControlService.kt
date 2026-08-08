package com.carplay.clone.services

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import android.widget.Toast

class VoiceControlService(private val context: Context) {
    
    private var speechRecognizer: SpeechRecognizer? = null
    private var isActive = false
    private val audioManager: AudioManager = 
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    
    companion object {
        private const val TAG = "VoiceControlService"
    }
    
    var onResult: ((String) -> Unit)? = null
    var onError: ((String) -> Unit)? = null
    
    fun initialize() {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
            setupRecognitionListener()
        }
    }
    
    private fun setupRecognitionListener() {
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                Log.d(TAG, "Listo para reconocimiento de voz")
                muteAudio(true)
            }
            
            override fun onBeginningOfSpeech() {
                Log.d(TAG, "Inicio de voz detectado")
            }
            
            override fun onRmsChanged(rmsdB: Float) {
                // Nivel de sonido: podemos usarlo para animaciones
                Log.d(TAG, "RMS: $rmsdB")
            }
            
            override fun onBufferReceived(buffer: ByteArray?) {}
            
            override fun onEndOfSpeech() {
                Log.d(TAG, "Fin de voz detectado")
                muteAudio(false)
            }
            
            override fun onError(error: Int) {
                val errorMessage = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "Error de audio"
                    SpeechRecognizer.ERROR_CLIENT -> "Error del cliente"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Permisos insuficientes"
                    SpeechRecognizer.ERROR_NETWORK -> "Error de red"
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Timeout de red"
                    SpeechRecognizer.ERROR_NO_MATCH -> "No se encontró coincidencia"
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Reconocedor ocupado"
                    SpeechRecognizer.ERROR_SERVER -> "Error del servidor"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Timeout de voz"
                    else -> "Error desconocido"
                }
                Log.e(TAG, "Error: $errorMessage")
                onError?.invoke(errorMessage)
                muteAudio(false)
            }
            
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val spokenText = matches?.get(0) ?: ""
                Log.d(TAG, "Resultado: $spokenText")
                onResult?.invoke(spokenText)
                processCommand(spokenText)
                muteAudio(false)
            }
            
            override fun onPartialResults(partialResults: Bundle?) {
                val partialMatches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                partialMatches?.get(0)?.let {
                    Log.d(TAG, "Parcial: $it")
                }
            }
            
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }
    
    fun startListening() {
        if (!isActive) {
            isActive = true
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-ES")
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "es-ES")
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, false)
                }
            }
            
            speechRecognizer?.startListening(intent)
            Toast.makeText(context, "Escuchando...", Toast.LENGTH_SHORT).show()
        }
    }
    
    fun stopListening() {
        if (isActive) {
            isActive = false
            speechRecognizer?.stopListening()
            muteAudio(false)
        }
    }
    
    fun isListening(): Boolean = isActive
    
    private fun processCommand(command: String) {
        when {
            command.contains("navegar", ignoreCase = true) -> {
                val destination = command.replace("navegar a ", "", ignoreCase = true)
                Toast.makeText(context, "Navegando a $destination", Toast.LENGTH_SHORT).show()
            }
            command.contains("llamar", ignoreCase = true) -> {
                val contact = command.replace("llamar a ", "", ignoreCase = true)
                Toast.makeText(context, "Llamando a $contact", Toast.LENGTH_SHORT).show()
            }
            command.contains("música", ignoreCase = true) -> {
                Toast.makeText(context, "Reproduciendo música", Toast.LENGTH_SHORT).show()
            }
            command.contains("mensaje", ignoreCase = true) -> {
                Toast.makeText(context, "Enviando mensaje", Toast.LENGTH_SHORT).show()
            }
            else -> {
                Toast.makeText(context, "Comando no reconocido", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun muteAudio(mute: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            audioManager.adjustStreamVolume(
                AudioManager.STREAM_MUSIC,
                if (mute) AudioManager.ADJUST_MUTE else AudioManager.ADJUST_UNMUTE,
                0
            )
        }
    }
    
    fun destroy() {
        stopListening()
        speechRecognizer?.destroy()
        speechRecognizer = null
    }
}
