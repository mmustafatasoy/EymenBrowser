/* EYMEN Browser's bundled, opt-in, cosmetic-only filter.
 * No fetch calls, no network interception and no browsing URL telemetry.
 * A GeckoSession-validated Android native policy grants or denies injection
 * for each HTTPS top-level document. Failure to receive permission => no CSS.
 */
(() => {
  "use strict";
  if (location.protocol !== "https:" || window.top !== window) return;

  // Conservative selectors: known ad *slots*, never generic .ad, iframe,
  // video, sponsored news, sign-in elements or site-wide containers.
  const SELECTORS = Object.freeze([
    "ins.adsbygoogle",
    "[data-ad-client][data-ad-slot]",
    "[id^='div-gpt-ad-']",
    "[id^='google_ads_iframe_']",
    "iframe[id^='google_ads_iframe_']"
  ]);

  function installCosmeticStyle() {
    if (document.getElementById("eymen-cosmetic-v1")) return;
    const style = document.createElement("style");
    style.id = "eymen-cosmetic-v1";
    style.textContent = SELECTORS.join(",\n") + "{display:none!important;}";
    (document.head || document.documentElement).appendChild(style);
  }

  // Never trust page-controlled globals or a user-provided URL as policy.
  // GeckoView MessageSender.session identifies the real owning GeckoSession.
  try {
    browser.runtime.sendNativeMessage("eymen_cosmetic", { type: "cosmetic-policy-v1" })
      .then(reply => {
        if (reply && reply.enabled === true) installCosmeticStyle();
      })
      .catch(() => {}); // fail closed when extension is unavailable
  } catch (_error) {
    // fail closed
  }
})();
