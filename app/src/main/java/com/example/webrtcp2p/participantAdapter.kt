package com.example.webrtcp2p

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.ListUpdateCallback
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.webrtc.EglRenderer
import org.webrtc.RendererCommon
import org.webrtc.SurfaceViewRenderer

class participantAdapter : ListAdapter<VideoTrackModel, PeerViewHolder>(DIFFUTIL_CALLBACK) {

    companion object {
        val DIFFUTIL_CALLBACK = object : DiffUtil.ItemCallback<VideoTrackModel>() {
            override fun areItemsTheSame(oldItem: VideoTrackModel,
                newItem: VideoTrackModel
            ) = oldItem.id == newItem.id

            @SuppressLint("DiffUtilEquals")
            override fun areContentsTheSame(oldItem: VideoTrackModel,
                                            newItem: VideoTrackModel) = oldItem == newItem

            override fun getChangePayload(
                oldItem: VideoTrackModel,
                newItem: VideoTrackModel
            ): Any? {
                if (oldItem.isMute != newItem.isMute) {
                    Log.d("participantAdapter","isMute not equals at")
                    return PeerUpdatePayloads.SpeakerMuteUnmute(newItem.isMute)
                }else if (!oldItem.name.equals(newItem.name)){
                    return PeerUpdatePayloads.NameChanged(newItem.name!!)
                }else if (oldItem.isTrackEnabled != newItem.isTrackEnabled){
                    return PeerUpdatePayloads.VideoEnabled(newItem.isTrackEnabled)
                }else if (oldItem.isDisconnected != newItem.isDisconnected){
                    return PeerUpdatePayloads.IsDisconnected(newItem.isDisconnected)
                }

                return null
            }

        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PeerViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.participant_item_view, parent, false)
        return PeerViewHolder(view, ::getItem)
    }

    override fun onBindViewHolder(holder: PeerViewHolder, position: Int) {
        getItem(position)?.let {
            holder.stopSurfaceView()
            holder.bind(it)
            holder.startSurfaceView()
        }
    }

    override fun onBindViewHolder(
        holder: PeerViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        Log.d("participantAdapter","payload size ${payloads.size}")
        if (payloads.isEmpty()) {
            return super.onBindViewHolder(holder, position, payloads)
        }else if (payloads.all { it is PeerUpdatePayloads }) {
            payloads.forEach { payload ->
                if (payload is PeerUpdatePayloads) {
                    when (payload){
                        is PeerUpdatePayloads.NameChanged -> {}
                        is PeerUpdatePayloads.SpeakerMuteUnmute -> {
                            if (payload.isMute){
                                holder.micMute()
                            }else {
                                holder.micUnMute()
                            }
                        }
                        is PeerUpdatePayloads.VideoEnabled -> {
                            if (payload.isVideoEnabled) {
                                holder.showSurfaceView()
                            }else {
                                holder.hideSurfaceView()
                            }
                        }
                        is PeerUpdatePayloads.IsDisconnected -> {
                            Log.d("participantAdapter", "${payload.isDisconnected}")
                            if (payload.isDisconnected){
                                holder.showLastFrame()
                            }else {

                            }
                        }
                    }
                }
            }
        }else {
            getItem(position)?.let {
                holder.stopSurfaceView()
                holder.bind(it)
            }
        }
    }

    fun removeItem(position: Int) {
        val currentList = currentList.toMutableList()
        currentList.removeAt(position)
        submitList(currentList)
    }

    override fun onViewAttachedToWindow(holder: PeerViewHolder) {
        super.onViewAttachedToWindow(holder)
        holder.startSurfaceView()
    }

    override fun onViewDetachedFromWindow(holder: PeerViewHolder) {
        super.onViewDetachedFromWindow(holder)
        holder.stopSurfaceView()
    }

    sealed class PeerUpdatePayloads {
        data class NameChanged(val name: String) : PeerUpdatePayloads()
        data class SpeakerMuteUnmute(val isMute: Boolean) : PeerUpdatePayloads()
        data class VideoEnabled(val isVideoEnabled: Boolean) : PeerUpdatePayloads()
        data class IsDisconnected(val isDisconnected: Boolean) : PeerUpdatePayloads()
    }

}

