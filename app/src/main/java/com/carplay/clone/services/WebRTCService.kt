package com.carplay.clone.services

import android.content.Context
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.util.Log
import org.webrtc.*
import java.net.Socket
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter

class WebRTCService(private val context: Context) {
    
    companion object {
        private const val TAG = "WebRTCService"
        private const val SIGNALING_SERVER = "ws://localhost:8080"
        private val ICE_SERVERS = listOf(
            PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer()
        )
    }
    
    private var eglBase: EglBase? = null
    private var peerConnectionFactory: PeerConnectionFactory? = null
    private var peerConnection: PeerConnection? = null
    private var videoCapturer: VideoCapturer? = null
    private var localVideoTrack: VideoTrack? = null
    private var audioTrack: AudioTrack? = null
    private var mediaProjection: MediaProjection? = null
    private var socket: Socket? = null
    private var writer: PrintWriter? = null
    private var reader: BufferedReader? = null
    
    fun initialize() {
        initializePeerConnectionFactory()
        setupSignaling()
    }
    
    private fun initializePeerConnectionFactory() {
        val options = PeerConnectionFactory.InitializationOptions.builder(context)
            .setFieldTrials("")
            .createInitializationOptions()
        
        PeerConnectionFactory.initialize(options)
        
        eglBase = EglBase.create()
        
        val encoderFactory = DefaultVideoEncoderFactory(
            eglBase?.eglBaseContext,
            true,
            true
        )
        
        val decoderFactory = DefaultVideoDecoderFactory(eglBase?.eglBaseContext)
        
        peerConnectionFactory = PeerConnectionFactory.builder()
            .setVideoEncoderFactory(encoderFactory)
            .setVideoDecoderFactory(decoderFactory)
            .setOptions(PeerConnectionFactory.Options())
            .createPeerConnectionFactory()
    }
    
    private fun setupSignaling() {
        Thread {
            try {
                socket = Socket("10.0.2.2", 8080) // Para emulador
                writer = PrintWriter(socket?.getOutputStream(), true)
                reader = BufferedReader(InputStreamReader(socket?.getInputStream()))
                
                startReadingMessages()
            } catch (e: Exception) {
                Log.e(TAG, "Error conectando al servidor de signaling: ${e.message}")
            }
        }.start()
    }
    
    private fun startReadingMessages() {
        Thread {
            try {
                var message: String?
                while (reader?.readLine().also { message = it } != null) {
                    handleSignalingMessage(message!!)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error leyendo mensajes: ${e.message}")
            }
        }.start()
    }
    
    private fun handleSignalingMessage(message: String) {
        try {
            val json = org.json.JSONObject(message)
            when (json.getString("type")) {
                "offer" -> handleOffer(json)
                "answer" -> handleAnswer(json)
                "candidate" -> handleCandidate(json)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error procesando mensaje: ${e.message}")
        }
    }
    
    private fun handleOffer(json: org.json.JSONObject) {
        val sdp = SessionDescription(
            SessionDescription.Type.OFFER,
            json.getString("sdp")
        )
        peerConnection?.setRemoteDescription(SimpleSdpObserver(), sdp)
        createAnswer()
    }
    
    private fun handleAnswer(json: org.json.JSONObject) {
        val sdp = SessionDescription(
            SessionDescription.Type.ANSWER,
            json.getString("sdp")
        )
        peerConnection?.setRemoteDescription(SimpleSdpObserver(), sdp)
    }
    
    private fun handleCandidate(json: org.json.JSONObject) {
        val candidate = IceCandidate(
            json.getString("id"),
            json.getInt("label"),
            json.getString("candidate")
        )
        peerConnection?.addIceCandidate(candidate)
    }
    
    private fun createPeerConnection() {
        val rtcConfig = PeerConnection.RTCConfiguration(ICE_SERVERS).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
        }
        
        peerConnection = peerConnectionFactory?.createPeerConnection(
            rtcConfig,
            object : PeerConnection.Observer {
                override fun onIceCandidate(candidate: IceCandidate) {
                    sendIceCandidate(candidate)
                }
                
                override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) {}
                override fun onSignalingChange(state: PeerConnection.SignalingState?) {}
                override fun onIceConnectionChange(state: PeerConnection.IceConnectionState?) {}
                override fun onIceConnectionReceivingChange(receiving: Boolean) {}
                override fun onIceGatheringChange(state: PeerConnection.IceGatheringState?) {}
                override fun onAddStream(stream: MediaStream?) {}
                override fun onRemoveStream(stream: MediaStream?) {}
                override fun onDataChannel(channel: DataChannel?) {}
                override fun onRenegotiationNeeded() {}
                override fun onAddTrack(receiver: RtpReceiver?, streams: Array<out MediaStream>?) {}
                override fun onStandardizedIceConnectionChange(newState: PeerConnection.IceConnectionState?) {}
                override fun onConnectionChange(newState: PeerConnection.PeerConnectionState?) {}
                override fun onIceCandidateError(event: IceCandidateErrorEvent?) {}
            }
        )
        
        addLocalStream()
    }
    
