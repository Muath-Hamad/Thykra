package com.jameeli.thykra.ui.share

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context

actual fun copyToClipboard(text: String) {
    val context = SharingHost.appContext ?: return
    val manager = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
    manager.setPrimaryClip(ClipData.newPlainText("thykra", text))
}