class PeerViewHolder(view: View, private val getItem: (Int) -> VideoTrackModel) :
    RecyclerView.ViewHolder(view) {
    private val TAG = PeerViewHolder::class.java.simpleName
    private var sinkAdded = false

    init {
        itemView.findViewById<SurfaceViewRenderer>(R.id.remoteView).apply {
            setEnableHardwareScaler(true)
            setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT)
        }
    }

    fun startSurfaceView() {
        if (!sinkAdded) {
            itemView.findViewById<SurfaceViewRenderer>(R.id.remoteView).apply {

                getItem(adapterPosition).videoTrack?.let { hmsVideoTrack ->
                    init(EglBaseClass.rootEglBase?.eglBaseContext, null)
                    hmsVideoTrack.addSink(this)
                    sinkAdded = true
                }
            }
        }
    }

    @SuppressLint("LongLogTag")
    fun stopSurfaceView() {
        // If the sink was added, AND there was a video
        //  then remove the sink and release
        itemView.findViewById<SurfaceViewRenderer>(R.id.remoteView).apply {

            if (sinkAdded && adapterPosition != -1) {
                getItem(adapterPosition).videoTrack?.let {
                    it.removeSink(this)
                    release()
                    sinkAdded = false
                }
            }
        }
    }

    fun bind(trackPeerMap: VideoTrackModel) {

        if (!sinkAdded) {
            itemView.findViewById<SurfaceViewRenderer>(R.id.remoteView).apply {
                setEnableHardwareScaler(true)
                setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT)
                sinkAdded = false
            }
        }
        itemView.findViewById<TextView>(R.id.name).text = trackPeerMap.name

//        if (trackPeerMap.isMute){
//            micMute()
//        }else {
//            micUnMute()
//        }
//
//        if (trackPeerMap.isTrackEnabled){
//            showSurfaceView()
//        }else {
//            hideSurfaceView()
//        }
    }

    fun micMute(){
        itemView.findViewById<ImageView>(R.id.mic).apply {
            setImageResource(R.drawable.mic_mute)
        }
    }

    fun micUnMute(){
        itemView.findViewById<ImageView>(R.id.mic).apply {
            setImageResource(R.drawable.mic_unmute)
        }
    }

    fun showSurfaceView(){
        itemView.findViewById<SurfaceViewRenderer>(R.id.remoteView).apply {
            visibility = View.VISIBLE
        }
    }

    fun hideSurfaceView(){
        itemView.findViewById<SurfaceViewRenderer>(R.id.remoteView).apply {
            visibility = View.GONE
        }
    }

    val frameListener = object : EglRenderer.FrameListener {
        override fun onFrame(p0: Bitmap?) {
            Log.d("participantAdapter", "bitmap ")
            CoroutineScope(Dispatchers.Main).launch{
                itemView.findViewById<ImageView>(R.id.lastFrameImage).apply {
                    setImageBitmap(p0)
                }
            }
        }
    }


    fun showLastFrame(){

        itemView.findViewById<SurfaceViewRenderer>(R.id.remoteView).apply {
            this.addFrameListener(frameListener, 1f)
        }

        Handler().postDelayed({
            frameListener.let { listener ->
                itemView.findViewById<SurfaceViewRenderer>(R.id.remoteView).apply {
                    removeFrameListener(listener)
                }
            }
        }, 1000)

    }

}

