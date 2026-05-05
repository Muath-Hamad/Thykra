import SwiftUI
import ComposeApp

@main
struct iOSApp: App {
    var body: some Scene {
        WindowGroup {
            ContentView()
                .onOpenURL { url in
                    // Forward the URL into the Compose navigation graph via the
                    // shared DeepLinkBus. The Kotlin file `DeepLinkBus.kt` exposes
                    // a top-level `handleDeepLink(url:)` function which becomes
                    // `DeepLinkBusKt.handleDeepLink(url:)` on the Swift side.
                    _ = DeepLinkBusKt.handleDeepLink(url: url.absoluteString)
                }
        }
    }
}