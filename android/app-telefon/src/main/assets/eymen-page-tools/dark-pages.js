/* Signed-APK built-in page style. The owning live GeckoSession decides policy. */
(() => {
  "use strict";
  if (location.protocol !== "https:" || window.top !== window) return;
  const id = "eymen-dark-pages-v1";
  function apply(enabled) {
    const old = document.getElementById(id);
    if (!enabled) { if (old) old.remove(); return; }
    if (old) return;
    const style = document.createElement("style");
    style.id = id;
    // Restore image/video colors after inverting the document. This deliberate,
    // optional effect can vary on sites with elaborate blending or backgrounds.
    style.textContent = "html{color-scheme:light!important;background:Canvas!important;filter:invert(1) hue-rotate(180deg)!important}img,video,canvas{filter:invert(1) hue-rotate(180deg)!important}";
    const parent = document.head || document.documentElement;
    if (parent) parent.appendChild(style);
    else document.addEventListener("DOMContentLoaded", () => apply(true), {once:true});
  }
  try {
    Promise.resolve(browser.runtime.sendNativeMessage("eymen_page_tools", {type:"dark-page-policy-v1"}))
      .then(raw => {const reply=typeof raw==="string"?JSON.parse(raw):raw;apply(reply && reply.enabled===true);})
      .catch(() => apply(false));
  } catch (_) { apply(false); }
})();
