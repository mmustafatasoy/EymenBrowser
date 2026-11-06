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
            AccessibilityServiceInfo info = getUiAutomation().getServiceInfo();
            info.flags |= AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
                | AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS;
            getUiAutomation().setServiceInfo(info);
            activity = startActivitySync(new Intent(Intent.ACTION_VIEW, Uri.parse(OLD_URL), app, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            String address = label(R.string.address_edit_label);
            await(() -> field(address) != null && OLD_URL.equals(text(field(address))), "initial URL");
            tap(field(address));
            await(() -> selectedAll(field(address)), "first tap selects the complete URL");
            sendStringSync("eymen yeni arama");
            await(() -> "eymen yeni arama".equals(text(field(address))), "typing replaces the old address without deletion");
            Bundle caret = new Bundle();
            caret.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, 5);
            caret.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, 5);
            require(field(address).performAction(AccessibilityNodeInfo.ACTION_SET_SELECTION, caret), "normal caret action");
            await(() -> field(address).getTextSelectionStart() == 5, "caret remains editable after first focus");
            sendStringSync("x");
            await(() -> "eymenx yeni arama".equals(text(field(address))), "typing at a chosen caret");
            click(label(R.string.address_clear));
            await(() -> "".equals(text(field(address))), "clear address button");
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
            capture("downloads-light.png", true);
            click(label(R.string.download_filter_video));
            await(() -> find(label(R.string.download_filter_video), false).isSelected(), "real download type filter");
            search(label(R.string.download_search_hint), "olmayan-dosya");
            await(() -> find(label(R.string.download_no_matches), false) != null, "download search empty result");
            click(label(R.string.search_clear));
            click(label(R.string.back));
            await(() -> field(address) != null, "downloads back restores the browser");
            result.putString("eymen_browser_ui_audit", "passed");
            result.putString("checks", "tap_select_all,type_replaces_URL,caret_edit,clear,settings_search,persist_engine,persist_theme,custom_about,download_filter,download_search,back_navigation,light_dark_captures");
            code = Activity.RESULT_OK;
        } catch (Throwable error) {
            result.putString("eymen_browser_ui_audit", "failed");
            result.putString("stack", android.util.Log.getStackTraceString(error));
        } finally {
            if (activity != null) runOnMainSync(() -> activity.finish());
        }
        finish(code, result);
    }
    private android.content.SharedPreferences prefs() {
        return getTargetContext().getSharedPreferences("eymen_browser_setup", Context.MODE_PRIVATE);
    }
    private String label(int resource) { return activity.getString(resource); }
    private static String text(AccessibilityNodeInfo node) {
        return node == null || node.getText() == null ? "" : node.getText().toString();
    }
    private static boolean selectedAll(AccessibilityNodeInfo node) {
        return node != null && node.getTextSelectionStart() == 0
            && node.getTextSelectionEnd() == text(node).length() && !text(node).isEmpty();
    }
    private AccessibilityNodeInfo field(String label) { return find(label, true); }
    private AccessibilityNodeInfo find(String label, boolean editable) {
        ArrayDeque<AccessibilityNodeInfo> queue = new ArrayDeque<>();
        for (AccessibilityWindowInfo window : getUiAutomation().getWindows()) {
            if (window.getType() != AccessibilityWindowInfo.TYPE_APPLICATION) continue;
            AccessibilityNodeInfo root = window.getRoot();
            if (root != null) queue.add(root);
        }
        while (!queue.isEmpty()) {
            AccessibilityNodeInfo node = queue.removeFirst();
            String description = node.getContentDescription() == null ? "" : node.getContentDescription().toString();
            String value = text(node);
            if (node.isVisibleToUser() && (!editable || node.isEditable()) &&
                    (description.equals(label) || value.equals(label) || value.startsWith(label + "\n"))) return node;
            for (int i = 0; i < node.getChildCount(); i++) {
                AccessibilityNodeInfo child = node.getChild(i);
                if (child != null) queue.add(child);
            }
        }
        return null;
    }
    private void click(String label) throws Exception {
        await(() -> find(label, false) != null, "visible control: " + label);
        AccessibilityNodeInfo node = find(label, false);
        while (node != null && !node.isClickable()) node = node.getParent();
        require(node != null && node.performAction(AccessibilityNodeInfo.ACTION_CLICK), "click: " + label);
        waitForIdleSync();
        SystemClock.sleep(100);
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
        long time = SystemClock.uptimeMillis();
        MotionEvent down = MotionEvent.obtain(time, time, MotionEvent.ACTION_DOWN, rect.centerX(), rect.centerY(), 0);
        down.setSource(InputDevice.SOURCE_TOUCHSCREEN);
        require(getUiAutomation().injectInputEvent(down, true), "touch down");
        down.recycle(); SystemClock.sleep(40);
        MotionEvent up = MotionEvent.obtain(time, SystemClock.uptimeMillis(), MotionEvent.ACTION_UP, rect.centerX(), rect.centerY(), 0);
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
