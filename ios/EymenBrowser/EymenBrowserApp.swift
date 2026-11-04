import SwiftUI

@main
struct EymenBrowserApp: App {
    var body: some Scene {
        WindowGroup {
            // Aşama 0: yalnız token temelli boş SwiftUI uygulaması.
            EymenTokens.gece_zemin.ignoresSafeArea()
        }
    }
}
