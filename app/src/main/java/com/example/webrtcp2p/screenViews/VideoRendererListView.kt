package com.example.webrtcp2p.screenViews

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role.Companion.Button
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.MutableLiveData
import com.example.webrtcp2p.EglBaseClass
import com.example.webrtcp2p.R
import com.example.webrtcp2p.VideoTrackModel
import com.example.webrtcp2p.interfaces.MainScreenInterface
import org.webrtc.RendererCommon
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoTrack

@Composable
fun VideoRendererListView(peers: MutableList<VideoTrackModel>){
    val list = remember {  mutableStateListOf<VideoTrackModel>() }
    list.addAll(peers)
    Column {
        LazyVerticalGrid(columns = GridCells.Fixed(2),
        content = {
            items(items = list,
                key = { snack: VideoTrackModel ->
                    snack.name!!
                }) {
                PeerVideoComposable(it)
            }
        })
        Button(
            onClick = {
                if (list.size > 0) {
                    list.removeLast()
                }
//                list.add(peers[1])
            },
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(text = "Add")
        }
    }

}

@Composable
fun PeerVideoComposable(peer: VideoTrackModel) {

    val composeableId by remember { mutableStateOf(Math.random().toString()) }

    var previousActivePeer by remember { mutableStateOf(peer) }
    var previousVideoTrack by remember { mutableStateOf<VideoTrack?>(null) }

    Box {
        AndroidView(
            factory = { context ->
                SurfaceViewRenderer(context).apply {
                    setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT)
                    setEnableHardwareScaler(true)
                }
            },
            update = {
                Log.d("VideoList","ContainerId: $composeableId, previous: ${previousActivePeer.id}/${previousActivePeer.name}, new: ${peer.id}/${peer.name}")
                // Peer changed, tile is rebound to a new peer.
                if (previousActivePeer.id != peer.id) {
                    if (previousVideoTrack != null) {
                        Log.d("VideoList","ContainerId: $composeableId Releasing video, removing sink")
                        previousVideoTrack?.removeSink(it)
                        it.release()
                    } else {
                        Log.d("VideoList","ContainerId: $composeableId, not releasing video since it was never enabled")
                    }

                    previousActivePeer = peer
                }

                if (peer.videoTrack == null) {
                    Log.d("VideoList","Peer ${peer.id} name:${peer.name} had no video")
                } else if (previousVideoTrack == null) {
                    it.init(EglBaseClass.rootEglBase?.eglBaseContext, null)
                    Log.d("VideoList","ContainerId: $composeableId, peer ${peer.name} had video, adding")
                    peer.videoTrack?.addSink(it)
                    previousVideoTrack = peer.videoTrack
                }

            }
        )
        Text(peer.name.toString(), modifier = Modifier
                .background(Color(0x80CCCCCC))
                .padding(4.dp)
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
            textAlign = TextAlign.Center
        )
    }

}