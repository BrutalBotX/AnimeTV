package com.miruronative.data.auth

object AuthManager {
    var anilistToken: String? = null
    fun current(): String? = anilistToken
}
