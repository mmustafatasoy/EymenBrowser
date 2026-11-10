package com.eymen.browser.updateaudit;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.LocaleList;
import android.os.SystemClock;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.security.MessageDigest;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;
import org.json.JSONObject;

/** Outside the browser process: survives the actual signed package replacement. */
public final class SignedUpdateInstrumentation extends Instrumentation {
    private static final String BROWSER = "com.eymen.browser.telefon";
    private static final String DRIVER = "com.eymen.browser.updateaudit";
    private static final String SETTINGS = "com.android.settings";
    private Bundle args;
    private Resources browserResources;
    private long sourceCode, targetCode;
    private String targetTag, pinnedSigner, marker;
    private int width, height;

    @Override public void onCreate(Bundle arguments) { super.onCreate(arguments); args = arguments; start(); }

    @Override public void onStart() {
        Bundle result = new Bundle();
        int outcome = Activity.RESULT_CANCELED;
        try {
            sourceCode = Long.parseLong(args.getString("source_code"));
            targetCode = Long.parseLong(args.getString("target_code"));
            targetTag = args.getString("target_tag");
            pinnedSigner = args.getString("signer_sha256").toLowerCase(Locale.ROOT);
            require(sourceCode < targetCode && targetTag.matches("v[0-9]+\\.[0-9]+\\.[0-9]+"), "valid monotonic release pair");
            require(pinnedSigner.matches("[a-f0-9]{64}"), "pinned public signing fingerprint");
            require(Build.VERSION.SDK_INT >= 30 && ("ranchu".equals(Build.HARDWARE)
                || "goldfish".equals(Build.HARDWARE)), "supported isolated Android emulator only");
            marker = "https://example.com/?eymen-signed-update-audit=" + sourceCode + "-" + targetCode;
            AccessibilityServiceInfo service = getUiAutomation().getServiceInfo();
            service.flags |= AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
                | AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS;
            getUiAutomation().setServiceInfo(service);
            Bitmap display = getUiAutomation().takeScreenshot();
            require(display != null, "real emulator display");
            width = display.getWidth(); height = display.getHeight(); display.recycle();
            loadBrowserResources();
            verifyInstalled(sourceCode);

            launchBrowser();
            await(() -> find(label("start"), BROWSER) != null, 45000, "real production first-run screen");
            scrollClick("Bing", BROWSER);
            click(label("start"), BROWSER);
            await(() -> find(label("browser_menu"), BROWSER) != null, 45000, "first-run completed through UI");
            key(KeyEvent.KEYCODE_HOME); launchBrowser();
            awaitOffer(); capture("update-offer.png");
            click(label("update_later"), BROWSER);
            require(versionCode() == sourceCode, "declining update preserves the installed release");

            getTargetContext().startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(marker))
                .setPackage(BROWSER).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            await(() -> find(marker, BROWSER) != null, 45000, "real HTTPS marker opened by browser intent");
            menu("menu_bookmark_add");
            menu("bookmarks");
            await(() -> find(marker.substring("https://".length()), BROWSER) != null, 15000, "bookmark stored through UI before upgrade");
            capture("bookmarks-before.png"); key(KeyEvent.KEYCODE_BACK);
            openSettings();
            await(() -> find("Bing", BROWSER) != null, 15000, "chosen search engine before upgrade");
            capture("preferences-before.png");
            scrollClick(label("settings_about_update"), BROWSER);
            click(label("update_manual_check"), BROWSER);
            awaitOffer();
            click(label("update_download_install"), BROWSER);
            await(() -> find(label("update_downloading", targetTag), BROWSER) != null, 15000, "actual in-app APK download starts");
            capture("update-downloading.png");
            await(() -> {
                require(find(label("update_failed"), BROWSER) == null, "download, SHA-256 and package/signer verification succeed");
                return find(label("update_install_permission_title"), BROWSER) != null;
            }, 300000, "verified APK awaits Android source permission");
            require(versionCode() == sourceCode, "APK has not installed before system consent");
            capture("unknown-source-prompt.png");
            click(label("update_open_permissions"), BROWSER);
            await(() -> permissionSwitch() != null, 30000, "real Android unknown-source settings");
            AccessibilityNodeInfo permission = permissionSwitch();
            require(permission != null && !permission.isChecked(), "install source is not pre-granted");
            capture("unknown-source-settings.png"); tap(permission);
            await(() -> permissionSwitch() != null && permissionSwitch().isChecked(), 15000, "user grants source permission in Android UI");
            capture("unknown-source-granted.png"); key(KeyEvent.KEYCODE_BACK);
            await(() -> installerPositive() != null && installerPositive().isEnabled(), 45000, "native Android update confirmation");
            require(versionCode() == sourceCode, "native installer still waits for final approval");
            capture("install-confirmation.png"); tap(installerPositive());
            await(() -> versionCode() == targetCode, 120000, "real PackageInstaller replaces the signed app");
            verifyInstalled(targetCode);
            await(() -> installerOpen() != null && installerOpen().isEnabled(), 45000,
                "native installer offers Open after successful update");
            capture("install-complete.png");

            String installer = pm().getInstallSourceInfo(BROWSER).getInstallingPackageName();
            // Android may retain installer attribution across updates. The
            // actual approval, version transition and installed APK digest
            // establish this flow; attribution is recorded as diagnostics.
            tap(installerOpen());
            await(() -> find(label("browser_menu"), BROWSER) != null, 45000, "updated production app reopens without first-run setup");
            require(find(label("start"), BROWSER) == null, "setup state survives signed update");
            loadBrowserResources();
            openSettings();
            await(() -> find("Bing", BROWSER) != null, 15000, "search engine preference survives signed update");
            capture("preferences-after.png"); key(KeyEvent.KEYCODE_BACK);
            menu("bookmarks");
            await(() -> find(marker.substring("https://".length()), BROWSER) != null, 15000, "saved bookmark survives signed update");
            capture("bookmarks-after.png");
            click(marker.substring("https://".length()), BROWSER);
            await(() -> find(marker, BROWSER) != null, 15000, "persisted bookmark opens its exact original URL");
            openSettings(); scrollClick(label("settings_about_update"), BROWSER);
            click(label("update_manual_check"), BROWSER);
            await(() -> find(label("update_no_release"), BROWSER) != null, 90000, "same installed stable version is not offered again");
            capture("current-version.png");
            JSONObject metadata = new JSONObject().put("sourceVersionCode", sourceCode)
                .put("targetVersionCode", versionCode()).put("targetTag", targetTag)
                .put("signerSha256", pinnedSigner).put("installingPackage", String.valueOf(installer))
                .put("productionDebuggable", false).put("preferencePreserved", "Bing")
                .put("bookmarkPreserved", marker).put("physicalDeviceAcceptance", false);
            write("installed-metadata.json", metadata.toString(2));
            result.putString("eymen_signed_update_audit", "passed");
            result.putString("checks", "unmodified_production_APKs,foreground_offer,cancel_preserves_old_version,manual_check,real_APK_download,real_unknown_source_consent,native_installer_confirmation,production_signature,actual_version_upgrade,setup_search_bookmark_persistence,no_same_version_reinstall");
            outcome = Activity.RESULT_OK;
        } catch (Throwable error) {
            result.putString("eymen_signed_update_audit", "failed");
            java.io.StringWriter stack = new java.io.StringWriter(); error.printStackTrace(new PrintWriter(stack));
            result.putString("stack", stack.toString());
            try { capture("failure.png"); write("hierarchy.txt", hierarchy()); } catch (Throwable ignored) { }
        }
        finish(outcome, result);
    }

    private PackageManager pm() { return getTargetContext().getPackageManager(); }
    private PackageInfo installed() {
        try { return pm().getPackageInfo(BROWSER, PackageManager.GET_SIGNING_CERTIFICATES); }
        catch (Exception error) { throw new RuntimeException(error); }
    }
    private long versionCode() { return installed().getLongVersionCode(); }
    private void verifyInstalled(long code) throws Exception {
        PackageInfo info = installed();
        require(info.getLongVersionCode() == code, "expected installed release version");
        require((info.applicationInfo.flags & ApplicationInfo.FLAG_DEBUGGABLE) == 0, "real non-debuggable production browser");
        android.content.pm.Signature[] signers = info.signingInfo.getApkContentsSigners();
        require(signers.length == 1, "one production signer");
        StringBuilder digest = new StringBuilder();
        for (byte value : MessageDigest.getInstance("SHA-256").digest(signers[0].toByteArray()))
            digest.append(String.format(Locale.ROOT, "%02x", value & 0xff));
        require(pinnedSigner.equals(digest.toString()), "published production certificate matches pinned identity");
    }
    private String label(String key, Object... arguments) {
        int id = browserResources.getIdentifier(key, "string", BROWSER);
        require(id != 0, "production string resource: " + key);
        return browserResources.getString(id, arguments);
    }
    private void loadBrowserResources() throws Exception {
        Context browser = getTargetContext().createPackageContext(BROWSER, 0);
        Configuration config = new Configuration(browser.getResources().getConfiguration());
        config.setLocales(new LocaleList(Locale.US));
        browserResources = browser.createConfigurationContext(config).getResources();
    }
    private void launchBrowser() {
        Intent launch = pm().getLaunchIntentForPackage(BROWSER);
        require(launch != null, "published browser launcher");
        getTargetContext().startActivity(launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    }
    private void awaitOffer() {
        await(() -> {
            require(find(label("update_check_failed"), BROWSER) == null, "real GitHub release lookup succeeds");
            return find(label("update_available_title", targetTag), BROWSER) != null;
        }, 90000, "actual stable target release offered");
    }
    private void menu(String key) { click(label("browser_menu"), BROWSER); scrollClick(label(key), BROWSER); }
    private void openSettings() { menu("settings"); await(() -> find(label("menu_settings_search"), BROWSER) != null, 15000, "production settings"); }
    private List<AccessibilityNodeInfo> nodes() {
        ArrayDeque<AccessibilityNodeInfo> queue = new ArrayDeque<>();
        for (AccessibilityWindowInfo window : getUiAutomation().getWindows()) {
            AccessibilityNodeInfo root = window.getRoot(); if (root != null) queue.add(root);
        }
        List<AccessibilityNodeInfo> values = new ArrayList<>();
        while (!queue.isEmpty()) {
            AccessibilityNodeInfo node = queue.removeFirst();
            if (!node.refresh()) continue;
            if (node.isVisibleToUser()) values.add(node);
            for (int i=0;i<node.getChildCount();i++) { AccessibilityNodeInfo child=node.getChild(i); if(child!=null)queue.add(child); }
        }
        return values;
    }
    private boolean belongs(AccessibilityNodeInfo node, String pkg) {
        return node.getPackageName() != null && pkg.contentEquals(node.getPackageName());
    }
    private AccessibilityNodeInfo find(String caption, String pkg) {
        for (AccessibilityNodeInfo node : nodes()) if (belongs(node,pkg) &&
            (caption.contentEquals(node.getText()==null?"":node.getText()) ||
                caption.contentEquals(node.getContentDescription()==null?"":node.getContentDescription()))) return node;
        return null;
    }
    private AccessibilityNodeInfo permissionSwitch() {
        for (AccessibilityNodeInfo node : nodes()) if (belongs(node,SETTINGS) && node.isCheckable()) return node;
        return null;
    }
    private AccessibilityNodeInfo installerPositive() {
        for (AccessibilityNodeInfo node : nodes()) {
            String pkg=String.valueOf(node.getPackageName());
            if (pkg.equals("com.android.packageinstaller") || pkg.equals("com.google.android.packageinstaller")) {
                String id=node.getViewIdResourceName(),text=String.valueOf(node.getText());
                if ("android:id/button1".equals(id) || (pkg+":id/ok_button").equals(id)
                    || (pkg+":id/install_button").equals(id)
                    || "Update".equalsIgnoreCase(text) || "Install".equalsIgnoreCase(text)) return node;
            }
        }
        return null;
    }
    private AccessibilityNodeInfo installerOpen() {
        for (AccessibilityNodeInfo node : nodes()) {
            String pkg=String.valueOf(node.getPackageName());
            if ((pkg.equals("com.android.packageinstaller") || pkg.equals("com.google.android.packageinstaller"))
                && "Open".equalsIgnoreCase(String.valueOf(node.getText()))) return node;
        }
        return null;
    }
    private void click(String caption, String pkg) {
        await(() -> find(caption,pkg)!=null,15000,"visible action: "+caption);
        tap(find(caption,pkg));
    }
    private void scrollClick(String caption, String pkg) {
        for (int count=0;count<9;count++) {
            AccessibilityNodeInfo node=find(caption,pkg);
            if(node!=null){tap(node);return;}
            swipe();SystemClock.sleep(200);
        }
        throw new AssertionError("Action not found after scrolling: "+caption);
    }
    private void tap(AccessibilityNodeInfo node) {
        require(node!=null,"visible touch target");
        AccessibilityNodeInfo action=node;
        while(action!=null && !action.isClickable())action=action.getParent();
        require(action!=null && action.isEnabled(),"enabled clickable ancestor");
        Rect bounds=new Rect();action.getBoundsInScreen(bounds);
        long time=SystemClock.uptimeMillis();
        touch(time,time,MotionEvent.ACTION_DOWN,bounds.centerX(),bounds.centerY());
        SystemClock.sleep(60);touch(time,SystemClock.uptimeMillis(),MotionEvent.ACTION_UP,bounds.centerX(),bounds.centerY());
        SystemClock.sleep(180);
    }
    private void swipe() {
        long time=SystemClock.uptimeMillis();float x=width*.55f,from=height*.78f,to=height*.32f;
        touch(time,time,MotionEvent.ACTION_DOWN,x,from);
        for(int i=1;i<=16;i++){SystemClock.sleep(14);touch(time,SystemClock.uptimeMillis(),MotionEvent.ACTION_MOVE,x,from+(to-from)*i/16);}
        touch(time,SystemClock.uptimeMillis(),MotionEvent.ACTION_UP,x,to);
    }
    private void touch(long start,long now,int action,float x,float y) {
        MotionEvent event=MotionEvent.obtain(start,now,action,x,y,0);event.setSource(InputDevice.SOURCE_TOUCHSCREEN);
        require(getUiAutomation().injectInputEvent(event,true),"real touch event");event.recycle();
    }
    private void key(int code) {
        long now=SystemClock.uptimeMillis();
        require(getUiAutomation().injectInputEvent(new KeyEvent(now,now,KeyEvent.ACTION_DOWN,code,0),true),"key down");
        require(getUiAutomation().injectInputEvent(new KeyEvent(now,now+60,KeyEvent.ACTION_UP,code,0),true),"key up");
        SystemClock.sleep(200);
    }
    private void await(BooleanSupplier condition,long timeout,String stage) {
        long until=SystemClock.uptimeMillis()+timeout,notice=0;
        while(SystemClock.uptimeMillis()<until){
            if(condition.getAsBoolean())return;
            if(SystemClock.uptimeMillis()>notice){Bundle progress=new Bundle();progress.putString("stage",stage);sendStatus(0,progress);notice=SystemClock.uptimeMillis()+30000;}
            SystemClock.sleep(150);
        }
        throw new AssertionError("Timed out: "+stage);
    }
    private File directory() { File dir=new File(getTargetContext().getCacheDir(),"signed-update-audit");require(dir.isDirectory()||dir.mkdirs(),"audit directory");return dir; }
    private void capture(String name) throws Exception {
        Bitmap bitmap=getUiAutomation().takeScreenshot();require(bitmap!=null,"real screenshot");
        try(FileOutputStream output=new FileOutputStream(new File(directory(),name))){require(bitmap.compress(Bitmap.CompressFormat.PNG,100,output),"PNG encoding");}finally{bitmap.recycle();}
    }
    private void write(String name,String text) throws Exception {try(FileOutputStream output=new FileOutputStream(new File(directory(),name))){output.write(text.getBytes(java.nio.charset.StandardCharsets.UTF_8));}}
    private String hierarchy() {StringBuilder out=new StringBuilder();for(AccessibilityNodeInfo n:nodes())out.append(n.getPackageName()).append(" ").append(n.getClassName()).append(" id=").append(n.getViewIdResourceName()).append(" text=").append(n.getText()).append(" desc=").append(n.getContentDescription()).append('\n');return out.toString();}
    private static void require(boolean condition,String description){if(!condition)throw new AssertionError(description);}
}
