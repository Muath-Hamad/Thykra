package com.jameeli.thykra.ui.media

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.readBytes
import platform.PhotosUI.PHPickerConfiguration
import platform.PhotosUI.PHPickerFilter
import platform.PhotosUI.PHPickerResult
import platform.PhotosUI.PHPickerViewController
import platform.PhotosUI.PHPickerViewControllerDelegateProtocol
import platform.UIKit.UIApplication
import platform.UIKit.UIViewController
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

@Composable
actual fun rememberMediaPickerLauncher(
    onResult: (List<PlatformMediaFile>) -> Unit
): () -> Unit {
    val delegate = remember { MediaPickerDelegate(onResult) }
    return remember {
        {
            val config = PHPickerConfiguration()
            config.selectionLimit = 0 // unlimited
            config.filter = PHPickerFilter.imagesFilter
            val picker = PHPickerViewController(configuration = config)
            picker.delegate = delegate
            topViewController()?.presentViewController(picker, animated = true, completion = null)
        }
    }
}

private fun topViewController(): UIViewController? {
    var vc = UIApplication.sharedApplication.keyWindow?.rootViewController
    while (vc?.presentedViewController != null) vc = vc.presentedViewController
    return vc
}

@OptIn(ExperimentalForeignApi::class)
private class MediaPickerDelegate(
    private val onResult: (List<PlatformMediaFile>) -> Unit
) : NSObject(), PHPickerViewControllerDelegateProtocol {

    override fun picker(picker: PHPickerViewController, didFinishPicking: List<*>) {
        picker.dismissViewControllerAnimated(true, completion = null)
        val pickerResults = didFinishPicking.filterIsInstance<PHPickerResult>()
        if (pickerResults.isEmpty()) {
            onResult(emptyList())
            return
        }
        loadNext(pickerResults, 0, mutableListOf())
    }

    // Load images one at a time, dispatching back to main thread between each to stay safe.
    // Requests "public.jpeg" so iOS auto-transcodes HEIC to JPEG.
    private fun loadNext(
        results: List<PHPickerResult>,
        index: Int,
        accumulated: MutableList<PlatformMediaFile>
    ) {
        if (index >= results.size) {
            onResult(accumulated)
            return
        }
        val provider = results[index].itemProvider
        if (!provider.hasItemConformingToTypeIdentifier("public.image")) {
            dispatch_async(dispatch_get_main_queue()) {
                loadNext(results, index + 1, accumulated)
            }
            return
        }
        provider.loadDataRepresentationForTypeIdentifier("public.jpeg") { data, _ ->
            if (data != null) {
                val bytes = data.bytes?.readBytes(data.length.toInt()) ?: ByteArray(0)
                val baseName = provider.suggestedName ?: "photo_$index"
                val name = if ('.' in baseName) baseName else "$baseName.jpg"
                accumulated.add(
                    PlatformMediaFile(
                        name = name,
                        contentType = "image/jpeg",
                        size = data.length.toLong(),
                        readBytes = { bytes }
                    )
                )
            }
            dispatch_async(dispatch_get_main_queue()) {
                loadNext(results, index + 1, accumulated)
            }
        }
    }
}
