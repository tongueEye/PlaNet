package com.example.planet_demo.navigation.model

import java.util.*
import kotlin.collections.HashMap

data class ContentDTO(var contentId: String = UUID.randomUUID().toString(), // 고유한 UUID 값
                      var explain: String?=null,
                      var imageUrl: String?=null,
                      var uid:String?=null,
                      var userId: String?=null,
                      var timestamp: Long?=null,
                      var favoriteCount: Int=0,
                      var favorites: MutableMap<String,Boolean> = HashMap()){
    data class Comment(var commentId: String = UUID.randomUUID().toString(), // 고유한 UUID 값
                       var uid: String?=null,
                       var userId: String?=null,
                       var comment: String?=null,
                       var timestamp: Long?=null)
}