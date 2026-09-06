package com.jameeli.thykra

import kotlin.time.Clock
import kotlin.time.Instant

/**
 * The current time, from the one module that already depends on kotlinx-datetime.
 *
 * The UI module reads the clock in three places — relative timestamps, invite expiry and
 * the upload rate sampler — and routing them through here keeps `Clock` resolution a
 * question `:shared` answers once rather than one every downstream source set has to.
 */
fun now(): Instant = Clock.System.now()

fun nowMillis(): Long = Clock.System.now().toEpochMilliseconds()
