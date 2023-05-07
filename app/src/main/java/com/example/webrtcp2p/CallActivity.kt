package com.example.webrtcp2p

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.compose.runtime.State
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.webrtcp2p.databinding.ActivityCallBinding
import com.example.webrtcp2p.interfaces.MyWebRTCCallBack
import com.example.webrtcp2p.models.User
import com.example.webrtcp2p.screenViews.VideoRendererListView
import com.example.webrtcp2p.utils.Contant
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.webrtc.MediaStream
import java.util.concurrent.CopyOnWriteArrayList

class CallActivity : AppCompatActivity(), MyWebRTCCallBack {
    private lateinit var binding: ActivityCallBinding
    private var meetingID = ""
    private var username = ""
    private var callingToUsername = ""
    private val rtcClient by lazy { createRtcClient() }
    private val participantAdapter by lazy { participantAdapter() }
    private var list: CopyOnWriteArrayList<VideoTrackModel> = CopyOnWriteArrayList()
    private var databaseReference: DatabaseReference? = null
    private var isCameraEnabled = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCallBinding.inflate(layoutInflater)
        setContentView(binding.root)

        init()

        binding.invite.setOnClickListener {
//            databaseReference!!.child(Contant.behaviour).child(username+"_"+callingToUsername).setValue("Offer")
//            databaseReference!!.child(Contant.behaviour).child(callingToUsername+"_"+username).setValue("Answer")
//            rtcClient.createOffer()

            if (list.isNotEmpty()) {

                list[1].apply {
                    this.isMute = !this.isMute
//                    this.isTrackEnabled = !this.isTrackEnabled
                }

                mapListAndSubmit()
            }

//            binding.composeView.setContent {
//                VideoRendererListView(statList)
//            }
        }

        binding.camera.setOnClickListener {
//            if (isCameraEnabled) {
//                isCameraEnabled = false
//                rtcClient.stopCamera()
//            }else {
//                isCameraEnabled = true
//                rtcClient.startCamera()
//            }

//            val model = VideoTrackModel()
//            model.id = "4"
//            model.videoTrack = list[0].videoTrack
//            model.isTrackEnabled = true
//            model.name = "Batman"
//
//            list.add(model)
            list.removeAt(3)
//            participantAdapter.removeItem(3)
            mapListAndSubmit()
        }

        binding.addBtn.setOnClickListener {
//            if (isCameraEnabled) {
//                isCameraEnabled = false
//                rtcClient.stopCamera()
//            }else {
//                isCameraEnabled = true
//                rtcClient.startCamera()
//            }

            val model = VideoTrackModel()
            model.id = "3"
            model.videoTrack = list[0].videoTrack
            model.isTrackEnabled = true
            model.name = "Batman"

            list.add(model)

            mapListAndSubmit()
        }
    }

    fun mapListAndSubmit(){
        val list3 = list.map {
            VideoTrackModel().apply {
                id = it.id
                name = it.name
                isMute = it.isMute
                videoTrack = it.videoTrack
                isTrackEnabled = it.isTrackEnabled
            }
        }
                        participantAdapter.updateListItems(list3)
//        participantAdapter.submitList(list3)
    }

    override fun onDestroy() {
        super.onDestroy()
        rtcClient.destroyPeerConnection()
        binding.recycler.adapter = null
    }

    private fun init(){
        intent.extras?.let {
            meetingID = it.getString("meetingID","")
            username = it.getString("username","")
            callingToUsername = it.getString("callingToUsername","")
        }

        databaseReference = FirebaseDatabase.getInstance().reference.child(meetingID)

        binding.recycler.layoutManager = GridLayoutManager(this, 2)
        binding.recycler.itemAnimator = null
        binding.recycler.adapter = participantAdapter

        rtcClient.initSurfaceView(binding.localView)
        rtcClient.initSurfaceView(binding.remoteView)
        rtcClient.startLocalVideoCapture(binding.localView, object : MyWebRTCCallBack{
            override fun mediaStream(stream: MediaStream?) {
                val myList = ArrayList<VideoTrackModel>()
                for (u in 0 until 4){
                    val model = VideoTrackModel()
                    model.id = u.toString()
                    model.videoTrack = stream!!.videoTracks[0]
                    model.isTrackEnabled = true
                    when(u){
                        0 -> {model.name = "Spider Man"}
                        1 -> {model.name = "Iron Man"}
                        2 -> {model.name = "Super Man"}
                        3 -> {model.name = "Ant Man"}
                    }

                    myList.add(model)
                }
                list.addAll(myList)
                participantAdapter.updateListItems(myList)
//                participantAdapter.submitList(list)
            }

            override fun onIceGatheringState(state: String) {
            }
        })
//        binding.composeView.setContent {
//            VideoRendererListView(list)
//        }
        binding.remoteView.setZOrderMediaOverlay(false)
        binding.localView.setZOrderMediaOverlay(true)

    }

    private fun createRtcClient() = RTCClient(this,meetingID, username, callingToUsername, this)

    override fun onIceGatheringState(state: String) {
        when(state){
            "COMPLETE" -> {

                val user = User()
                user.name = username
                databaseReference!!.child(Contant.user).child(username).setValue(user)
            }
        }
    }

    override fun mediaStream(stream: MediaStream?) {
        if (stream!!.videoTracks.size > 0){
            stream.videoTracks[0].addSink(binding.remoteView)
        }
    }
}