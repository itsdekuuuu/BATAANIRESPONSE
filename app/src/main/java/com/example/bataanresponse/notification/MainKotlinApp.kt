package com.example.bataanresponse.notification

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import com.example.bataanresponse.R
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.lang.Exception

const val TOPIC = "/topics/all"

class MainKotlinApp : AppCompatActivity() {

    val TAG = "MainApp.kt"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_kotlin_app)
        FirebaseMessaging.getInstance().subscribeToTopic(TOPIC)
                .addOnSuccessListener {
                    print("success")
                }
    }

    fun clickMe(view: View){
        val title = "Kamusta"
        val message = "Hello po tapos na!"
        PushNotification(NotificationData(title, message), TOPIC)
                .also {
                    sendNotificaiton(it)
                }
    }

    private fun sendNotificaiton(notification: PushNotification) = CoroutineScope(Dispatchers.IO).launch {
        try {
            val response = RetrofitInstance.api.postNotification(notification)
            if (response.isSuccessful) {
                Log.e(TAG, "PUSH NOTIFICATION SUCCESS")
            } else {
                Log.e(TAG, "FAILED TO PUSH NOTIFICATION")
            }
        } catch (e: Exception) {
            Log.e("ERROR FROM NOTIFICAITON", e.toString())
        }
    }
}