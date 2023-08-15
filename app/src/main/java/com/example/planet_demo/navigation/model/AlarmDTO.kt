package com.example.planet_demo.navigation.model

import com.google.api.Billing
import java.util.*

data class AlarmDTO(
    var alarmId: String = UUID.randomUUID().toString(), // 고유한 UUID 값
    var destinationUid: String? = null,
    var userId: String? = null,
    var uid:String? = null,
    var kind: Int? = 0, //0: 좋아요, 1: 댓글, 2: 팔로우
    var message: String? = null,
    var timestamp: Long? = null
)