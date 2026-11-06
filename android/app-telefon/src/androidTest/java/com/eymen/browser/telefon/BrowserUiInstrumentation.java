package com.eymen.browser.telefon;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayDeque;
import java.util.function.BooleanSupplier;

/** Real touch, keyboard and accessibility interactions. This class is test-APK only. */
public final class BrowserUiInstrumentation extends Instrumentation {
    private Activity activity;
    private Uri fixtureUri;
    private static final String FIXTURE = "Arayüz testi.txt";
    private static final String OLD_URL = "https://example.com/eymen-ui-old?value=1";
    @Override public void onCreate(Bundle args) { super.onCreate(args); start(); }
    @Override public void onStart() {
        Bundle result = new Bundle();
        int code = Activity.RESULT_CANCELED;
        try {
            Context app = getTargetContext().getApplicationContext();
            app.getSharedPreferences("eymen_browser_setup", Context.MODE_PRIVATE).edit()
                .putBoolean("setup_completed", true).putBoolean("day_theme", true)
                .putBoolean("light_design_default_migrated_v105", true).commit();
            app.getSharedPreferences("eymen_child_mode", Context.MODE_PRIVATE).edit().clear().commit();
            new DownloadRegistry(app).clearListOnly();
            createFixture(app);
            AccessibilityServiceInfo info = getUiAutomation().getServiceInfo();
            info.flags |= AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
                | AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS;
            getUiAutomation().setServiceInfo(info);
            activity = startActivitySync(new Intent(Intent.ACTION_VIEW, Uri.parse(OLD_URL), app, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            popupRoundTrip();
            String address = label(R.string.address_edit_label);
            await(() -> field(address) != null && !text(field(address)).isEmpty(), "existing address field");
            tap(field(address));
            await(() -> field(address) != null && field(address).isFocused(), "address receives focus after touch");
            waitForIdleSync();
            SystemClock.sleep(100);
            sendStringSync("eymen yeni arama");
            await(() -> "eymen yeni arama".equals(text(field(address))), "typing replaces the old address without deletion");
            Bundle caret = new Bundle();
            caret.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, 5);
            caret.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, 5);
            require(field(address).performAction(AccessibilityNodeInfo.ACTION_SET_SELECTION, caret), "normal caret action");
            waitForIdleSync();
            SystemClock.sleep(100);
            sendStringSync("x");
            await(() -> "eymenx yeni arama".equals(text(field(address))), "typing at a chosen caret");
            click(label(R.string.address_clear));
            await(() -> field(address) != null && "".equals(text(field(address))), "clear address button");
            sendStringSync("tekrar");
            await(() -> "tekrar".equals(text(field(address))), "field remains writable after clear");

            click("Tarayıcı menüsü");
            click(label(R.string.settings));
            await(() -> field(label(R.string.settings_search_hint)) != null, "custom settings page");
            capture("settings-light.png", true);
            search(label(R.string.settings_search_hint), label(R.string.menu_settings_search));
            click(label(R.string.menu_settings_search));
            click("Bing");
            await(() -> "Bing".equals(prefs().getString("search_engine", "")), "search engine persisted");
            click(label(R.string.back));
            search(label(R.string.settings_search_hint), label(R.string.settings_appearance));
            click(label(R.string.settings_appearance));
            click(label(R.string.theme_night));
            await(() -> !prefs().getBoolean("day_theme", true), "night theme persisted");
            capture("appearance-night.png", false);
            click(label(R.string.theme_day));
            await(() -> prefs().getBoolean("day_theme", false), "day theme persisted");
            click(label(R.string.back));
            search(label(R.string.settings_search_hint), label(R.string.settings_about_update));
            click(label(R.string.settings_about_update));
            await(() -> find(label(R.string.update_manual_check), false) != null, "custom update/about page");
            click(label(R.string.back));
            search(label(R.string.settings_search_hint), label(R.string.downloads));
            click(label(R.string.downloads));
            await(() -> field(label(R.string.download_search_hint)) != null, "custom downloads page");
            await(() -> find(FIXTURE, false) != null, "real saved download is listed");
            capture("downloads-light.png", true);
            click(label(R.string.download_filter_video));
            await(() -> find(FIXTURE, false) == null, "video filter hides the actual text document");
            click(label(R.string.download_filter_document));
            await(() -> find(FIXTURE, false) != null, "document filter restores the actual file");
            search(label(R.string.download_search_hint), "olmayan-dosya");
            await(() -> find(label(R.string.download_no_matches), false) != null, "download search empty result");
            click(label(R.string.search_clear));
            await(() -> find(FIXTURE, false) != null, "clearing search restores the saved file");
            click(label(R.string.download_clear_records));
            click(label(R.string.download_confirm_deny));
            require(!new DownloadRegistry(app).items().isEmpty(), "cancel clear keeps history");
            click(label(R.string.download_clear_records));
            click(label(R.string.download_clear_records));
            await(() -> new DownloadRegistry(app).items().isEmpty(), "custom confirmation clears history");
            try (java.io.InputStream input = app.getContentResolver().openInputStream(fixtureUri)) {
                require(input != null && input.read() >= 0, "clearing history never deletes the real file");
            }
            click(label(R.string.back));
            await(() -> field(address) != null, "downloads back restores the browser");
            click("Ana sayfa");
            await(() -> field(address) != null && "".equals(text(field(address))), "home resets the address editor");
            click("Yeni sekme");
            await(() -> field(address) != null && "".equals(text(field(address))), "new tab has an empty address editor");
            advancedSettings();
            result.putString("eymen_browser_ui_audit", "passed");
            result.putString("checks", "real_popup_open_write_opener_close,address_replace_caret_clear,settings_search_engine_theme,real_download_filter_clear,home_new_tab,real_cookie_reject,real_cookie_storage_clear,real_runtime_privacy,android_autofill_toggle,signed_addon_install_toggle_remove,parent_PIN_allowlist_private_child,local_QR_bitmap_decode,actual_language_recreation");
            code = Activity.RESULT_OK;
        } catch (Throwable error) {
            try { recordFailure(); } catch (Throwable ignored) {}
            result.putString("eymen_browser_ui_audit", "failed");
            result.putString("stack", android.util.Log.getStackTraceString(error));
        } finally {
            if (activity != null) runOnMainSync(() -> activity.finish());
            if (fixtureUri != null) getTargetContext().getContentResolver().delete(fixtureUri, null, null);
            new DownloadRegistry(getTargetContext()).clearListOnly();
            getTargetContext().getSharedPreferences("eymen_child_mode", Context.MODE_PRIVATE).edit().clear().commit();
        }
        finish(code, result);
    }
    private android.content.SharedPreferences prefs() {
        return getTargetContext().getSharedPreferences("eymen_browser_setup", Context.MODE_PRIVATE);
    }
    private org.mozilla.geckoview.GeckoView browserView() throws Exception {
        java.lang.reflect.Field value = MainActivity.class.getDeclaredField("view");
        value.setAccessible(true);
        return (org.mozilla.geckoview.GeckoView) value.get(activity);
    }
    private Object controller(String name) {
        try { java.lang.reflect.Field f = MainActivity.class.getDeclaredField(name); f.setAccessible(true); return f.get(activity); }
        catch (Exception error) { throw new RuntimeException(error); }
    }
    private org.mozilla.geckoview.GeckoRuntime runtime() {
        return com.eymen.browser.motor.GeckoMotorDenemesi.INSTANCE.getRuntime(activity);
    }
    private void openSettingsItem(int labelId) throws Exception {
        click(label(R.string.browser_menu)); click(label(R.string.settings));
        search(label(R.string.settings_search_hint), label(labelId)); click(label(labelId));
    }
    private void advancedSettings() throws Exception {
        BrowserSettingsServices services = (BrowserSettingsServices) controller("settingsServices");
        java.util.concurrent.atomic.AtomicReference<String> pageTitle = new java.util.concurrent.atomic.AtomicReference<>("");
        java.util.concurrent.atomic.AtomicInteger completedLoads = new java.util.concurrent.atomic.AtomicInteger();
        java.util.concurrent.atomic.AtomicBoolean testPageLoading = new java.util.concurrent.atomic.AtomicBoolean();
        org.mozilla.geckoview.GeckoSession page = currentSession();
        runOnMainSync(() -> {
            org.mozilla.geckoview.GeckoSession.ProgressDelegate progress = page.getProgressDelegate();
            page.setProgressDelegate(new org.mozilla.geckoview.GeckoSession.ProgressDelegate() {
                @Override public void onPageStart(org.mozilla.geckoview.GeckoSession s, String url) {
                    testPageLoading.set("https://example.com/".equals(url));
                    progress.onPageStart(s, url);
                }
                @Override public void onPageStop(org.mozilla.geckoview.GeckoSession s, boolean success) {
                    if (testPageLoading.getAndSet(false) && success) completedLoads.incrementAndGet();
                    progress.onPageStop(s, success);
                }
            });
            org.mozilla.geckoview.GeckoSession.ContentDelegate original = page.getContentDelegate();
            page.setContentDelegate(new org.mozilla.geckoview.GeckoSession.ContentDelegate() {
                @Override public void onTitleChange(org.mozilla.geckoview.GeckoSession s, String value) {
                    pageTitle.set(value); original.onTitleChange(s, value);
                }
            });
            page.loadUri("https://example.com/");
        });
        await(() -> completedLoads.get() > 0 && pageTitle.get() != null && pageTitle.get().contains("Example Domain"), "completed HTTPS page for real privacy settings");
        await(() -> uiCheck(() -> Boolean.TRUE.equals(((androidx.compose.runtime.MutableState<?>)controller("cosmeticFilterReady")).getValue())), "bundled policy extension installed in the real engine");
        openSettingsItem(R.string.settings_shield);
        int loadsBeforeToggle = completedLoads.get();
        click(label(R.string.shield_cookie_dialog));
        await(services::getCookieReject, "cookie rejection preference enabled");
        await(() -> completedLoads.get() > loadsBeforeToggle, "privacy change completes a fresh HTTPS reload");
        String fixture = "javascript:void(function(){var b=document.createElement('button');b.id='onetrust-reject-all-handler';b.textContent='Reject all test cookies';b.onclick=function(){document.title='Eymen cookie rejected';};document.body.prepend(b);document.cookie='eymen_ui_cookie=1;SameSite=Lax;Secure';localStorage.setItem('eymen_ui_cookie','1');}())";
        waitForIdleSync(); SystemClock.sleep(500);
        runOnMainSync(() -> page.loadUri(fixture));
        await(() -> "Eymen cookie rejected".equals(pageTitle.get()), "real bundled extension clicks explicit reject, without accepting");
        runOnMainSync(() -> page.loadUri("javascript:void(document.title=(document.cookie.includes('eymen_ui_cookie=')&&localStorage.getItem('eymen_ui_cookie'))?'Eymen storage seeded':'Eymen storage missing')"));
        await(() -> "Eymen storage seeded".equals(pageTitle.get()), "test cookie and DOM storage really exist before clearing");
        click(label(R.string.shield_fingerprint));
        await(() -> Boolean.TRUE.equals(runtime().getSettings().getFingerprintingProtection()), "fingerprinting toggle changes actual Gecko runtime");
        click(label(R.string.shield_fingerprint));
        click(label(R.string.shield_https));
        await(() -> runtime().getSettings().getAllowInsecureConnections() == org.mozilla.geckoview.GeckoRuntimeSettings.ALLOW_ALL, "HTTPS-only setting changes real runtime");
        click(label(R.string.shield_https));
        await(() -> runtime().getSettings().getAllowInsecureConnections() == org.mozilla.geckoview.GeckoRuntimeSettings.HTTPS_ONLY, "HTTPS-only restored");
        scrollClick(label(R.string.cookie_policy_first));
        await(() -> runtime().getSettings().getContentBlocking().getCookieBehavior() == org.mozilla.geckoview.ContentBlocking.CookieBehavior.ACCEPT_FIRST_PARTY, "cookie policy changes real storage behavior");
        scrollClick(label(R.string.cookie_policy_partition));
        capture("privacy-settings.png", true);
        scrollClick(label(R.string.cookie_clear));
        click(activity.getString(android.R.string.ok));
        await(() -> !services.getClearing() && currentSession() != page, "clearing closes old Gecko sessions and finishes actual storage removal");
        click(label(R.string.back)); click(label(R.string.back));
        org.mozilla.geckoview.GeckoSession clean = currentSession();
        java.util.concurrent.atomic.AtomicReference<String> cleanTitle = new java.util.concurrent.atomic.AtomicReference<>("");
        runOnMainSync(() -> {
            org.mozilla.geckoview.GeckoSession.ContentDelegate original = clean.getContentDelegate();
            clean.setContentDelegate(new org.mozilla.geckoview.GeckoSession.ContentDelegate() {
                @Override public void onTitleChange(org.mozilla.geckoview.GeckoSession s, String value) { cleanTitle.set(value); original.onTitleChange(s, value); }
            });
            clean.loadUri("https://example.com/");
        });
        await(() -> cleanTitle.get() != null && cleanTitle.get().contains("Example Domain"), "new clean HTTPS session");
        runOnMainSync(() -> clean.loadUri("javascript:void(document.title=(document.cookie.includes('eymen_ui_cookie=')||localStorage.getItem('eymen_ui_cookie'))?'Eymen storage dirty':'Eymen storage cleared')"));
        await(() -> "Eymen storage cleared".equals(cleanTitle.get()), "real cookies and DOM storage were removed");

        openSettingsItem(R.string.settings_passwords);
        click(label(R.string.autofill_title));
        require(!services.getAutofill(), "autofill preference disabled");
        java.util.concurrent.atomic.AtomicBoolean autofillEnabled = new java.util.concurrent.atomic.AtomicBoolean(true);
        runOnMainSync(() -> { try { autofillEnabled.set(browserView().getAutofillEnabled()); } catch (Exception e) { throw new RuntimeException(e); } });
        require(!autofillEnabled.get(), "actual GeckoView autofill participation disabled");
        click(label(R.string.autofill_title));
        click(label(R.string.back)); click(label(R.string.back));

        openSettingsItem(R.string.settings_site_permissions);
        click(label(R.string.popup_allow));
        require(services.popupAllowed("https://example.com/", false), "site popup permission is persisted for its real HTTPS origin");
        click(label(R.string.popup_allow));
        require(!services.popupAllowed("https://example.com/", false), "site popup permission can be revoked");
        capture("site-permissions.png", true);
        click(label(R.string.back)); click(label(R.string.back));

        openSettingsItem(R.string.menu_extensions);
        BrowserAddons addons = (BrowserAddons) controller("addons");
        search(label(R.string.addon_url_hint), "https://addons.mozilla.org/firefox/downloads/latest/ublock-origin/latest.xpi");
        click(label(R.string.addon_install));
        awaitLong(() -> find(label(R.string.addon_permissions), false) != null, "Mozilla-signed XPI reaches native permission prompt", 90000);
        click(label(R.string.addon_install));
        awaitLong(() -> uiCheck(() -> !addons.getItems().isEmpty() && !addons.getBusy()), "real Mozilla-signed extension installed", 90000);
        String addOnName = addons.getItems().get(0).metaData.name;
        scrollClick(addOnName);
        await(() -> uiCheck(() -> !addons.getItems().isEmpty() && !addons.getItems().get(0).metaData.enabled), "installed extension disabled through Gecko");
        click(addOnName);
        await(() -> uiCheck(() -> !addons.getItems().isEmpty() && addons.getItems().get(0).metaData.enabled), "installed extension re-enabled through Gecko");
        capture("extensions-installed.png", true);
        scrollClick(label(R.string.addon_remove)); click(activity.getString(android.R.string.ok));
        await(() -> uiCheck(() -> addons.getItems().isEmpty()), "real extension uninstalled");
        click(label(R.string.back)); click(label(R.string.back));

        openSettingsItem(R.string.settings_child_mode);
        click(label(R.string.child_set_pin)); enterPin("947251"); enterPin("947251");
        android.content.SharedPreferences childPrefs = getTargetContext().getSharedPreferences("eymen_child_mode", 0);
        await(() -> childPrefs.contains("hash"), "parent PIN stored as salted hash");
        search(label(R.string.child_host_hint), "example.com");
        scrollClick(label(R.string.child_add_host)); enterPin("947251");
        await(() -> childPrefs.getStringSet("hosts", java.util.Collections.emptySet()).contains("example.com"), "allowlist saved after parent authentication");
        scrollClick(label(R.string.child_enable)); enterPin("947251");
        await(() -> childPrefs.getBoolean("enabled", false), "PIN enables real child mode");
        require(currentSession().getSettings().getUsePrivateMode(), "child browsing uses a real private Gecko session");
        runOnMainSync(() -> {
            try {
                java.lang.reflect.Method method = MainActivity.class.getDeclaredMethod("handleMenuAction", MenuAction.class);
                method.setAccessible(true); method.invoke(activity, MenuAction.QR);
            } catch (Exception error) { throw new RuntimeException(error); }
        });
        waitForIdleSync(); SystemClock.sleep(200);
        require(getTargetContext().getPackageName().contentEquals(getUiAutomation().getRootInActiveWindow().getPackageName()),
            "child mode does not hand off to camera or its permission activity");
        capture("child-mode.png", true);
        click(label(R.string.child_disable)); enterPin("000000");
        await(() -> find(label(R.string.child_wrong_pin), false) != null, "incorrect parent PIN is rejected");
        require(childPrefs.getBoolean("enabled", false), "wrong PIN cannot exit child mode");
        click(label(R.string.child_disable)); enterPin("947251");
        await(() -> !childPrefs.getBoolean("enabled", true), "correct PIN exits child mode");
        require(!currentSession().getSettings().getUsePrivateMode(), "parent mode restores normal browsing");
        click(label(R.string.back)); click(label(R.string.back));

        verifyQrImage();
        openSettingsItem(R.string.settings_language);
        click("English");
        await(() -> find("Device language", false) != null, "actual Activity recreation displays English settings");
        require("en".equals(prefs().getString("app_language", "")), "English preference persisted");
        capture("language-english.png", true);
        click("Türkçe");
        await(() -> find("Telefonun dili", false) != null, "Turkish UI restored after locale recreation");
    }
    private void verifyQrImage() throws Exception {
        String expected = "https://example.com/eymen-qr-acceptance";
        com.google.zxing.common.BitMatrix matrix = new com.google.zxing.MultiFormatWriter().encode(expected, com.google.zxing.BarcodeFormat.QR_CODE, 384, 384);
        Bitmap bitmap = Bitmap.createBitmap(384, 384, Bitmap.Config.ARGB_8888);
        for (int y=0; y<384; y++) for (int x=0; x<384; x++) bitmap.setPixel(x,y,matrix.get(x,y)?Color.BLACK:Color.WHITE);
        File file = new File(getTargetContext().getCacheDir(), "qr-acceptance.png");
        try {
            try (FileOutputStream stream = new FileOutputStream(file)) { bitmap.compress(Bitmap.CompressFormat.PNG,100,stream); }
            require(expected.equals(BrowserQrScanner.decode(file)), "real Android bitmap decoded locally as a QR URL");
        } finally { bitmap.recycle(); file.delete(); }
    }
    private void enterPin(String value) throws Exception {
        await(() -> pinEditor() != null, "native parent PIN editor");
        Bundle args = new Bundle(); args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, value);
        require(pinEditor().performAction(AccessibilityNodeInfo.ACTION_SET_TEXT,args), "enter test-only PIN");
        click(activity.getString(android.R.string.ok));
    }
    private AccessibilityNodeInfo pinEditor() {
        AccessibilityNodeInfo root = getUiAutomation().getRootInActiveWindow();
        if (root == null) return null;
        ArrayDeque<AccessibilityNodeInfo> queue = new ArrayDeque<>(); queue.add(root);
        while (!queue.isEmpty()) {
            AccessibilityNodeInfo node=queue.removeFirst(); if (!node.refresh()) continue;
            if (node.isVisibleToUser() && node.isEditable() && node.isPassword()) return node;
            for(int i=0;i<node.getChildCount();i++){AccessibilityNodeInfo child=node.getChild(i);if(child!=null)queue.add(child);}
        }
        return null;
    }
    private void scrollClick(String label) throws Exception {
        if (clickTarget(label)==null) for(int attempt=0;attempt<3 && clickTarget(label)==null;attempt++) swipe(false);
        for(int attempt=0;attempt<8 && clickTarget(label)==null;attempt++) swipeUp();
        click(label);
    }
    private void swipeUp() {
        swipe(true);
    }
    private void swipe(boolean up) {
        Bitmap bitmap=getUiAutomation().takeScreenshot(); int width=bitmap.getWidth(),height=bitmap.getHeight(); bitmap.recycle();
        long start=SystemClock.uptimeMillis();
        for(int step=0;step<=12;step++){
            float y=height*(up ? 0.76f-0.38f*step/12f : 0.38f+0.38f*step/12f);
            int action=step==0?MotionEvent.ACTION_DOWN:step==12?MotionEvent.ACTION_UP:MotionEvent.ACTION_MOVE;
            MotionEvent event=MotionEvent.obtain(start,SystemClock.uptimeMillis(),action,width*0.65f,y,0);event.setSource(InputDevice.SOURCE_TOUCHSCREEN);
            getUiAutomation().injectInputEvent(event,true);event.recycle();SystemClock.sleep(25);
        }
        waitForIdleSync();SystemClock.sleep(150);
    }
    private static void awaitLong(BooleanSupplier condition,String name,long timeout) throws Exception {
        long until=SystemClock.elapsedRealtime()+timeout;
        while(!condition.getAsBoolean()){if(SystemClock.elapsedRealtime()>until)throw new AssertionError("Timed out: "+name);Thread.sleep(100);}
    }
    private org.mozilla.geckoview.GeckoSession currentSession() {
        java.util.concurrent.atomic.AtomicReference<org.mozilla.geckoview.GeckoSession> result = new java.util.concurrent.atomic.AtomicReference<>();
        runOnMainSync(() -> { try { result.set(browserView().getSession()); } catch (Exception e) { throw new RuntimeException(e); } });
        return result.get();
    }
    private void popupRoundTrip() throws Exception {
        org.mozilla.geckoview.GeckoSession parent = currentSession();
        java.util.concurrent.atomic.AtomicReference<String> title = new java.util.concurrent.atomic.AtomicReference<>("");
        java.util.concurrent.atomic.AtomicReference<org.mozilla.geckoview.GeckoSession.ContentDelegate> delegate = new java.util.concurrent.atomic.AtomicReference<>();
        runOnMainSync(() -> delegate.set(parent.getContentDelegate()));
        org.mozilla.geckoview.GeckoSession.ContentDelegate original = delegate.get();
        runOnMainSync(() -> {
            parent.setContentDelegate(new org.mozilla.geckoview.GeckoSession.ContentDelegate() {
                @Override public void onTitleChange(org.mozilla.geckoview.GeckoSession session, String text) {
                    title.set(text); original.onTitleChange(session, text);
                }
            });
            parent.loadUri("https://example.com/");
        });
        await(() -> title.get() != null && title.get().contains("Example Domain"), "real HTTPS fixture page loaded");
        String child = "<html><head><title>Eymen sign-in window</title></head><body style='margin:0'><button style='position:fixed;inset:0;width:100%;height:100%' onclick='window.close()'>Close sign-in test</button></body></html>";
        String open = "var w=window.open('about:blank','_blank');if(w){w.document.open();w.document.write(" + org.json.JSONObject.quote(child) + ");w.document.close();w.opener.document.title='Eymen opener verified';}";
        String page = "var b=document.createElement('button');b.textContent='Open sign-in test';b.style.cssText='position:fixed;inset:0;width:100%;height:100%;z-index:99999';b.onclick=function(){" + open + "};document.body.replaceChildren(b);document.title='Eymen popup fixture';";
        String script = "javascript:void(function(){if(document.body){" + page + "}else{document.addEventListener('DOMContentLoaded',function(){" + page + "},{once:true});}}())";
        runOnMainSync(() -> parent.loadUri(script));
        await(() -> "Eymen popup fixture".equals(title.get()), "in-page popup fixture ready");
        tapBrowserCenter();
        await(() -> currentSession() != parent, "user-clicked sign-in popup attached without app crash");
        org.mozilla.geckoview.GeckoSession popup = currentSession();
        require(uiCheck(popup::isOpen), "Gecko opened the popup session before the view attached");
        await(() -> "Eymen opener verified".equals(title.get()), "popup can write and reach its live parent");
        waitForIdleSync(); SystemClock.sleep(350);
        tapBrowserCenter();
        await(() -> currentSession() == parent, "window.close returns to the original sign-in tab");
        runOnMainSync(() -> { parent.setContentDelegate(original); parent.loadUri(OLD_URL); });
    }
    private void tapBrowserCenter() {
        int[] point = new int[2];
        runOnMainSync(() -> { try {
            org.mozilla.geckoview.GeckoView view = browserView();
            view.getLocationOnScreen(point); point[0] += view.getWidth() / 2; point[1] += view.getHeight() / 2;
        } catch (Exception e) { throw new RuntimeException(e); } });
        tapAt(point[0], point[1]);
    }
    private boolean uiCheck(BooleanSupplier condition) {
        java.util.concurrent.atomic.AtomicBoolean result = new java.util.concurrent.atomic.AtomicBoolean();
        runOnMainSync(() -> result.set(condition.getAsBoolean()));
        return result.get();
    }
    private String label(int resource) { return activity.getString(resource); }
    private static String text(AccessibilityNodeInfo node) {
        return node == null || !node.refresh() || node.getText() == null ? "" : node.getText().toString();
    }
    private AccessibilityNodeInfo field(String label) { return find(label, true); }
    private static boolean editorNode(AccessibilityNodeInfo node) {
        if (node.isEditable() || "android.widget.EditText".equals(String.valueOf(node.getClassName()))) return true;
        for (AccessibilityNodeInfo.AccessibilityAction action : node.getActionList())
            if (action.getId() == AccessibilityNodeInfo.ACTION_SET_TEXT) return true;
        return false;
    }
    private static boolean matches(AccessibilityNodeInfo node, String label) {
        String desc = node.getContentDescription() == null ? "" : node.getContentDescription().toString();
        String value = text(node);
        return desc.equals(label) || value.equals(label) || value.startsWith(label + "\n");
    }
    private AccessibilityNodeInfo find(String label, boolean editable) {
        ArrayDeque<AccessibilityNodeInfo> queue = roots();
        java.util.List<AccessibilityNodeInfo> editors = new java.util.ArrayList<>();
        java.util.List<Rect> markers = new java.util.ArrayList<>();
        while (!queue.isEmpty()) {
            AccessibilityNodeInfo node = queue.removeFirst();
            if (!node.refresh()) continue;
            if (node.isVisibleToUser()) {
                if (matches(node, label)) {
                    if (!editable) return node;
                    Rect bounds = new Rect(); node.getBoundsInScreen(bounds);
                    markers.add(bounds);
                }
                if (editable && editorNode(node)) editors.add(node);
            }
            for (int i = 0; i < node.getChildCount(); i++) {
                AccessibilityNodeInfo child = node.getChild(i);
                if (child != null) queue.add(child);
            }
        }
        // With value-based BasicTextField, Compose may put the description
        // inside the EditText virtual node after focus. Link the labelled
        // control to its visible editor by bounds instead of parent direction.
        editors.sort((left, right) -> Integer.compare(left.isEditable() ? 0 : 1,
            right.isEditable() ? 0 : 1));
        for (AccessibilityNodeInfo node : editors) {
            Rect bounds = new Rect(); node.getBoundsInScreen(bounds);
            for (Rect marker : markers)
                if (bounds.contains(marker.centerX(), marker.centerY())) return node;
        }
        return null;
    }
    private ArrayDeque<AccessibilityNodeInfo> roots() {
        ArrayDeque<AccessibilityNodeInfo> queue = new ArrayDeque<>();
        for (AccessibilityWindowInfo window : getUiAutomation().getWindows()) {
            if (window.getType() != AccessibilityWindowInfo.TYPE_APPLICATION) continue;
            AccessibilityNodeInfo root = window.getRoot();
            if (root != null && root.refresh()) queue.add(root);
        }
        return queue;
    }
    private void recordFailure() throws Exception {
        File directory = new File(getTargetContext().getCacheDir(), "browser-ui-audit");
        directory.mkdirs();
        Bitmap bitmap = getUiAutomation().takeScreenshot();
        if (bitmap != null) {
            try (FileOutputStream out = new FileOutputStream(new File(directory, "failure.png"))) {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            }
            bitmap.recycle();
        }
        StringBuilder tree = new StringBuilder();
        ArrayDeque<AccessibilityNodeInfo> queue = roots();
        int count = 0;
        while (!queue.isEmpty() && count++ < 1500) {
            AccessibilityNodeInfo node = queue.removeFirst();
            if (!node.refresh()) continue;
            Rect rect = new Rect(); node.getBoundsInScreen(rect);
            tree.append(node.getClassName()).append(" visible=").append(node.isVisibleToUser())
                .append(" editable=").append(node.isEditable()).append(" focused=").append(node.isFocused())
                .append(" desc=").append(node.getContentDescription()).append(" text=").append(text(node))
                .append(" selection=").append(node.getTextSelectionStart()).append(":").append(node.getTextSelectionEnd())
                .append(" bounds=").append(rect).append("\n");
            for (int i = 0; i < node.getChildCount(); i++) {
                AccessibilityNodeInfo child = node.getChild(i);
                if (child != null) queue.add(child);
            }
        }
        try (FileOutputStream out = new FileOutputStream(new File(directory, "hierarchy.txt"))) {
            out.write(tree.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }
    }
    private AccessibilityNodeInfo clickTarget(String label) {
        ArrayDeque<AccessibilityNodeInfo> queue = roots();
        java.util.List<AccessibilityNodeInfo> labels = new java.util.ArrayList<>();
        java.util.List<Rect> buttons = new java.util.ArrayList<>();
        java.util.List<Rect> editors = new java.util.ArrayList<>();
        while (!queue.isEmpty()) {
            AccessibilityNodeInfo node = queue.removeFirst();
            if (!node.refresh()) continue;
            if (node.isVisibleToUser()) {
                Rect bounds = new Rect(); node.getBoundsInScreen(bounds);
                boolean input = node.isEditable() || "android.widget.EditText".equals(String.valueOf(node.getClassName()));
                if (input) editors.add(bounds);
                else {
                    if (matches(node, label)) labels.add(node);
                    boolean clickable = node.isClickable();
                    for (AccessibilityNodeInfo.AccessibilityAction action : node.getActionList())
                        if (action.getId() == AccessibilityNodeInfo.ACTION_CLICK) clickable = true;
                    if (clickable && node.isEnabled()) buttons.add(bounds);
                }
            }
            for (int i = 0; i < node.getChildCount(); i++) {
                AccessibilityNodeInfo child = node.getChild(i);
                if (child != null) queue.add(child);
            }
        }
        AccessibilityNodeInfo best = null;
        long bestArea = Long.MAX_VALUE;
        for (AccessibilityNodeInfo node : labels) {
            Rect marker = new Rect(); node.getBoundsInScreen(marker);
            boolean inEditor = false;
            for (Rect input : editors) if (input.contains(marker.centerX(), marker.centerY())) inEditor = true;
            if (inEditor) continue;
            for (Rect button : buttons) {
                long area = (long) button.width() * button.height();
                if (area > 0 && area < bestArea && button.contains(marker.centerX(), marker.centerY())) {
                    // Compose can expose text and the clickable virtual node
                    // as siblings. Tap the actual label inside its hit area.
                    bestArea = area;
                    best = node;
                }
            }
        }
        return best;
    }
    private void click(String label) throws Exception {
        await(() -> clickTarget(label) != null, "clickable control: " + label);
        tap(clickTarget(label));
        waitForIdleSync();
        SystemClock.sleep(100);
    }
    private void createFixture(Context app) throws Exception {
        android.content.ContentValues values = new android.content.ContentValues();
        values.put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, FIXTURE);
        values.put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "text/plain");
        values.put(android.provider.MediaStore.MediaColumns.IS_PENDING, 1);
        fixtureUri = app.getContentResolver().insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
        require(fixtureUri != null, "create real fixture file");
        byte[] data = "Eymen Browser arayüz doğrulama dosyası.".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        try (java.io.OutputStream output = app.getContentResolver().openOutputStream(fixtureUri)) {
            require(output != null, "open real fixture output");
            output.write(data);
        }
        values.clear(); values.put(android.provider.MediaStore.MediaColumns.IS_PENDING, 0);
        app.getContentResolver().update(fixtureUri, values, null, null);
        new DownloadRegistry(app).record(new SavedDownload(fixtureUri.toString(), FIXTURE, "text/plain", data.length, System.currentTimeMillis()));
    }
    private void search(String label, String query) throws Exception {
        await(() -> field(label) != null, "search field: " + label);
        Bundle args = new Bundle();
        args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, query);
        require(field(label).performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args), "set search query");
        await(() -> query.equals(text(field(label))), "search query update");
    }
    private void tap(AccessibilityNodeInfo node) {
        Rect rect = new Rect(); node.getBoundsInScreen(rect);
        tapAt(rect.centerX(), rect.centerY());
    }
    private void tapAt(int x, int y) {
        long time = SystemClock.uptimeMillis();
        MotionEvent down = MotionEvent.obtain(time, time, MotionEvent.ACTION_DOWN, x, y, 0);
        down.setSource(InputDevice.SOURCE_TOUCHSCREEN);
        require(getUiAutomation().injectInputEvent(down, true), "touch down");
        down.recycle(); SystemClock.sleep(40);
        MotionEvent up = MotionEvent.obtain(time, SystemClock.uptimeMillis(), MotionEvent.ACTION_UP, x, y, 0);
        up.setSource(InputDevice.SOURCE_TOUCHSCREEN);
        require(getUiAutomation().injectInputEvent(up, true), "touch up"); up.recycle();
    }
    private void capture(String name, boolean light) throws Exception {
        waitForIdleSync(); SystemClock.sleep(250);
        Bitmap bitmap = getUiAutomation().takeScreenshot();
        require(bitmap != null, "real window screenshot");
        int pixel = bitmap.getPixel(8, bitmap.getHeight() / 2);
        require(light ? Color.red(pixel) > 180 && Color.green(pixel) > 180 : Color.red(pixel) < 80,
            "screen follows selected theme: " + name);
        File directory = new File(getTargetContext().getCacheDir(), "browser-ui-audit");
        require(directory.isDirectory() || directory.mkdirs(), "capture directory");
        try (FileOutputStream out = new FileOutputStream(new File(directory, name))) {
            require(bitmap.compress(Bitmap.CompressFormat.PNG, 100, out), "save screenshot");
        }
        bitmap.recycle();
    }
    private static void await(BooleanSupplier condition, String name) throws Exception {
        long until = SystemClock.elapsedRealtime() + 15000;
        while (!condition.getAsBoolean()) {
            if (SystemClock.elapsedRealtime() > until) throw new AssertionError("Timed out: " + name);
            Thread.sleep(60);
        }
    }
    private static void require(boolean value, String name) { if (!value) throw new AssertionError(name); }
}
