package com.example.webrtcp2p

import org.webrtc.EglBase

object EglBaseClass {

    var rootEglBase: EglBase? = null
    init {
        rootEglBase = EglBase.create()
    }
}