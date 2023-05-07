package com.example.webrtcp2p.models

class IceCandidateModel {


    var type: String? = null
    var sdpMLineIndex = 0
    var sdpMid: String? = null
    var candidate: String? = null

    constructor()

    constructor(type: String?, sdpMLineIndex: Int, sdpMid: String?, candidate: String?) {
        this.type = type
        this.sdpMLineIndex = sdpMLineIndex
        this.sdpMid = sdpMid
        this.candidate = candidate
    }
}