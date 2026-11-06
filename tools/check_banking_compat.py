#!/usr/bin/env python3
"""Bank-site handoff safety regression; no actual banking account is accessed.

Native bank applications are independent packages and are NEVER embedded or
impersonated. Only on-device, user-approved app handoff is supported.
"""
import json
from pathlib import Path

root = Path(__file__).resolve().parents[1]
app = root / "android/app-telefon/src/main/java/com/eymen/browser/telefon"
motor = (root / "android/core-motor/src/main/java/com/eymen/browser/motor/GeckoMotorDenemesi.kt").read_text()
activity = (app / "MainActivity.kt").read_text()
menu = (app / "BrowserMenu.kt").read_text()
policy = (app / "ExternalAppLinkPolicy.kt").read_text()

assert "session.navigationDelegate = object : GeckoSession.NavigationDelegate" in motor
assert "override fun onLoadRequest(" in motor
assert "request.hasUserGesture" in motor
assert "request.triggerUri" in motor
assert "onExternalAppLink(uri, request.triggerUri, request.hasUserGesture)" in motor
assert "AllowOrDeny.DENY" in motor
assert '"https", "http", "about", "data", "blob", "javascript"' in motor

assert "fun fromWebsite(" in policy
assert "hasUserGesture: Boolean" in policy
assert "if (!hasUserGesture ||" in policy
assert "GeckoMotorDenemesi.verifiedHttpsUrl(sourceUrl)" in policy
assert "Intent.parseUri(rawLink, Intent.URI_INTENT_SCHEME)" in policy
assert "packageSyntax.matches(packageName)" in policy
assert "scheme in blockedSchemes" in policy
assert '"file", "content", "data", "javascript", "about"' in policy
assert "Intent(Intent.ACTION_VIEW, dest)" in policy
assert "addCategory(Intent.CATEGORY_BROWSABLE)" in policy
assert "setPackage(packageName)" in policy
assert "setComponent(" not in policy and "addFlags(" not in policy
assert "putExtra(" not in policy and "Log." not in policy

assert "onExternalAppLink = { link, trigger, gesture ->" in activity
assert "ExternalAppLinkPolicy.fromWebsite(link, origin, hasGesture)" in activity
assert "activeId.value != tabId" in activity
assert "showSecureExternalAppPrompt(" in activity
assert "setPositiveButton(R.string.bank_external_confirm)" in activity
assert "startActivity(safe.intent)" in activity

assert "MenuAction.OPEN_EXTERNAL -> openCurrentPageInOfficialApp()" in activity
assert "Intent.FLAG_ACTIVITY_REQUIRE_NON_BROWSER" in activity
assert "Intent.createChooser(" in activity
assert "MenuAction.OPEN_EXTERNAL" in menu
assert "R.string.menu_open_official_app" in menu

for callback in ("onAlertPrompt", "onButtonPrompt", "onTextPrompt"):
    assert f"override fun {callback}(" in activity
assert "installStandardWebPrompts(tab)" in activity
assert "tab.gecko.promptDelegate = object : GeckoSession.PromptDelegate" in activity
assert "verifiedHost(tab.url)" in activity
assert "prompt.message.orEmpty().take(3000)" in activity
assert "result.complete(prompt.dismiss())" in activity
assert "AtomicBoolean(false)" in activity
assert "edit.text.clear()" in activity
assert "bank_https_handoff_prompt" in activity
for lang in ("tr", "en"):
    strings = json.loads((root / f"shared/i18n/{lang}.json").read_text())
    for k in ("menu_open_official_app", "menu_open_official_app_hint",
              "bank_external_title", "bank_external_prompt",
              "bank_external_missing", "bank_external_origin",
              "bank_external_target", "bank_external_confirm",
              "bank_https_handoff_prompt", "bank_js_prompt_title"):
        assert isinstance(strings.get(k), str) and strings[k], f"{lang}: {k}"

print("Bank-app handoff, external-intent sanitization and Gecko dialog policy checks passed.")
print("Not evidence of successful third-party bank authentication.")