//
//class participantAdapter(): RecyclerView.Adapter<participantAdapter.ParticipantHolder>() {
//
////    private val oldList = mutableListOf<VideoTrackModel>()
//    private val oldList = ArrayList<VideoTrackModel>()
//
//    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ParticipantHolder {
//        val view = LayoutInflater.from(parent.context).inflate(R.layout.participant_item_view, parent,false)
//        val holder = ParticipantHolder(view)
//        return holder
//
//    }
//
//    override fun onBindViewHolder(holder: ParticipantHolder, position: Int) {
//        holder.unbindSurfaceView() // Free the context initialized for the previous item
//        holder.bind(oldList[position])
//        holder.bindSurfaceView()
////        holder.name.text = oldList[position].name
////
////        oldList[position].videoTrack?.addSink(holder.remoteView)
//    }
//
//    override fun onBindViewHolder(
//        holder: ParticipantHolder,
//        position: Int,
//        payloads: MutableList<Any>
//    ) {
//        Log.d("participantAdapter","payload size ${payloads.size}")
//        if (payloads.isEmpty()) {
//            return super.onBindViewHolder(holder, position, payloads)
//        }else if (payloads.all { it is PeerUpdatePayloads }) {
//            payloads.forEach { payload ->
//                if (payload is PeerUpdatePayloads) {
//                    when (payload){
//                        is PeerUpdatePayloads.NameChanged -> {
//                            holder.name.text = payload.name
//                        }
//                        is PeerUpdatePayloads.SpeakerMuteUnmute -> {
//                            if (payload.isMute){
//                                holder.micMute()
//                            }else {
//                                holder.micUnMute()
//                            }
//                        }
//                        is PeerUpdatePayloads.VideoEnabled -> {
//                            if (payload.isVideoEnabled) {
//                                holder.showSurfaceView()
//                            } else {
//                                holder.hideSurfaceView()
//                            }
//                        }
//                    }
//                }
//            }
//        }else {
//            holder.unbindSurfaceView() // Free the context initialized for the previous item
//            holder.bind(oldList[position])
//            holder.bindSurfaceView()
//        }
//    }
//
////    fun setList(employees: List<VideoTrackModel>) {
////        val diffCallback = ParticipantDiffUtils(oldList = oldList, newList = employees)
////        val diffResult = DiffUtil.calculateDiff(diffCallback)
////        oldList.addAll(employees)
////        diffResult.dispatchUpdatesTo(this)
////    }
//
//    fun updateListItems(employees: List<VideoTrackModel>) {
//        val diffCallback = ParticipantDiffUtils(oldList = oldList, newList = employees)
//        val diffResult = DiffUtil.calculateDiff(diffCallback)
//        diffResult.dispatchUpdatesTo(this)
//        oldList.clear()
//        oldList.addAll(employees)
//    }
//
//    override fun onViewAttachedToWindow(holder: ParticipantHolder) {
//        super.onViewAttachedToWindow(holder)
//        Log.d("participantAdapter","View Attach")
//        holder.bindSurfaceView()
//    }
//
//    override fun onViewDetachedFromWindow(holder: ParticipantHolder) {
//        super.onViewDetachedFromWindow(holder)
//        Log.d("participantAdapter","View detach")
//        holder.unbindSurfaceView()
//    }
//
//    override fun getItemCount() = oldList.size
//
//
//    class ParticipantHolder(itemView: View): RecyclerView.ViewHolder(itemView){
//
//        val remoteView: SurfaceViewRenderer = itemView.findViewById(R.id.remoteView)
//        val name: TextView = itemView.findViewById(R.id.name)
//        val mute: ImageView = itemView.findViewById(R.id.mic)
//        private var isSurfaceViewBinded = false
//        private var itemRef: VideoTrackModel? = null
////
//        init {
//            remoteView.run {
//                setMirror(true)
//                setEnableHardwareScaler(true)
//                init(EglBaseClass.rootEglBase!!.eglBaseContext, null)
//                setZOrderMediaOverlay(true)
//            }
//        }
//
//        fun bind(item: VideoTrackModel) {
//            name.text = item.name
//
//            remoteView.apply {
////                init(EglBaseClass.rootEglBase!!.eglBaseContext, null)
//                setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT)
//                // Meanwhile until the video is not binded, hide the view.
//                visibility = View.GONE
//                // Update the reference such that when view is attached to window
//                // surface view is initialized with correct [VideoTrack]
//                item.videoTrack?.let{ it.addSink(this) }
//                itemRef = item
//                isSurfaceViewBinded = false
//            }
//        }
//
//        fun bindSurfaceView() {
//            if (isSurfaceViewBinded) {
//                Log.d("participantAdapter", "bindSurfaceView: Surface view already initialized")
//                return
//            }
//
//            itemRef?.videoTrack?.let {
//                it.addSink(remoteView)
//                remoteView.visibility = View.VISIBLE
//                isSurfaceViewBinded = true
//            }
//
//
//        }
//
//        fun unbindSurfaceView() {
//            if (!isSurfaceViewBinded) return
//
////            remoteView.release()
//            isSurfaceViewBinded = false
//            remoteView.visibility = View.GONE
//
//        }
//
//        fun micMute() {
//            mute.apply {
//                setImageResource(R.drawable.mic_mute)
//            }
//        }
//
//        fun micUnMute() {
//            mute.apply {
//                setImageResource(R.drawable.mic_unmute)
//            }
//        }
//
//        fun showSurfaceView() {
//            remoteView.apply {
//                visibility = View.VISIBLE
//            }
//        }
//
//        fun hideSurfaceView() {
//            remoteView.apply {
//                visibility = View.GONE
//            }
//        }
//
//    }
//
//    sealed class PeerUpdatePayloads {
//        data class NameChanged(val name: String) : PeerUpdatePayloads()
//        data class SpeakerMuteUnmute(val isMute: Boolean) : PeerUpdatePayloads()
//        data class VideoEnabled(val isVideoEnabled: Boolean) : PeerUpdatePayloads()
//    }
//}