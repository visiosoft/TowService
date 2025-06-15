package com.mpo.trucktow.models

data class User(
    val id: Long = 0,
    val email: String,
    val password: String,
    val name: String,
    val phone: String
) 