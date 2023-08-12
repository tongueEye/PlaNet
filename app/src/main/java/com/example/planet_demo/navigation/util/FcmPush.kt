package com.example.planet_demo.navigation.util

import com.example.planet_demo.navigation.model.PushDTO
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import com.squareup.okhttp.*
import java.io.IOException

//push를 전송해 주는 클래스
class FcmPush {
    var JSON = MediaType.parse("application/json; charset=utf-8")
    var url = "https://fcm.googleapis.com/fcm/send"
    var serverKey="AAAA9yj9Wg8:APA91bGL11w2n7P1SQFih72jJ21YBmQjmKnOCSp-uR0FYkVFt0wUIVAbBx_9Or-e-p96sFjG0EuePoDZ4qSnKz9QG-zISvAZ5VAu0T8Ycq43A2z3Tm5ggt-KTUIZx9X1xq5JesIByZUm"
    var gson: Gson? = null
    var okHttpClient: OkHttpClient? = null
    companion object{
        var instance = FcmPush()
    }

    init {
        gson= Gson()
        okHttpClient= OkHttpClient()
    }

    //push 메시지 전송하는 함수
    fun sendMessage(destinationUid: String, title: String, message: String){
        FirebaseFirestore.getInstance().collection("pushtokens").document(destinationUid).get().addOnCompleteListener {
            task ->
            if(task.isSuccessful){ //상대방의 uid를 이용해서 pushToken을 받아옴
                var token=task?.result?.get("pushToken").toString()

                var pushDTO=PushDTO()
                pushDTO.to=token
                pushDTO.notification.title=title
                pushDTO.notification.body=message

                var body = RequestBody.create(JSON,gson?.toJson(pushDTO))
                var requst = Request.Builder()
                    .addHeader("Content-Type","application/json")
                    .addHeader("Authorization","key="+serverKey)
                    .url(url)
                    .post(body) //post 방식으로 넘겨줌
                    .build()

                okHttpClient?.newCall(requst)?.enqueue(object : Callback{
                    override fun onFailure(request: Request?, e: IOException?) { //요청 실패했을 때

                    }

                    override fun onResponse(response: Response?) { //요청 성공했을 때 - 메시지 출력
                        println(response?.body()?.string())
                    }

                })
            }
        }
    }
}