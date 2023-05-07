package com.example.webrtcp2p

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.SurfaceView
import android.widget.EditText
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role.Companion.Image
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.webrtcp2p.databinding.ActivityMainBinding
import com.example.webrtcp2p.interfaces.MainScreenInterface
import com.example.webrtcp2p.screenViews.MainView
import com.google.firebase.annotations.concurrent.Background


class MainActivity : AppCompatActivity(), MainScreenInterface {
    private lateinit var binding: ActivityMainBinding
    private var meetingID = ""
    private var username = ""
    private var callingToUsername = ""
    private val participantAdapter by lazy { participantAdapter() }
    private lateinit var list: MutableList<VideoTrackModel>
    val PERMISSIONS = arrayOf<String>(
        android.Manifest.permission.CAMERA
    )

    val multiplePermission = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()){ isGranted ->
        if (isGranted.containsValue(false)) {
            Toast.makeText(this, "permission denied", Toast.LENGTH_LONG).show()
        }else {
            validateAndCall(meetingID, username, callingToUsername)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        binding = ActivityMainBinding.inflate(layoutInflater)
//        setContentView(binding.root)
//
//        binding.recycler.layoutManager = LinearLayoutManager(this)
//        binding.recycler.adapter = participantAdapter
//
//        list = mutableListOf()
//        for (u in 0 until 4){
//            val model = VideoTrackModel()
//            model.id = u.toString()
//            when(u){
//                0 -> {model.name = "Spider Man"}
//                1 -> {model.name = "Iron Man"}
//                2 -> {model.name = "Super Man"}
//                3 -> {model.name = "Ant Man"}
//            }
//
//            list.add(model)
//        }
//
//        participantAdapter.updateListItems(list)
//
//        binding.changeBtn.setOnClickListener {
////            list[1].apply {
////                this.name = "Abdul Hanan"
////            }
//            participantAdapter.updateListItems(list)
//        }

        setContent{
            MainView(this)
        }

    }

    private fun validateAndCall(meetingID: String, username: String,
                                callingToUsername: String){

        if (meetingID.isEmpty() || username.isEmpty() || callingToUsername.isEmpty()){
            return
        }

        val intent = Intent(this, CallActivity::class.java)
        intent.putExtra("meetingID", meetingID)
        intent.putExtra("username", username)
        intent.putExtra("callingToUsername", callingToUsername)
        startActivity(intent)

    }

    override fun onCallClick(meetingId: String, userName: String, callingToUserName: String) {
        this.meetingID = meetingId
        this.username = userName
        this.callingToUsername = callingToUserName
        if (ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            multiplePermission.launch(PERMISSIONS)
        }else {
            validateAndCall(meetingID, username, callingToUsername)
        }
    }
}