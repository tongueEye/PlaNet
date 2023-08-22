package com.example.planet_demo.navigation.model

import java.util.*

data class TodoDTO(
    val todoId: String = UUID.randomUUID().toString(), // 고유한 UUID 값
    val timestamp: Long = System.currentTimeMillis(),
    var checked: Boolean = false,
    val todo: String = ""
)