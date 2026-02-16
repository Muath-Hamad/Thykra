package com.jameeli.thykra

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform