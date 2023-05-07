package com.example.webrtcp2p.screenViews

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.icons.Icons
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.webrtcp2p.MainActivity
import com.example.webrtcp2p.R
import com.example.webrtcp2p.interfaces.MainScreenInterface

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    MainView(object : MainScreenInterface{
        override fun onCallClick(meetingId: String, userName: String, callingToUserName: String) {
            TODO("Not yet implemented")
        }
    })
}
@Composable
fun MainView(mainOnClick: MainScreenInterface){
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally,
    ){

        var isMeetingEmpty by rememberSaveable { mutableStateOf(false) }
        var isUserNameEmpty by rememberSaveable { mutableStateOf(false) }
        var isCallingToUserNameEmpty by rememberSaveable { mutableStateOf(false) }

        fun validateMeeting(meetingID: String, username: String,
                            callingTopUsername: String) {
            isMeetingEmpty = meetingID.isEmpty()
            isUserNameEmpty = username.isEmpty()
            isCallingToUserNameEmpty = callingTopUsername.isEmpty()
        }

        Spacer(modifier = Modifier.height(40.dp))
        var meetingIDText by remember { mutableStateOf(TextFieldValue("")) }
        TextField(
            value = meetingIDText,
            onValueChange = {
                meetingIDText = it
                isMeetingEmpty = false
            },
            singleLine = true,
            isError = isMeetingEmpty,
            placeholder = { Text(text = "Meeting Id") },
        )
        if (isMeetingEmpty) {
            Text(
                text = "Please enter meeting ID",
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(start = 16.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))
        var usernameText by remember { mutableStateOf(TextFieldValue("")) }
        TextField(
            value = usernameText,
            onValueChange = {
                usernameText = it
                isUserNameEmpty = false
            },
            singleLine = true,
            isError = isUserNameEmpty,
            placeholder = { Text(text = "username") },
        )
        if (isUserNameEmpty) {
            Text(
                text = "Please enter Username",
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(start = 16.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))
        var callingToUsernameText by remember { mutableStateOf(TextFieldValue("")) }
        TextField(
            value = callingToUsernameText,
            onValueChange = {
                callingToUsernameText = it
                isCallingToUserNameEmpty = false
            },
            singleLine = true,
            isError = isCallingToUserNameEmpty,
            placeholder = { Text(text = "calling to username") },
        )
        if (isCallingToUserNameEmpty) {
            Text(
                text = "Please enter calling to username",
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(start = 16.dp)
            )
        }

        Spacer(modifier = Modifier.height(40.dp))
        Image(painter = painterResource(id = R.drawable.call_icon),
            contentDescription = "",
            modifier = Modifier
                .clickable {
                    validateMeeting(
                        meetingIDText.text, usernameText.text,
                        callingToUsernameText.text
                    )

                    mainOnClick.onCallClick(
                        meetingIDText.text, usernameText.text,
                        callingToUsernameText.text
                    )
                }
                .height(30.dp)
                .width(30.dp))

    }

}