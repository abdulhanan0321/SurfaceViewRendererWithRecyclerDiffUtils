package com.example.webrtcp2p

import org.webrtc.VideoTrack

class VideoTrackModel {

    var id: String? = null
    var name: String? = null
    var videoTrack: VideoTrack? = null
    var isTrackEnabled = false
    var isMute = false
}