    private fun addLocalStream() {
        val audioSource = peerConnectionFactory?.createAudioSource(MediaConstraints())
        audioTrack = peerConnectionFactory?.createAudioTrack("audio", audioSource)
        
        // Captura de video de la pantalla
        videoCapturer = createScreenCapturer()
        val videoSource = peerConnectionFactory?.createVideoSource(false)
        videoCapturer?.initialize(
            SurfaceTextureHelper.create("CaptureThread", eglBase?.eglBaseContext),
            context,
            videoSource?.capturerObserver
        )
        localVideoTrack = peerConnectionFactory?.createVideoTrack("video", videoSource)
        
        peerConnection?.addTrack(audioTrack)
        peerConnection?.addTrack(localVideoTrack)
    }
    
    private fun createScreenCapturer(): VideoCapturer? {
        val mediaProjectionManager = context.getSystemService(
            Context.MEDIA_PROJECTION_SERVICE
        ) as MediaProjectionManager
        
        // Aquí necesitarías un Intent para iniciar la captura de pantalla
        return ScreenCapturerAndroid(
            DataChannel.Init().apply { ordered = true },
            object : ScreenCapturerAndroid.CapturerObserver {
                override fun onCapturerStarted(success: Boolean) {
                    Log.d(TAG, "Captura iniciada: $success")
                }
                
                override fun onCapturerStopped() {
                    Log.d(TAG, "Captura detenida")
                }
            }
        )
    }
    
    private fun createOffer() {
        peerConnection?.createOffer(object : SdpObserver by SimpleSdpObserver() {
            override fun onCreateSuccess(sessionDescription: SessionDescription) {
                peerConnection?.setLocalDescription(SimpleSdpObserver(), sessionDescription)
                sendSDP(sessionDescription)
            }
        }, MediaConstraints())
    }
    
    private fun createAnswer() {
        peerConnection?.createAnswer(object : SdpObserver by SimpleSdpObserver() {
            override fun onCreateSuccess(sessionDescription: SessionDescription) {
                peerConnection?.setLocalDescription(SimpleSdpObserver(), sessionDescription)
                sendSDP(sessionDescription)
            }
        }, MediaConstraints())
    }
    
    private fun sendSDP(sdp: SessionDescription) {
        try {
            val message = org.json.JSONObject().apply {
                put("type", sdp.type.name.lowercase())
                put("sdp", sdp.description)
            }
            writer?.println(message.toString())
        } catch (e: Exception) {
            Log.e(TAG, "Error enviando SDP: ${e.message}")
        }
    }
    
    private fun sendIceCandidate(candidate: IceCandidate) {
        try {
            val message = org.json.JSONObject().apply {
                put("type", "candidate")
                put("id", candidate.sdpMid)
                put("label", candidate.sdpMLineIndex)
                put("candidate", candidate.sdp)
            }
            writer?.println(message.toString())
        } catch (e: Exception) {
            Log.e(TAG, "Error enviando ICE candidate: ${e.message}")
        }
    }
    
    fun destroy() {
        localVideoTrack?.dispose()
        audioTrack?.dispose()
        videoCapturer?.dispose()
        peerConnection?.close()
        peerConnectionFactory?.dispose()
        eglBase?.release()
        socket?.close()
    }
}

class SimpleSdpObserver : SdpObserver {
    override fun onCreateSuccess(sessionDescription: SessionDescription?) {}
    override fun onSetSuccess() {}
    override fun onCreateFailure(error: String?) {}
    override fun onSetFailure(error: String?) {}
}
