import SwiftUI
import WebKit

/// Aşama 1: WKWebView ile HTTPS yükleme denemesi; henüz ürün ekranı değildir.
struct MotorDenemesiView: UIViewRepresentable {
    func makeUIView(context: Context) -> WKWebView {
        let configuration = WKWebViewConfiguration()
        let browser = WKWebView(frame: .zero, configuration: configuration)
        let page = ProcessInfo.processInfo.environment["EYMEN_TEST_URL"] ?? "https://example.org/"
        if let url = URL(string: page), url.scheme == "https", url.host != nil {
            browser.load(URLRequest(url: url))
        }
        return browser
    }

    func updateUIView(_ uiView: WKWebView, context: Context) {}
}
