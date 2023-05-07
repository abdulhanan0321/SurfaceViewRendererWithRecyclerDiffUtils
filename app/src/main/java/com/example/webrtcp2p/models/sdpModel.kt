package com.example.webrtcp2p.models

class sdpModel {

    var sdp: String? = null
    var type: String? = null

    constructor()

    constructor(sdp: String?, type: String?) {
        this.sdp = sdp
        this.type = type
    }
}