package com.jameeli.thykra.ui.upload

import com.jameeli.thykra.ui.media.PlatformMediaFile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Photos handed to the app from outside it — Android's share sheet, and eventually the
 * iOS share extension.
 *
 * A [kotlinx.coroutines.flow.StateFlow] rather than an event flow, deliberately, and for
 * the same reason [com.jameeli.thykra.navigation.DeepLinkBus] keeps a `pending`: a share
 * is usually a *cold start*. The files arrive while the activity is still being built and
 * before any composable is listening, and if the app was signed out they have to survive
 * the sign-in round trip that rebuilds the whole graph. Held state survives both; an
 * emitted event does not.
 *
 * The files are held, not copied. [PlatformMediaFile.readBytes] is a lambda over a
 * content URI whose read permission lives only as long as the activity that received the
 * intent, so this must be drained during that activity's life — [clear] is called once
 * the queue has staged the bytes to disk, and never before.
 */
object IncomingShareBus {

    private val _pending = MutableStateFlow<List<PlatformMediaFile>>(emptyList())

    /** Non-empty when something was shared in and has not been dealt with yet. */
    val pending: StateFlow<List<PlatformMediaFile>> = _pending.asStateFlow()

    /**
     * Offers shared files to the app. Replaces rather than appends: two shares in a row
     * without the first being resolved means the person changed their mind, and the
     * second is what they meant.
     */
    fun offer(files: List<PlatformMediaFile>) {
        if (files.isEmpty()) return
        _pending.value = files
    }

    /** Called once the files have been queued, or the picker was dismissed. */
    fun clear() {
        _pending.value = emptyList()
    }
}
