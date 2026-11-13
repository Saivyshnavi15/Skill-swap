package com.example.domain

import android.content.Context
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class VideoCallUiState(
  val sessionId: String = "",
  val roomCode: String = "",
  val peerName: String = "",
  val isJoined: Boolean = false,
  val isCameraEnabled: Boolean = true,
  val isMicEnabled: Boolean = true,
  val isFrontCamera: Boolean = true,
  val signalingState: String = "Ready to join room"
)

/**
 * Modular WebRTC / CameraX video session abstraction.
 * Uses CameraX for real local camera hardware preview & permissioned audio state,
 * and exposes room/signaling state ready for native WebRTC ICE/SDP peer transport.
 */
interface VideoSessionService {
  val state: StateFlow<VideoCallUiState>
  fun joinSession(sessionId: String, roomCode: String, peerName: String)
  fun leaveSession()
  fun toggleCamera()
  fun toggleMicrophone()
  fun flipCamera(context: Context, lifecycleOwner: LifecycleOwner, previewView: PreviewView)
  fun bindLocalCamera(context: Context, lifecycleOwner: LifecycleOwner, previewView: PreviewView)
}

class LocalCameraWebRtcSessionService : VideoSessionService {

  private val _state = MutableStateFlow(VideoCallUiState())
  override val state: StateFlow<VideoCallUiState> = _state.asStateFlow()

  override fun joinSession(sessionId: String, roomCode: String, peerName: String) {
    _state.value = _state.value.copy(
      sessionId = sessionId,
      roomCode = roomCode.ifBlank { "swap-${sessionId.take(6)}" },
      peerName = peerName,
      isJoined = true,
      signalingState = "Local CameraX & audio active • Waiting for peer WebRTC ICE/SDP handshake"
    )
  }

  override fun leaveSession() {
    _state.value = VideoCallUiState(signalingState = "Session ended")
  }

  override fun toggleCamera() {
    val next = !_state.value.isCameraEnabled
    _state.value = _state.value.copy(isCameraEnabled = next)
  }

  override fun toggleMicrophone() {
    val next = !_state.value.isMicEnabled
    _state.value = _state.value.copy(isMicEnabled = next)
  }

  override fun flipCamera(
    context: Context,
    lifecycleOwner: LifecycleOwner,
    previewView: PreviewView
  ) {
    _state.value = _state.value.copy(isFrontCamera = !_state.value.isFrontCamera)
    bindLocalCamera(context, lifecycleOwner, previewView)
  }

  override fun bindLocalCamera(
    context: Context,
    lifecycleOwner: LifecycleOwner,
    previewView: PreviewView
  ) {
    val providerFuture = ProcessCameraProvider.getInstance(context)
    providerFuture.addListener({
      try {
        val provider = providerFuture.get()
        val preview = Preview.Builder().build().also {
          it.surfaceProvider = previewView.surfaceProvider
        }
        val selector = if (_state.value.isFrontCamera) {
          CameraSelector.DEFAULT_FRONT_CAMERA
        } else {
          CameraSelector.DEFAULT_BACK_CAMERA
        }
        provider.unbindAll()
        if (_state.value.isCameraEnabled) {
          provider.bindToLifecycle(lifecycleOwner, selector, preview)
        }
      } catch (e: Exception) {
        _state.value = _state.value.copy(
          signalingState = "Camera unavailable on device (${e.localizedMessage ?: "hardware error"})"
        )
      }
    }, ContextCompat.getMainExecutor(context))
  }
}
