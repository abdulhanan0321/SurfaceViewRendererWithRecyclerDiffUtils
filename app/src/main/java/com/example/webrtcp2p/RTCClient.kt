package com.example.webrtcp2p

import android.content.Context
import android.util.Log
import com.example.webrtcp2p.interfaces.MyWebRTCCallBack
import com.example.webrtcp2p.models.IceCandidateModel
import com.example.webrtcp2p.models.User
import com.example.webrtcp2p.models.sdpModel
import com.example.webrtcp2p.utils.Contant
import com.google.firebase.database.*
import org.webrtc.*
import org.webrtc.PeerConnection.*
import java.util.*

class RTCClient(context: Context, meetingId: String, username: String,
                callingTo: String, myWebRTCCallBack: MyWebRTCCallBack
): PeerConnection.Observer,
    SdpObserver, DataChannel.Observer, ChildEventListener {
    private val TAG = "Connect_webRTC"

    private var context: Context? = null
    private var myWebRTCCallBack: MyWebRTCCallBack? = null
    private var dataChannel: DataChannel? = null
    private var constraints: MediaConstraints? = null
    private var peerConnection: PeerConnection? = null
    private var databaseReference: DatabaseReference? = null
    private val rootEglBase: EglBase = EglBase.create()
    private var localAudioTrack : AudioTrack? = null
    private var localVideoTrack : VideoTrack? = null
    private var isIceGatheringComplete = false
    private var behaviour = ""

    private val iceServer = listOf(
        PeerConnection.IceServer.builder("stun:stun.l.google.com:19302")
            .createIceServer()
    )

    private val peerConnectionFactory by lazy { buildPeerConnectionFactory() }
    private val videoCapturer by lazy { getVideoCapturer(context) }

    private val audioSource by lazy { peerConnectionFactory.createAudioSource(MediaConstraints())}
    private val localVideoSource by lazy { peerConnectionFactory.createVideoSource(false) }

//    private val audio: Audio? = null
//    private val byteQueue: Queue<ByteArray>? = null
    private var meetingId: String? = null
    private var username = ""
    private var callingTo = ""

    init {
        this.context = context
        this.myWebRTCCallBack = myWebRTCCallBack
        this.meetingId = meetingId
        this.username = username
        this.callingTo = callingTo

        initPeerConnectionFactory()

        constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("DtlsSrtpKeyAgreement", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
        }
        databaseReference = FirebaseDatabase.getInstance().reference.child(meetingId)

        InitializePeerConnection()

    }

    private fun initPeerConnectionFactory() {
        val options = PeerConnectionFactory.InitializationOptions.builder(context)
            .setEnableInternalTracer(true)
            .setFieldTrials("WebRTC-H264HighProfile/Enabled/")
            .createInitializationOptions()
        PeerConnectionFactory.initialize(options)
    }

    private fun buildPeerConnectionFactory(): PeerConnectionFactory {
        return PeerConnectionFactory
            .builder()
            .setVideoDecoderFactory(DefaultVideoDecoderFactory(EglBaseClass.rootEglBase!!.eglBaseContext))
            .setVideoEncoderFactory(DefaultVideoEncoderFactory(EglBaseClass.rootEglBase!!.eglBaseContext, true, true))
            .setOptions(PeerConnectionFactory.Options())
            .createPeerConnectionFactory()
    }

    private fun getVideoCapturer(context: Context) =
        Camera2Enumerator(context).run {
            deviceNames.find {
                isFrontFacing(it)
            }?.let {
                createCapturer(it, null)
            } ?: throw IllegalStateException()
        }

    fun initSurfaceView(view: SurfaceViewRenderer) = view.run {
        setMirror(true)
        setEnableHardwareScaler(true)
        init(EglBaseClass.rootEglBase!!.eglBaseContext, null)
    }

    fun startLocalVideoCapture(localVideoOutput: SurfaceViewRenderer, myWebRTCCallBack: MyWebRTCCallBack) {
        val surfaceTextureHelper = SurfaceTextureHelper.create(Thread.currentThread().name,EglBaseClass.rootEglBase!!.eglBaseContext)
        (videoCapturer as VideoCapturer).initialize(surfaceTextureHelper, localVideoOutput.context, localVideoSource.capturerObserver)
        videoCapturer.startCapture(320, 240, 32)
        localAudioTrack = peerConnectionFactory.createAudioTrack(Contant.LOCAL_TRACK_ID + "_audio", audioSource)
        localVideoTrack = peerConnectionFactory.createVideoTrack(Contant.LOCAL_TRACK_ID, localVideoSource)
        localVideoTrack?.addSink(localVideoOutput)
        val localStream = peerConnectionFactory.createLocalMediaStream(Contant.LOCAL_STREAM_ID)
        localStream.addTrack(localVideoTrack)
        localStream.addTrack(localAudioTrack)
        peerConnection?.addStream(localStream)

        myWebRTCCallBack.mediaStream(localStream)
    }

    fun startCamera(){
        videoCapturer.startCapture(320, 240, 32)
    }
    fun stopCamera(){
        videoCapturer.stopCapture()
    }


    fun destroyPeerConnection(){
        videoCapturer.stopCapture()
        peerConnection?.dispose()
    }

    fun createOffer() {
        peerConnection!!.createOffer(this@RTCClient, constraints)
    }

    fun createAnswer(){
        peerConnection!!.createAnswer(this@RTCClient, constraints)
    }

    fun setupListeners() {
//        databaseReference!!.child(callingToRoot).addValueEventListener(candidateDataListener)
        databaseReference!!.child(Contant.user).addChildEventListener(this@RTCClient)
    }

    var candidateDataListener: ValueEventListener = object : ValueEventListener {
        override fun onDataChange(dataSnapshot: DataSnapshot) {
            val dataModel: sdpModel? = dataSnapshot.child(Contant.sdp).getValue(sdpModel::class.java)
            if (dataModel != null) {
                Log.d(TAG," sdp found")
                val sdp2 = SessionDescription(
                    SessionDescription.Type.fromCanonicalForm(dataModel.type),
                    dataModel.sdp)
                peerConnection!!.setRemoteDescription(this@RTCClient, sdp2)

//                if (!createdOffer)
                createAnswer()
                for (postSnapshot in dataSnapshot.child(Contant.iceCandidate).children) {
                    val iceCandidateModel: IceCandidateModel? = postSnapshot.getValue(IceCandidateModel::class.java)
                    val iceCandidate1 = IceCandidate(
                        iceCandidateModel!!.sdpMid,
                        iceCandidateModel.sdpMLineIndex,
                        iceCandidateModel.candidate)
                    peerConnection!!.addIceCandidate(iceCandidate1)
                    Log.e(TAG, "ice added")
                }
            } else {
                Log.e(TAG, "DATA MODEL IS NULL")
            }
        }

        override fun onCancelled(databaseError: DatabaseError) {}
    }

    fun InitializePeerConnection() {

        peerConnection = peerConnectionFactory.createPeerConnection(iceServer, this)
        val init = DataChannel.Init()
        init.ordered = true
        dataChannel = peerConnection!!.createDataChannel("RTCDataChannel", init)
        dataChannel?.registerObserver(this@RTCClient)
        setupListeners()
//        if (createdOffer) {
//            createOffer()
//        }
    }


    /**
     * PeerConnection Callbacks
     * */
    override fun onSignalingChange(signalingState: SignalingState?) {
        Log.d(TAG, "onSignalingChange() " + signalingState?.name)
    }

    override fun onIceConnectionChange(iceConnectionState: IceConnectionState?) {
        Log.d(TAG, "onIceConnectionChange() " + iceConnectionState?.name)
    }

    override fun onIceConnectionReceivingChange(p0: Boolean) {
        Log.d(TAG, "onIceConnectionReceivingChange(): $p0")
    }

    override fun onIceGatheringChange(iceGatheringState: IceGatheringState?) {
        Log.d(TAG, "onIceGatheringChange() " + iceGatheringState?.name)
        if(iceGatheringState?.name.toString() == "COMPLETE"){

            val sdpModel = sdpModel(
                peerConnection!!.localDescription.description,
                peerConnection!!.localDescription.type.toString().lowercase(
                    Locale.getDefault()
                )
            )

            Log.d(TAG, "SDP ${sdpModel.sdp}")
            databaseReference!!.child(Contant.peer_connection)
                .child(username).child(Contant.sdp)
                .setValue(sdpModel)

                    myWebRTCCallBack?.onIceGatheringState(iceGatheringState?.name.toString())
        }
//        myWebRTCCallBack?.onIceGatheringState(iceGatheringState?.name.toString())
    }

    override fun onIceCandidate(iceCandidate: IceCandidate?) {
        Log.d(TAG, "onIceCandidate: $iceCandidate")
        val iceCandidateModel = IceCandidateModel(
            "candidate", iceCandidate!!.sdpMLineIndex,
            iceCandidate.sdpMid, iceCandidate.sdp)
        // Do Some Signaling stuff to share IceCandidate Model with other Steve or Bill
        databaseReference!!.child(Contant.peer_connection)
            .child(username).child(Contant.iceCandidate).push()
            .setValue(iceCandidateModel)
    }

    override fun onIceCandidatesRemoved(p0: Array<out IceCandidate>?) {
        TODO("Not yet implemented")
    }

    override fun onAddStream(p0: MediaStream?) {
        myWebRTCCallBack?.mediaStream(p0)
    }

    override fun onRemoveStream(p0: MediaStream?) {}

    override fun onDataChannel(dataChannel1: DataChannel?) {
        dataChannel = dataChannel1
        dataChannel?.registerObserver(this@RTCClient)
    }

    override fun onRenegotiationNeeded() {
        Log.d(TAG, "onRenegotiationNeeded()")
    }

    override fun onAddTrack(p0: RtpReceiver?, p1: Array<out MediaStream>?) {
    }



    /**
     * Session Description Callbacks
     * */
    override fun onCreateSuccess(sessionDescription: SessionDescription?) {
        peerConnection!!.setLocalDescription(this, sessionDescription)
//        val sdpModel = sdpModel(
//            sessionDescription?.description, sessionDescription?.type.toString().lowercase(
//                Locale.getDefault()
//            )
//        )

//            Log.d(TAG, "SDP ${sdpModel.sdp}")
//            databaseReference!!.child(Contant.peer_connection)
//                .child(username).child(Contant.sdp)
//                .setValue(sdpModel)
    }

    override fun onSetSuccess() {
    }

    override fun onCreateFailure(p0: String?) {
    }

    override fun onSetFailure(p0: String?) {
    }


    /**
     * DataChannel Callbacks
     * */
    override fun onBufferedAmountChange(p0: Long) {
    }

    override fun onStateChange() {
    }

    override fun onMessage(p0: DataChannel.Buffer?) {
        //            try {
//                if (!buffer.binary) {
//                    val limit = buffer.data.limit()
//                    val data = ByteArray(limit)
//                    buffer.data[data]
//                    byteQueue.add(data)
//                } else {
//                    Log.e(
//                        TAG,
//                        "Data is received but not binary."
//                    )
//                }
//            } catch (e: Exception) {
//                e.printStackTrace()
//                Log.e(TAG, "ERROR: $e")
//            }
    }


    /**
     * firebase child added listener
     * */
    override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
        val userModel: User? = snapshot.getValue(User::class.java)
        if (userModel?.name != username){
            val behaviourKey = username +"_"+callingTo
            Log.d(TAG,"$behaviourKey")
            databaseReference!!.child(Contant.behaviour).child(behaviourKey).addListenerForSingleValueEvent(
                object : ValueEventListener{
                    override fun onDataChange(snapshot: DataSnapshot) {
                        behaviour = snapshot.getValue(String::class.java).toString()
                        databaseReference!!.child(Contant.peer_connection)
                            .child(callingTo).addValueEventListener(candidateDataListener)
                        when(behaviour){
                            "Offer" -> Log.d(TAG,"Answer")
                            "Answer" -> Log.d(TAG,"Answer")
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {}
                }
            )
        }

    }

    override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {

    }

    override fun onChildRemoved(snapshot: DataSnapshot) {

    }

    override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {

    }

    override fun onCancelled(error: DatabaseError) {

    }
}