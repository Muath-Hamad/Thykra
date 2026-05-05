package com.jameeli.thykra.ui.share

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGRectMake
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIViewController
import platform.UIKit.popoverPresentationController

/**
 * Presents the iOS system share sheet (UIActivityViewController) on top of
 * the current root view controller. Walks up the presentedViewController
 * chain so the sheet appears above any modal Compose host (e.g. an open
 * picker) instead of being silently swallowed.
 */
@OptIn(ExperimentalForeignApi::class)
actual fun shareText(content: String) {
    val activityVc = UIActivityViewController(
        activityItems = listOf(content),
        applicationActivities = null
    )
    val presenter = topViewController() ?: return
    // On iPad UIActivityViewController is presented as a popover, which
    // requires a source anchor or it throws at runtime. Anchor to the
    // centre of the presenter view; on iPhone this property is ignored.
    val popover = activityVc.popoverPresentationController
    val view = presenter.view
    if (popover != null) {
        popover.sourceView = view
        val (cx, cy) = view.bounds.useContents {
            (origin.x + size.width / 2.0) to (origin.y + size.height / 2.0)
        }
        popover.sourceRect = CGRectMake(cx, cy, 0.0, 0.0)
    }
    presenter.presentViewController(activityVc, animated = true, completion = null)
}

private fun topViewController(): UIViewController? {
    var vc = UIApplication.sharedApplication.keyWindow?.rootViewController
    while (vc?.presentedViewController != null) vc = vc.presentedViewController
    return vc
}
