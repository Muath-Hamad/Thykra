package com.jameeli.thykra

const val SERVER_PORT = 8081
expect val API_HOST: String
val API_BASE_URL: String get() = "http://$API_HOST:$SERVER_PORT"
