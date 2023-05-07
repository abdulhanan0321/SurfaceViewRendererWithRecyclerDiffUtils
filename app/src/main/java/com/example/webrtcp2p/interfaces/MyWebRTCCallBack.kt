package com.example.webrtcp2p.interfaces

import org.webrtc.MediaStream

interface MyWebRTCCallBack {
    fun onIceGatheringState(state: String)
    fun mediaStream(stream: MediaStream?)
}