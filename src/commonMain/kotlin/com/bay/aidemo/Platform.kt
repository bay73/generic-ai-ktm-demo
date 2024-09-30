package com.bay.aidemo

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform
