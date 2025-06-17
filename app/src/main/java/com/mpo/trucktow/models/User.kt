package com.mpo.trucktow.models

data class User(
    val id: String,
    val name: String,
    val email: String,
    var phoneNumber: String,
    var address: String
) 