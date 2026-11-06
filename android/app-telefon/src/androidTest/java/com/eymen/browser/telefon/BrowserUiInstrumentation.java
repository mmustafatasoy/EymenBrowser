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
            result.putString("eymen_browser_ui_audit", "passed");
            result.putString("checks", "real_popup_open_write_opener_close,tap_select_all,type_replaces_URL,caret_edit,clear,settings_search,persist_engine,persist_theme,custom_about,download_filter,download_search,confirm_clear_keeps_file,back_navigation,home_new_tab_reset,light_dark_captures");
            code = Activity.RESULT_OK;
        } catch (Throwable error) {
            try { recordFailure(); } catch (Throwable ignored) {}
            result.putString("eymen_browser_ui_audit", "failed");
            result.putString("stack", android.util.Log.getStackTraceString(error));
        } finally {
            if (activity != null) runOnMainSync(() -> activity.finish());
            if (fixtureUri != null) getTargetContext().getContentResolver().delete(fixtureUri, null, null);
            new DownloadRegistry(getTargetContext()).clearListOnly();
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
        require(currentSession().isOpen(), "Gecko opened the popup session before the view attached");
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
