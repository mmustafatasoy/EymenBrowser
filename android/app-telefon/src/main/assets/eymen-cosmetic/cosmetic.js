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
    const container = document.head || document.documentElement;
    if (container) {
      container.appendChild(style);
    } else {
      document.addEventListener("DOMContentLoaded", () => {
        const target = document.head || document.documentElement;
        if (target && !document.getElementById(style.id)) target.appendChild(style);
      }, { once: true });
    }
  }

  // Click only explicit reject controls from known consent managers.
  // Never accept consent, hide a dialog without a choice, or inspect forms.
  function rejectKnownCookieDialog() {
    const rejectSelectors = [
      "button#onetrust-reject-all-handler",
      "button#CybotCookiebotDialogBodyButtonDecline",
      "button#didomi-notice-disagree-button",
      "button#cookiescript_reject",
      "button#uc-deny-all-button"
    ];
    let observer = null;
    let scheduled = false;
    function attempt() {
      scheduled = false;
      for (const selector of rejectSelectors) {
        const button = document.querySelector(selector);
        if (button && !button.disabled && button.getClientRects().length) {
          button.click();
          if (observer) observer.disconnect();
          return true;
        }
      }
      return false;
    }
    if (attempt() || typeof MutationObserver !== "function") return;
    observer = new MutationObserver(() => {
      if (!scheduled) { scheduled = true; setTimeout(attempt, 80); }
    });
    if (document.documentElement) observer.observe(document.documentElement, { childList: true, subtree: true });
    else document.addEventListener("DOMContentLoaded", () => {
      if (!attempt() && document.documentElement) observer.observe(document.documentElement, { childList: true, subtree: true });
    }, { once: true });
    setTimeout(() => observer.disconnect(), 15000);
  }

  // Never trust page-controlled globals or a user-provided URL as policy.
  // GeckoView MessageSender.session identifies the real owning GeckoSession.
  try {
    browser.runtime.sendNativeMessage("eymen_cosmetic", { type: "cosmetic-policy-v1" })
      .then(serialized => {
        if (typeof serialized !== "string") return;
        const reply = JSON.parse(serialized);
        if (reply && reply.enabled === true) installCosmeticStyle();
        if (reply && reply.rejectCookies === true) rejectKnownCookieDialog();
      })
      .catch(() => {}); // fail closed when extension is unavailable
  } catch (_error) {
    // fail closed
  }
})();
