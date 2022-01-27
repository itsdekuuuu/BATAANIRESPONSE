package com.example.bataanresponse.notification

import android.util.Log
import android.view.View
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.lang.Exception
import java.util.concurrent.Executor

const val TOPIC_ROOT = "/topics"
class MainApp {
    val TAG = "MainApp.kt"
    fun sendNotification(topic: String,title: String,message: String) {
        Log.e(TAG,"notification sent!")
        PushNotification(NotificationData(title, message), "$TOPIC_ROOT/${topic.replace(" ","-")}-topic")
                .also {
                    sendNotificaiton(it)
                    Log.e(TAG,"notification sent 2!")
                }
    }

    fun subscribe(topic: String) {
        FirebaseMessaging.getInstance().subscribeToTopic("$TOPIC_ROOT/${topic.replace(" ","-")}-topic")
                .addOnSuccessListener {
                    Log.e("SUBSCRIBE", topic)
                }.addOnFailureListener {
                    Log.e("SUBSCRIBE FAILED", it.message.toString());
                }
    }

    fun unsubscribe(topic: String) {
        FirebaseMessaging.getInstance().unsubscribeFromTopic("$TOPIC_ROOT/${topic.replace(" ","-")}-topic")
                .addOnSuccessListener {
                    Log.e("UNSUBSCRIBE", topic)
                }.addOnFailureListener {
                    Log.e("UNSUBSCRIBE FAILED", it.message.toString());
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