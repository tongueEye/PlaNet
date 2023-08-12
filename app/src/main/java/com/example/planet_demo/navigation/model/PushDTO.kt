package com.example.planet_demo.navigation.model


data class PushDTO (
    var to: String? = null, //push를 받는 사람의 token ID
    var notification: Notification = Notification()
){
    data class Notification(
        var body: String?=null, //푸시 메시지 주 내용
        var title: String?=null //푸시 메시지 제목
    )
}