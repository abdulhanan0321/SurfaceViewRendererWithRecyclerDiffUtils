package com.example.webrtcp2p

import android.util.Log
import androidx.recyclerview.widget.DiffUtil

class ParticipantDiffUtils(private val oldList: List<VideoTrackModel>,
                           private val newList: List<VideoTrackModel>): DiffUtil.Callback() {

    override fun getOldListSize() = oldList.size

    override fun getNewListSize() = newList.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition].id == newList[newItemPosition].id
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition] == newList[newItemPosition]
//        oldList[oldItemPosition].name == newList[newItemPosition].name
//                && oldList[oldItemPosition].isMute == newList[newItemPosition].isMute
//                && oldList[oldItemPosition].videoTrack == newList[newItemPosition].videoTrack
//        when{
//            oldList[oldItemPosition].videoTrack == newList[newItemPosition].videoTrack -> {
//                false
//            }
//            oldList[oldItemPosition].name == newList[newItemPosition].name -> {
//                false
//            }
//            oldList[oldItemPosition].isMute == newList[newItemPosition].isMute -> {
//                false
//            }
//            else -> true
//        }
    }

    override fun getChangePayload(oldItemPosition: Int, newItemPosition: Int): Any? {
        if (oldItemPosition >= oldList.size || newItemPosition >= newList.size) {
            return null
        }

        val old = oldList[oldItemPosition]
        val new = newList[newItemPosition]

        if (old.isMute != new.isMute) {
            Log.d("adsvavd", "isMute not equals at $newItemPosition")
            return participantAdapter.PeerUpdatePayloads.SpeakerMuteUnmute(new.isMute)
        } else if (!old.name.equals(new.name)) {
            return participantAdapter.PeerUpdatePayloads.NameChanged(new.name!!)
        } else if (old.isTrackEnabled != new.isTrackEnabled) {
            return participantAdapter.PeerUpdatePayloads.VideoEnabled(new.isTrackEnabled)
        }

        return null
    }
}