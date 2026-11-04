import SwiftUI

@main
struct EymenBrowserApp: App {
    var body: some Scene {
        WindowGroup {
            // Aşama 1 WKWebView motor denemesi; tasarım ekranları henüz yazılmadı.
            MotorDenemesiView().ignoresSafeArea()
        }
    }
}
