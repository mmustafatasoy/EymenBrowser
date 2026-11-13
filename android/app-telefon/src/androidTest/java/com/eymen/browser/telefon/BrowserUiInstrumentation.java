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
    private Uri vpnFixtureUri;
    private final java.util.List<Uri> libraryFixtures=new java.util.ArrayList<>();
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
            app.getSharedPreferences("eymen_browser_bookmarks", Context.MODE_PRIVATE).edit()
                .putStringSet("urls", new java.util.HashSet<>(java.util.Arrays.asList("https://example.net/legacy-bookmark"))).commit();
            app.deleteDatabase("browser-bookmarks.db");
            try(android.database.sqlite.SQLiteDatabase db=app.openOrCreateDatabase("browser-bookmarks.db",Context.MODE_PRIVATE,null)) {
                db.execSQL("CREATE TABLE folders (id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT NOT NULL UNIQUE COLLATE NOCASE)");
                db.execSQL("CREATE TABLE bookmarks (id INTEGER PRIMARY KEY AUTOINCREMENT,url TEXT NOT NULL UNIQUE,title TEXT NOT NULL,folder_id INTEGER REFERENCES folders(id) ON DELETE SET NULL)");
                db.execSQL("CREATE TABLE metadata (key TEXT PRIMARY KEY)");
                db.execSQL("INSERT INTO bookmarks(url,title) VALUES(?,?)",new Object[]{"https://example.org/v1-library","V1 kitaplık başlığı"});db.setVersion(1);
            }
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
            Rect firstAddress=new Rect();field(address).getBoundsInScreen(firstAddress);
            tapAt(firstAddress.centerX(),firstAddress.centerY(),180);
            await(() -> field(address) != null && field(address).isFocused(), "address receives focus after touch");
            await(this::softwareKeyboardVisible,"the real on-screen keyboard is visible for address entry");
            waitForIdleSync();
            SystemClock.sleep(100);
            android.view.inputmethod.EditorInfo initialInfo=new android.view.inputmethod.EditorInfo();
            android.view.inputmethod.InputConnection composingConnection=addressConnection(initialInfo);
            require(initialInfo.initialSelStart==0&&initialInfo.initialSelEnd==text(field(address)).length(),"a physical tap longer than one frame selects the entire native IME address");
            require(uiCheck(()->composingConnection.setComposingText("eymen yeni arama",1)),"native IME composing text accepted");
            await(() -> "eymen yeni arama".equals(text(field(address))), "typing replaces the old address without deletion");
            // A Go -> Search IME-options change restarts Android input. Real
            // keyboards bind the fresh connection; the previous one is closed.
            android.view.inputmethod.EditorInfo searchInfo=new android.view.inputmethod.EditorInfo();
            android.view.inputmethod.InputConnection activeComposition=addressConnection(searchInfo);
            require((searchInfo.imeOptions&android.view.inputmethod.EditorInfo.IME_MASK_ACTION)==android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH,"plain text exposes a real Search keyboard action");
            require(uiCheck(activeComposition::finishComposingText),"native IME composition committed through the current connection");
            await(()->softwareKeyboardVisible()&&"eymen yeni arama".equals(text(field(address))),"Search action preserves the query and the real on-screen keyboard");
            capture("address-search-ime.png",true);
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
            scrollClick(label(R.string.settings));
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
            await(() -> find(label(R.string.download_confirm_deny), false) == null, "cancel closes the actual confirmation window");
            require(!new DownloadRegistry(app).items().isEmpty(), "cancel clear keeps history");
            click(label(R.string.download_clear_records));
            await(() -> find(label(R.string.download_clear_confirm), false) != null, "custom clear confirmation is visible before approval");
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
            navigationAndSearch();
            realHistoryAndScrolling();
            polishedSharingAndInformation();
            openTabSearch();
            bookmarkLibrary();
            mediaAndSystemTheme();
            libraryTransferAndReading();
            privateLockWithRealAndroidCredential();
            secureDnsAndNativePrint();
            fullPageCaptureAndDarkPages();
            inactiveTabKeepsFormAndSession();
            freeVpnSetup();
            advancedSettings();
            result.putString("eymen_browser_ui_audit", "passed");
            result.putString("checks", "real_popup_open_write_opener_close_and_system_back,address_held_tap_select_all_native_IME_composition_search_go,real_same_tab_back_forward_system_and_menu_SPA_location,first_page_home_back_forward,keyboard_back_cancels_address_edit,settings_search_engine_theme,real_download_filter_clear,home_new_tab,real_cookie_reject,real_cookie_storage_clear,real_runtime_privacy,android_autofill_toggle,signed_addon_install_toggle_remove,parent_PIN_allowlist_private_child,local_QR_bitmap_decode,actual_language_recreation,native_browser_role_http_filter_and_cancel,real_persistent_history_private_exclusion_search_delete_clear,animated_top_bottom_toolbar_tiny_reverse,fixed_footer_hit_test,native_list_menu_settings,share_exact_URL_copy_sensitive_clipboard_QR_PNG_decode_and_system_chooser,offline_license_search_read_back_stack,about_website_privacy_support_and_reviewed_email,styled_native_confirmation_light_dark,open_tab_search_real_title_URL_private_isolation_clear_reset_no_hidden_close_light_dark,bookmarks_legacy_migration_title_folder_edit_move_delete_preserves_links_SQLite_and_local_suggestions_private_exclusion,real_Gecko_VP8_Vorbis_background_time_progress_native_media_pause_resume_stop_private_metadata_and_system_PiP_theme,real_ContentResolver_JSON_HTML_import_export_transaction_rollback_SQLite_v1_upgrade_and_reading_list,real_Android_Keyguard_enable_cancel_unlock_disable_FLAG_SECURE_and_detached_private_session,real_Gecko_DoH_runtime_modes_and_Android_PrintSpooler_preview_cancel,real_full_document_PNG_export_bottom_pixels_and_opt_in_dark_page_style,real_inactive_Gecko_tab_visibility_memory_cycle_form_and_RAM_state_preserved,free_VPN_official_setup_confirmed_download_import_cancel_Keystore_browser_only_no_auto_connect");
            code = Activity.RESULT_OK;
        } catch (Throwable error) {
            try { recordFailure(); } catch (Throwable ignored) {}
            result.putString("eymen_browser_ui_audit", "failed");
            result.putString("stack", android.util.Log.getStackTraceString(error));
        } finally {
            if (activity != null) runOnMainSync(() -> activity.finish());
            for(Uri uri:libraryFixtures)getTargetContext().getContentResolver().delete(uri,null,null);
            if (fixtureUri != null) getTargetContext().getContentResolver().delete(fixtureUri, null, null);
            if (vpnFixtureUri != null) {
                getTargetContext().getContentResolver().delete(vpnFixtureUri, null, null);
                new EymenVpnConfigStore(getTargetContext()).clear();
            }
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
        click(label(R.string.browser_menu)); scrollClick(label(R.string.settings));
        search(label(R.string.settings_search_hint), label(labelId)); click(label(labelId));
    }
    private android.view.inputmethod.InputConnection addressConnection(android.view.inputmethod.EditorInfo info) {
        java.util.concurrent.atomic.AtomicReference<android.view.inputmethod.InputConnection> result=new java.util.concurrent.atomic.AtomicReference<>();
        runOnMainSync(()->{
            android.view.View target=activity.getCurrentFocus();
            if(target!=null)result.set(target.onCreateInputConnection(info));
        });
        require(result.get()!=null,"native address InputConnection exists");return result.get();
    }
    private boolean softwareKeyboardVisible() {
        for(AccessibilityWindowInfo window:getUiAutomation().getWindows())if(window.getType()==AccessibilityWindowInfo.TYPE_INPUT_METHOD){
            Rect bounds=new Rect();window.getBoundsInScreen(bounds);
            AccessibilityNodeInfo root=window.getRoot();
            if(root!=null&&root.refresh()&&root.isVisibleToUser()&&bounds.height()>getTargetContext().getResources().getDisplayMetrics().density*100)return true;
        }
        return false;
    }
    private void menuNavigation(int labelId) throws Exception {
        click(label(R.string.browser_menu));click(label(labelId));
    }
    private boolean homeVisible(){return uiCheck(()->Boolean.TRUE.equals(((androidx.compose.runtime.MutableState<?>)controller("atHome")).getValue()));}
    private void systemBack() throws Exception {
        waitForIdleSync();long time=SystemClock.uptimeMillis();
        int flags=KeyEvent.FLAG_FROM_SYSTEM|KeyEvent.FLAG_VIRTUAL_HARD_KEY;
        require(getUiAutomation().injectInputEvent(new KeyEvent(time,time,KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_BACK,0,0,
            android.view.KeyCharacterMap.VIRTUAL_KEYBOARD,0,flags,InputDevice.SOURCE_KEYBOARD),true),"system Back down");
        require(getUiAutomation().injectInputEvent(new KeyEvent(time,SystemClock.uptimeMillis(),KeyEvent.ACTION_UP,KeyEvent.KEYCODE_BACK,0,0,
            android.view.KeyCharacterMap.VIRTUAL_KEYBOARD,0,flags,InputDevice.SOURCE_KEYBOARD),true),"system Back up");
        waitForIdleSync();SystemClock.sleep(200);
    }
    private void navigationAndSearch() throws Exception {
        String address=label(R.string.address_edit_label);
        tap(field(address));
        await(()->field(address)!=null&&field(address).isFocused(),"new-tab address is focused for native search");
        if(!text(field(address)).isEmpty())click(label(R.string.address_clear));
        android.view.inputmethod.EditorInfo info=new android.view.inputmethod.EditorInfo();
        android.view.inputmethod.InputConnection input=addressConnection(info);
        final android.view.inputmethod.InputConnection composingInput=input;
        require(uiCheck(()->composingInput.setComposingText("eymen ime arama",1)),"native query composition");
        await(()->"eymen ime arama".equals(text(field(address))),"native search query visible");
        input=addressConnection(info);
        require((info.imeOptions&android.view.inputmethod.EditorInfo.IME_MASK_ACTION)==android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH,"Search action is available on the real input connection");
        final android.view.inputmethod.InputConnection activeInput=input;
        require(uiCheck(activeInput::finishComposingText),"query composition finished on the current input connection");
        org.mozilla.geckoview.GeckoSession searchSession=currentSession();
        java.util.concurrent.atomic.AtomicBoolean searched=new java.util.concurrent.atomic.AtomicBoolean();
        runOnMainSync(()->{
            org.mozilla.geckoview.GeckoSession.ProgressDelegate original=searchSession.getProgressDelegate();
            searchSession.setProgressDelegate(new org.mozilla.geckoview.GeckoSession.ProgressDelegate(){
                @Override public void onPageStart(org.mozilla.geckoview.GeckoSession s,String url){original.onPageStart(s,url);if(url.startsWith("https://www.bing.com/search?")&&url.contains("eymen+ime+arama"))searched.set(true);}
                @Override public void onPageStop(org.mozilla.geckoview.GeckoSession s,boolean success){original.onPageStop(s,success);}
            });
        });
        final android.view.inputmethod.InputConnection searchInput=input;
        require(uiCheck(()->searchInput.performEditorAction(android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH)),"native keyboard Search submitted");
        await(searched::get,"keyboard Search starts the selected real search engine in Gecko");
        menuNavigation(R.string.menu_new_normal_tab_accessibility);
        tap(field(address));
        await(()->field(address)!=null&&field(address).isFocused(),"new-tab address is focused for native Go");
        search(address,"https://example.com/?eymen-navigation=first");
        android.view.inputmethod.EditorInfo goInfo=new android.view.inputmethod.EditorInfo();
        android.view.inputmethod.InputConnection goInput=addressConnection(goInfo);
        require((goInfo.imeOptions&android.view.inputmethod.EditorInfo.IME_MASK_ACTION)==android.view.inputmethod.EditorInfo.IME_ACTION_GO,"web addresses expose the real Go action");
        require(uiCheck(()->goInput.performEditorAction(android.view.inputmethod.EditorInfo.IME_ACTION_GO)),"native keyboard Go submitted");
        BrowserHistory history=(BrowserHistory)controller("history");
        String first="https://example.com/?eymen-navigation=first",second="https://example.com/?eymen-navigation=second";
        await(()->uiCheck(()->history.getEntries().stream().anyMatch(e->e.getUrl().equals(first)&&e.getTitle().equals("Example Domain"))),"first page really completed");
        menuNavigation(R.string.back);await(this::homeVisible,"Back from a first page reaches native home instead of a no-op");
        menuNavigation(R.string.forward);await(()->!homeVisible()&&first.equals(text(field(address))),"Forward from native home restores its actual page");
        click(label(R.string.home));await(this::homeVisible,"explicit home button");systemBack();
        await(()->!homeVisible()&&first.equals(text(field(address))),"system Back after explicit Home returns to the page");
        org.mozilla.geckoview.GeckoSession session=currentSession();
        java.util.concurrent.atomic.AtomicReference<String> title=new java.util.concurrent.atomic.AtomicReference<>("");
        runOnMainSync(()->{
            org.mozilla.geckoview.GeckoSession.ContentDelegate original=session.getContentDelegate();
            session.setContentDelegate(new org.mozilla.geckoview.GeckoSession.ContentDelegate(){
                @Override public void onTitleChange(org.mozilla.geckoview.GeckoSession s,String value){title.set(value);original.onTitleChange(s,value);}
            });
            session.loadUri("javascript:void(function(){document.body.innerHTML="+org.json.JSONObject.quote("<a href='"+second+"' style='position:fixed;inset:0;display:grid;place-items:center'>Eymen next page</a>")+";document.title='Eymen navigation ready';}())");
        });
        await(()->"Eymen navigation ready".equals(title.get()),"actual page link ready");tapBrowserCenter();
        await(()->uiCheck(()->history.getEntries().stream().anyMatch(e->e.getUrl().equals(second)&&e.getTitle().equals("Example Domain"))),"a real link opened the second page in the same tab");
        require(currentSession()==session,"normal links keep the same real Gecko history");
        menuNavigation(R.string.back);await(()->first.equals(text(field(address))),"menu Back returns to the first real site");
        menuNavigation(R.string.forward);await(()->second.equals(text(field(address))),"menu Forward returns to the second real site");
        await(()->"Example Domain".equals(title.get()),"second page document ready again");
        String spaHtml="<button style='position:fixed;inset:0;width:100%;height:100%' onclick=\"history.pushState({},'',location.href+'#native-step');document.title='Eymen SPA step ready';\">Eymen SPA step</button>";
        String spaScript="javascript:void(function(){document.body.innerHTML="+org.json.JSONObject.quote(spaHtml)+";document.title='Eymen SPA ready';}())";
        runOnMainSync(()->session.loadUri(spaScript));
        await(()->"Eymen SPA ready".equals(title.get()) && find("Eymen SPA step",false)!=null,"user-activated SPA history fixture is laid out and accessible");
        click("Eymen SPA step");
        await(()->text(field(address)).equals(second+"#native-step"),"same-document location updates the address without PageStart");
        menuNavigation(R.string.back);await(()->second.equals(text(field(address))),"menu Back updates the SPA address");
        menuNavigation(R.string.forward);await(()->(second+"#native-step").equals(text(field(address))),"menu Forward updates the SPA address");
        systemBack();await(()->second.equals(text(field(address))),"Android Back handles same-document history");
        systemBack();await(()->first.equals(text(field(address))),"Android Back returns to the previous real website");
        tap(field(address));sendStringSync("unfinished address");systemBack();
        await(()->first.equals(text(field(address)))&&!homeVisible(),"keyboard Back cancels editing without leaving the page");
        capture("navigation-restored.png",true);
        menuNavigation(R.string.menu_new_normal_tab_accessibility);
        org.mozilla.geckoview.GeckoSession blank=currentSession();
        systemBack();await(()->currentSession()==session,"Back from a new empty tab returns to the actual previous tab");
        require(uiCheck(()->!blank.isOpen()),"only the new empty tab closed");
        menuNavigation(R.string.menu_new_normal_tab_accessibility);
        menuNavigation(R.string.back);await(()->currentSession()==session,"menu Back also returns from a new empty tab");
        menuNavigation(R.string.menu_new_normal_tab_accessibility);
    }
    private void realHistoryAndScrolling() throws Exception {
        BrowserHistory history=(BrowserHistory)controller("history");
        Intent webIntent=new Intent(Intent.ACTION_VIEW,Uri.parse("http://example.com/?eymen-browser-role=1")).addCategory(Intent.CATEGORY_BROWSABLE);
        // Inspect our installed filter independently of Android's current default browser.
        Intent ownWebIntent=new Intent(webIntent).setPackage(getTargetContext().getPackageName());
        java.util.List<android.content.pm.ResolveInfo> handlers=getTargetContext().getPackageManager().queryIntentActivities(
            ownWebIntent,android.content.pm.PackageManager.MATCH_ALL|android.content.pm.PackageManager.MATCH_DEFAULT_ONLY
                |android.content.pm.PackageManager.GET_RESOLVED_FILTER);
        require(handlers.stream().anyMatch(info->getTargetContext().getPackageName().equals(info.activityInfo.packageName)
            &&info.filter!=null&&info.filter.hasDataScheme("http")&&info.filter.countDataAuthorities()==0
            &&info.filter.countDataPaths()==0),"installed HTTP filter handles all web hosts: "+handlers);
        require("http://example.com/?eymen-browser-role=1".equals(com.eymen.browser.motor.GeckoMotorDenemesi.INSTANCE.urlFromIntent(webIntent)),"safe external HTTP link reaches browser routing without being dropped");
        require(com.eymen.browser.motor.GeckoMotorDenemesi.INSTANCE.urlFromIntent(new Intent(Intent.ACTION_VIEW,Uri.parse("javascript:alert(1)")))==null,"non-web external intent cannot enter browser routing");

        openSettingsItem(R.string.settings_default_browser);
        await(()->systemBrowserChoice()!=null,"real Android browser-role dialog lists this app");
        // The system dialog follows Android's theme and dim amount, not the app palette.
        capture("default-browser-system.png",null);
        await(()->systemCancelButton()!=null,"Android role dialog cancel button");
        tap(systemCancelButton());waitForIdleSync();
        await(()->systemBrowserChoice()==null&&field(label(R.string.settings_search_hint))!=null,"cancel returns to settings without changing the default");
        click(label(R.string.back));

        String first="https://example.com/?eymen-history=a";
        String second="https://example.com/?eymen-history=b";
        org.mozilla.geckoview.GeckoSession session=currentSession();
        runOnMainSync(()->session.loadUri(first));
        await(()->uiCheck(()->history.getEntries().stream().anyMatch(e->e.getUrl().equals(first)&&e.getTitle().equals("Example Domain"))),"first completed HTTPS title really stored");
        runOnMainSync(()->session.loadUri(second));
        await(()->uiCheck(()->history.getEntries().stream().anyMatch(e->e.getUrl().equals(second))),"second completed HTTPS visit stored");
        try(android.database.sqlite.SQLiteDatabase db=android.database.sqlite.SQLiteDatabase.openDatabase(
            getTargetContext().getDatabasePath("browser-history.db").getPath(),null,android.database.sqlite.SQLiteDatabase.OPEN_READONLY);
            android.database.Cursor rows=db.rawQuery("SELECT title,visited_at FROM visits WHERE url=?",new String[]{first})){
            require(rows.moveToFirst()&&"Example Domain".equals(rows.getString(0))&&rows.getLong(1)>0,"real title and visit time persisted to SQLite");
        }
        click(label(R.string.browser_menu));capture("menu-native.png",true);click(label(R.string.history));
        await(()->field(label(R.string.history_search_hint))!=null,"real full-page history list");
        await(()->find("example.com/?eymen-history=a",false)!=null,"history shows actual URL rows");
        require(find(label(R.string.history_today),false)!=null,"visits grouped under their real local day");
        capture("history-light.png",true);
        search(label(R.string.history_search_hint),"eymen-history=b");
        await(()->find("example.com/?eymen-history=a",false)==null&&find("example.com/?eymen-history=b",false)!=null,"history searches real titles and URLs");
        click(label(R.string.search_clear));
        scrollClick(activity.getString(R.string.history_delete_item,second));
        await(()->uiCheck(()->history.getEntries().stream().noneMatch(e->e.getUrl().equals(second))),"delete action removes an actual database visit");
        click("example.com/?eymen-history=a");
        await(()->field(label(R.string.address_edit_label))!=null&&text(field(label(R.string.address_edit_label))).contains("eymen-history=a"),"tapping a history link navigates the live browser");

        click(label(R.string.browser_menu));click(label(R.string.private_tab));
        org.mozilla.geckoview.GeckoSession secret=currentSession();
        java.util.concurrent.atomic.AtomicBoolean privateLoaded=new java.util.concurrent.atomic.AtomicBoolean();
        java.util.concurrent.atomic.AtomicBoolean privateLoading=new java.util.concurrent.atomic.AtomicBoolean();
        runOnMainSync(()->{
            org.mozilla.geckoview.GeckoSession.ProgressDelegate original=secret.getProgressDelegate();
            secret.setProgressDelegate(new org.mozilla.geckoview.GeckoSession.ProgressDelegate(){
                @Override public void onPageStart(org.mozilla.geckoview.GeckoSession s,String url){privateLoading.set(url.contains("eymen-private-history=1"));original.onPageStart(s,url);}
                @Override public void onPageStop(org.mozilla.geckoview.GeckoSession s,boolean success){original.onPageStop(s,success);if(privateLoading.getAndSet(false)&&success)privateLoaded.set(true);}
            });
            secret.loadUri("https://example.com/?eymen-private-history=1");
        });
        await(privateLoaded::get,"private page really completed in Gecko");
        require(uiCheck(()->history.getEntries().stream().noneMatch(e->e.getUrl().contains("eymen-private-history"))),"private navigation excluded from visible history");
        try(android.database.sqlite.SQLiteDatabase db=android.database.sqlite.SQLiteDatabase.openDatabase(
            getTargetContext().getDatabasePath("browser-history.db").getPath(),null,android.database.sqlite.SQLiteDatabase.OPEN_READONLY);
            android.database.Cursor rows=db.rawQuery("SELECT COUNT(*) FROM visits WHERE url LIKE '%eymen-private-history%'",null)){
            require(rows.moveToFirst()&&rows.getInt(0)==0,"private URL never reached SQLite");
        }
        click(label(R.string.browser_menu));click(label(R.string.menu_new_normal_tab_accessibility));
        require(!currentSession().getSettings().getUsePrivateMode(),"explicit new tab returns to normal context");
        org.mozilla.geckoview.GeckoSession longPage=currentSession();
        java.util.concurrent.atomic.AtomicReference<String> title=new java.util.concurrent.atomic.AtomicReference<>("");
        runOnMainSync(()->{
            org.mozilla.geckoview.GeckoSession.ContentDelegate original=longPage.getContentDelegate();
            longPage.setContentDelegate(new org.mozilla.geckoview.GeckoSession.ContentDelegate(){
                @Override public void onTitleChange(org.mozilla.geckoview.GeckoSession s,String value){title.set(value);original.onTitleChange(s,value);}
            });
            longPage.loadUri("https://example.com/?eymen-scroll=1");
        });
        await(()->"Example Domain".equals(title.get())&&uiCheck(()->history.getEntries().stream().anyMatch(e->e.getUrl().contains("eymen-scroll=1"))),"completed real HTTPS document for native scrolling");
        String html="<main style='height:7000px;background:linear-gradient(white,lightgray)'><h1>Eymen scrolling test</h1><p>Real page movement</p></main><button id='footer' style='position:fixed;bottom:0;left:0;width:100%;height:48px'>Eymen footer action</button>";
        String fixture="javascript:void(function(){document.documentElement.style.cssText='overflow-y:auto;height:auto';document.body.style.cssText='display:block;width:100%;max-width:none;height:auto;min-height:0;margin:0;padding:0;overflow:visible';document.body.innerHTML="+org.json.JSONObject.quote(html)+";document.getElementById('footer').onclick=function(){document.title='Eymen footer tapped';};requestAnimationFrame(function(){requestAnimationFrame(function(){document.title='Eymen scroll ready';});});}())";
        runOnMainSync(()->longPage.loadUri(fixture));
        await(()->"Eymen scroll ready".equals(title.get()),"long web page injected only into the test document");
        await(()->find("Eymen scrolling test",false)!=null&&find("Eymen footer action",false)!=null,"long test document is painted and accessible before touch scrolling");
        int engineHeight=uiInt(()->{try{return browserView().getHeight();}catch(Exception e){throw new RuntimeException(e);}});
        swipeUp();
        await(()->uiCheck(()->chromeView().getVisibility()==android.view.View.INVISIBLE),"scrolling down hides the complete top address/menu bar");
        require(engineHeight==uiInt(()->{try{return browserView().getHeight();}catch(Exception e){throw new RuntimeException(e);}}),"toolbar animation preserves the engine measured height");
        capture("scroll-hidden.png",true);
        tinyReverseSwipe();
        await(()->uiCheck(()->chromeView().getVisibility()==android.view.View.VISIBLE&&Math.abs(chromeView().getTranslationY())<1f),"eight-pixel upward reversal reveals top controls");
        capture("scroll-visible.png",true);
        openSettingsItem(R.string.settings_address);click(label(R.string.onboarding_bottom));
        require(prefs().getBoolean("address_at_bottom",false),"bottom address choice persisted in light theme");
        click(label(R.string.back));click(label(R.string.back));
        swipeUp();await(()->uiCheck(()->chromeView().getVisibility()==android.view.View.INVISIBLE),"bottom toolbar also hides on downward page scrolling");
        tinyReverseSwipe();await(()->uiCheck(()->chromeView().getVisibility()==android.view.View.VISIBLE&&Math.abs(chromeView().getTranslationY())<1f),"bottom toolbar returns on the same tiny reversal");
        capture("scroll-bottom.png",true);
        await(()->find("Eymen footer action",false)!=null,"real fixed page footer remains accessible");
        tap(find("Eymen footer action",false));
        await(()->"Eymen footer tapped".equals(title.get()),"fixed footer hit testing stays correct above visible bottom controls");
        openSettingsItem(R.string.settings_address);click(label(R.string.onboarding_top));click(label(R.string.back));click(label(R.string.back));

        openSettingsItem(R.string.settings_appearance);click(label(R.string.theme_night));click(label(R.string.back));
        click(label(R.string.search_clear));capture("settings-native-dark.png",false);click(label(R.string.back));
        click(label(R.string.browser_menu));click(label(R.string.history));capture("history-dark.png",false);
        click(label(R.string.history_clear));
        await(()->nativePositive()!=null,"native styled history confirmation");capture("confirmation-dark.png",null);clickNativePositive();
        await(()->uiCheck(()->history.getEntries().isEmpty()&&!history.getLoading()),"clear history empties real persistent records");
        require(find(label(R.string.history_empty),false)!=null,"cleared history displays an honest empty list");
        click(label(R.string.back));
        openSettingsItem(R.string.settings_appearance);click(label(R.string.theme_day));click(label(R.string.back));click(label(R.string.back));
    }
    private android.view.View chromeView(){return (android.view.View)controller("chrome");}
    private int uiInt(java.util.function.IntSupplier value){java.util.concurrent.atomic.AtomicInteger result=new java.util.concurrent.atomic.AtomicInteger();runOnMainSync(()->result.set(value.getAsInt()));return result.get();}
    private void tinyReverseSwipe(){
        Bitmap bitmap=getUiAutomation().takeScreenshot();float x=bitmap.getWidth()*0.6f,y=bitmap.getHeight()*0.5f;bitmap.recycle();
        long down=SystemClock.uptimeMillis();
        for(int i=0;i<=5;i++){
            int action=i==0?MotionEvent.ACTION_DOWN:i==5?MotionEvent.ACTION_UP:MotionEvent.ACTION_MOVE;
            MotionEvent event=MotionEvent.obtain(down,SystemClock.uptimeMillis(),action,x,y+8f*i/5f,0);
            event.setSource(InputDevice.SOURCE_TOUCHSCREEN);getUiAutomation().injectInputEvent(event,true);event.recycle();SystemClock.sleep(25);
        }
        waitForIdleSync();SystemClock.sleep(250);
    }
    private void polishedSharingAndInformation() throws Exception {
        String expected="https://example.com/?eymen-share=1#section";
        org.mozilla.geckoview.GeckoSession page=currentSession();
        java.util.concurrent.atomic.AtomicReference<String> title=new java.util.concurrent.atomic.AtomicReference<>("");
        runOnMainSync(()->{
            org.mozilla.geckoview.GeckoSession.ContentDelegate original=contentDelegate(page);
            page.setContentDelegate(new org.mozilla.geckoview.GeckoSession.ContentDelegate(){
                @Override public void onTitleChange(org.mozilla.geckoview.GeckoSession session,String value){title.set(value);original.onTitleChange(session,value);}
            });page.loadUri(expected);
        });
        await(()->title.get()!=null&&title.get().contains("Example Domain")&&expected.equals(text(field(label(R.string.address_edit_label)))),"real HTTPS page ready for sharing");
        BrowserShareController sharing=(BrowserShareController)controller("sharing");
        openShare();
        await(()->find(label(R.string.share_copy_link),false)!=null,"custom share actions visible");
        require(uiCheck(()->sharing.getPage()!=null&&expected.equals(sharing.getPage().getUrl())&&!sharing.getPage().getPrivateMode()),"share preview captures the actual selected normal page");
        require(find(expected,false)!=null,"exact URL is visible in share preview");
        capture("share-light.png",null);
        click(label(R.string.share_copy_link));
        await(()->uiCheck(()->sharing.getPage()==null),"copy closes the share panel");
        android.content.ClipboardManager clipboard=getTargetContext().getSystemService(android.content.ClipboardManager.class);
        require(uiCheck(()->clipboard.getPrimaryClip()!=null&&expected.contentEquals(clipboard.getPrimaryClip().getItemAt(0).getText())),"copy preserves the complete real URL including query and fragment");
        openShare();click(label(R.string.share_qr));
        await(()->uiCheck(()->sharing.getQrBitmap()!=null),"offline QR generation completes");
        capture("share-qr-light.png",null);
        java.util.concurrent.atomic.AtomicReference<Intent> imageIntent=new java.util.concurrent.atomic.AtomicReference<>();
        java.util.concurrent.atomic.AtomicReference<Intent> textIntent=new java.util.concurrent.atomic.AtomicReference<>();
        runOnMainSync(()->{imageIntent.set(sharing.qrIntent(sharing.getPage()));textIntent.set(sharing.textIntent(sharing.getPage()));});
        Intent image=imageIntent.get();Uri imageUri=image.getParcelableExtra(Intent.EXTRA_STREAM,Uri.class);
        require(Intent.ACTION_SEND.equals(image.getAction())&&"image/png".equals(image.getType())&&imageUri!=null&&"content".equals(imageUri.getScheme()),"QR share uses an Android content URI with the correct image MIME type");
        require((image.getFlags()&Intent.FLAG_GRANT_READ_URI_PERMISSION)!=0&&(image.getFlags()&Intent.FLAG_GRANT_WRITE_URI_PERMISSION)==0&&image.getClipData()!=null&&imageUri.equals(image.getClipData().getItemAt(0).getUri()),"QR shares grant only read access to the explicitly shared image");
        File decoded=new File(getTargetContext().getCacheDir(),"share-qr-decode.png");
        try(java.io.InputStream input=getTargetContext().getContentResolver().openInputStream(imageUri);FileOutputStream output=new FileOutputStream(decoded)){
            require(input!=null,"shared QR image is readable");byte[] buffer=new byte[8192];int count;while((count=input.read(buffer))!=-1)output.write(buffer,0,count);
        }
        try{require(expected.equals(BrowserQrScanner.decode(decoded)),"the actual shared PNG decodes to the exact browser URL");}finally{decoded.delete();}
        require(Intent.ACTION_SEND.equals(textIntent.get().getAction())&&"text/plain".equals(textIntent.get().getType())&&expected.equals(textIntent.get().getStringExtra(Intent.EXTRA_TEXT)),"other-app text sharing sends the same complete URL");
        click(label(R.string.share_qr_image));await(this::systemShareChooserVisible,"Android image sharesheet actually opens");systemBack();
        await(()->field(label(R.string.address_edit_label))!=null,"closing image sharesheet restores the browser");
        openShare();click(label(R.string.share_other_apps));await(this::systemShareChooserVisible,"Android link sharesheet actually opens");systemBack();
        await(()->field(label(R.string.address_edit_label))!=null,"closing link sharesheet restores the browser");
        openSettingsItem(R.string.settings_appearance);click(label(R.string.theme_night));click(label(R.string.back));click(label(R.string.back));
        openShare();capture("share-dark.png",null);systemBack();
        require(uiCheck(()->sharing.getPage()==null),"Android Back dismisses the custom share panel");
        openSettingsItem(R.string.settings_appearance);click(label(R.string.theme_day));click(label(R.string.back));click(label(R.string.back));
        click(label(R.string.browser_menu));scrollClick(label(R.string.private_tab));
        runOnMainSync(()->{try{browserView().getSession().loadUri(expected);}catch(Exception error){throw new RuntimeException(error);}});
        await(()->!homeVisible()&&expected.equals(text(field(label(R.string.address_edit_label)))),"real private tab navigates to the share page");
        openShare();require(uiCheck(()->sharing.getPage()!=null&&sharing.getPage().getPrivateMode()),"private page remains explicitly marked in the custom share preview");
        click(label(R.string.share_copy_link));
        require(uiCheck(()->clipboard.getPrimaryClip()!=null&&clipboard.getPrimaryClip().getDescription().getExtras()!=null&&clipboard.getPrimaryClip().getDescription().getExtras().getBoolean("android.content.extra.IS_SENSITIVE")),"private link copy is marked sensitive for Android clipboard previews");
        click(label(R.string.browser_menu));scrollClick(label(R.string.menu_new_normal_tab_accessibility));
        openSettingsItem(R.string.settings_about_update);
        await(()->find(label(R.string.about_publisher),false)!=null,"About names the actual publisher");capture("about-light.png",true);
        scrollClick(label(R.string.about_licenses));
        await(()->field(label(R.string.license_search))!=null,"licenses open as their own full page");
        search(label(R.string.license_search),"ZXing");
        await(()->find(label(R.string.license_zxing),false)!=null&&find(label(R.string.license_gecko),false)==null,"license search filters the real bundled component list");
        click(label(R.string.license_zxing));
        await(()->find("Apache-2.0",false)!=null&&!softwareKeyboardVisible(),"full offline license detail opens and hides the keyboard");
        capture("license-detail-light.png",true);
        swipeUp();require(find(label(R.string.license_unavailable),false)==null,"license text remains readable while scrolling");
        systemBack();await(()->field(label(R.string.license_search))!=null,"Back from a license restores its filtered list");
        systemBack();await(()->find(label(R.string.update_manual_check),false)!=null,"Back from licenses restores About");
        scrollClick(label(R.string.about_feedback));
        await(()->find(label(R.string.feedback_email),false)!=null,"About opens the dedicated feedback page");capture("feedback-light.png",true);
        java.util.concurrent.atomic.AtomicReference<Intent> email=new java.util.concurrent.atomic.AtomicReference<>();
        runOnMainSync(()->{try{java.lang.reflect.Method m=MainActivity.class.getDeclaredMethod("feedbackEmailIntent");m.setAccessible(true);email.set((Intent)m.invoke(activity));}catch(Exception error){throw new RuntimeException(error);}});
        require(Intent.ACTION_SENDTO.equals(email.get().getAction())&&"mailto".equals(email.get().getData().getScheme())&&email.get().getData().getSchemeSpecificPart().startsWith("destek@atasoyyazilim.com?"),"feedback prepares a draft addressed to the public support mailbox");
        require(!Uri.decode(email.get().getDataString()).contains("eymen-share")&&email.get().getExtras()==null,"feedback never attaches browsing URLs, logs or VPN data automatically");
        android.content.IntentFilter filter=new android.content.IntentFilter(Intent.ACTION_SENDTO);filter.addDataScheme("mailto");
        ActivityMonitor monitor=addMonitor(filter,new ActivityResult(Activity.RESULT_CANCELED,null),true);
        try{click(label(R.string.feedback_email));require(monitor.getHits()==1,"feedback button requests the email composer without sending a message");}finally{removeMonitor(monitor);}
        systemBack();await(()->find(label(R.string.update_manual_check),false)!=null,"feedback Back returns to About");
        scrollClick(label(R.string.about_website));
        await(()->"https://atasoyyazilim.com/".equals(text(field(label(R.string.address_edit_label)))),"publisher website opens in a real browser tab");
        openSettingsItem(R.string.settings_about_update);scrollClick(label(R.string.about_privacy));
        await(()->"https://atasoyyazilim.com/gizlilik/".equals(text(field(label(R.string.address_edit_label)))),"privacy policy opens the publisher's real HTTPS page");
        click(label(R.string.browser_menu));scrollClick(label(R.string.settings_help));
        await(()->find(label(R.string.feedback_support),false)!=null,"menu Help opens the real feedback page");
        click(label(R.string.feedback_support));
        await(()->"https://atasoyyazilim.com/destek/".equals(text(field(label(R.string.address_edit_label)))),"support action opens the publisher's real support center");
    }
    private void openShare() throws Exception {click(label(R.string.browser_menu));scrollClick(label(R.string.menu_share));}

    private org.mozilla.geckoview.GeckoSession.ContentDelegate contentDelegate(org.mozilla.geckoview.GeckoSession page) {
        if(android.os.Looper.myLooper()==android.os.Looper.getMainLooper())return page.getContentDelegate();
        java.util.concurrent.atomic.AtomicReference<org.mozilla.geckoview.GeckoSession.ContentDelegate> delegate=new java.util.concurrent.atomic.AtomicReference<>();
        runOnMainSync(() -> delegate.set(page.getContentDelegate()));return delegate.get();
    }
    private org.mozilla.geckoview.GeckoSession searchTabPage(boolean privateMode, String url, String pageTitle) throws Exception {
        menuNavigation(privateMode ? R.string.private_tab : R.string.menu_new_normal_tab_accessibility);
        org.mozilla.geckoview.GeckoSession page = currentSession();
        java.util.concurrent.atomic.AtomicReference<String> title = new java.util.concurrent.atomic.AtomicReference<>("");
        org.mozilla.geckoview.GeckoSession.ContentDelegate original = contentDelegate(page);
        runOnMainSync(() -> {
            page.setContentDelegate(new org.mozilla.geckoview.GeckoSession.ContentDelegate() {
                @Override public void onTitleChange(org.mozilla.geckoview.GeckoSession session, String value) {
                    title.set(value); original.onTitleChange(session, value);
                }
            });
            page.loadUri(url);
        });
        await(() -> "Example Domain".equals(title.get()) && url.equals(text(field(label(R.string.address_edit_label)))),
            "real HTTPS document loaded for tab search");
        runOnMainSync(() -> page.loadUri("javascript:void(document.title=" + org.json.JSONObject.quote(pageTitle) + ")"));
        await(() -> pageTitle.equals(title.get()), "actual document title ready for tab search");
        runOnMainSync(() -> page.setContentDelegate(original));
        return page;
    }

    private int realTabCount() {
        return uiInt(() -> ((java.util.List<?>) controller("tabs")).size());
    }

    private void openTabSearch() throws Exception {
        String alphaUrl = "https://example.com/?eymen-tab-search=alpha";
        String betaUrl = "https://example.com/?eymen-tab-search=beta";
        String alphaTitle = "Eymen Sekme Deneyi İzmir", betaTitle = "Eymen Sekme Deneyi Ankara";
        String secretTitle = "Eymen Sekme Deneyi Gizli";
        org.mozilla.geckoview.GeckoSession alpha = searchTabPage(false, alphaUrl, alphaTitle);
        org.mozilla.geckoview.GeckoSession beta = searchTabPage(false, betaUrl, betaTitle);
        org.mozilla.geckoview.GeckoSession secret = searchTabPage(true,
            "https://example.com/?eymen-tab-search=private", secretTitle);
        int count = realTabCount();
        String hint = label(R.string.tabs_search_hint);

        click(label(R.string.tabs));
        await(() -> field(hint) != null, "tab overview has a real search editor");
        tap(field(hint));
        await(this::softwareKeyboardVisible, "tab search opens the actual on-screen keyboard");
        sendStringSync("eymen sekme deneyi");
        await(() -> "eymen sekme deneyi".equals(text(field(hint))) &&
            tabResult(activity.getString(R.string.tabs_search_results, 1)) != null,
            "physical typing finds only the private matching tab");
        systemBack();
        await(() -> field(hint) != null && !softwareKeyboardVisible(),
            "Back hides the tab search keyboard without closing its results");
        require(tabResult(secretTitle) != null && tabResult(alphaTitle) == null && tabResult(betaTitle) == null,
            "private results never include matching normal titles");
        capture("tabs-search-private.png", true);
        search(hint, "eymen-tab-search=alpha");
        await(() -> tabResult(label(R.string.tabs_search_empty)) != null,
            "a normal-only address cannot appear in private search");

        click(label(R.string.tabs));
        await(() -> field(hint) != null && text(field(hint)).isEmpty(),
            "switching to normal tabs discards the private search text");
        search(hint, "eymen sekme deneyi");
        await(() -> tabResult(activity.getString(R.string.tabs_search_results, 2)) != null,
            "normal search finds the two real normal pages");
        require(tabResult(secretTitle) == null && tabResult(alphaTitle) != null && tabResult(betaTitle) != null,
            "normal results exclude the matching private tab");
        click(label(R.string.tab_next) + " ›");
        await(() -> currentSession() == alpha && field(hint) == null,
            "Next from an excluded active tab selects the first matching existing session");
        click(label(R.string.tabs));
        search(hint, "izmir");
        await(() -> tabResult(alphaTitle) != null && tabResult(betaTitle) == null &&
            tabResult(activity.getString(R.string.tabs_search_results, 1)) != null,
            "Turkish title search ignores case without changing tab order");
        capture("tabs-search-title.png", true);
        search(hint, "EXAMPLE.COM/?EYMEN-TAB-SEARCH=BETA");
        await(() -> tabResult(betaTitle) != null && tabResult(alphaTitle) == null,
            "actual URL path and query are searchable independently of the page title");
        click(label(R.string.tab_next) + " ›");
        await(() -> currentSession() == beta && field(hint) == null,
            "quick switching can activate a single result when the current tab is excluded");
        click(label(R.string.tabs));
        search(hint, "no-such-open-tab-acceptance");
        await(() -> tabResult(label(R.string.tabs_search_empty)) != null,
            "unmatched query shows an honest empty result");
        AccessibilityNodeInfo closeAll = tabResult(label(R.string.close_all_tabs));
        require(closeAll != null, "close-all action remains visible while filtered");
        tap(closeAll); waitForIdleSync();
        require(realTabCount() == count && uiCheck(() -> alpha.isOpen() && beta.isOpen() && secret.isOpen()),
            "touching disabled Close all cannot close hidden or private tabs");
        capture("tabs-search-empty.png", true);
        click(label(R.string.search_clear));
        await(() -> field(hint) != null && text(field(hint)).isEmpty(), "clear restores the current normal scope");
        search(hint, "izmir"); click(alphaTitle);
        await(() -> currentSession() == alpha && alphaUrl.equals(text(field(label(R.string.address_edit_label)))) &&
            field(hint) == null && !softwareKeyboardVisible(), "result opens its existing real session and hides the keyboard");
        require(realTabCount() == count, "search and result selection never create or remove a tab");
        click(label(R.string.tabs));
        await(() -> field(hint) != null && text(field(hint)).isEmpty(), "closing and reopening overview forgets its search");
        click(label(R.string.back));

        openSettingsItem(R.string.settings_appearance); click(label(R.string.theme_night));
        click(label(R.string.back)); click(label(R.string.back));
        click(label(R.string.tabs));
        search(hint, "eymen sekme deneyi");
        await(() -> tabResult(activity.getString(R.string.tabs_search_results, 2)) != null,
            "the same normal scope remains searchable in night theme");
        capture("tabs-search-dark.png", false);
        click(label(R.string.back));
        openSettingsItem(R.string.settings_appearance); click(label(R.string.theme_day));
        click(label(R.string.back)); click(label(R.string.back));
    }
    private BrowserBookmarks bookmarkStore() {
        return (BrowserBookmarks) ((kotlin.Lazy<?>) controller("bookmarks$delegate")).getValue();
    }
    private void bookmarkLibrary() throws Exception {
        String url="https://example.com/?eymen-bookmark-audit=original";
        String original="Eymen Yer İmi Başlığı", edited="Kütüphane Doğrulama";
        String editedUrl="https://example.com/?eymen-bookmark-audit=edited";
        String folder="Eymen Deneme Klasörü";
        BrowserBookmarks store=bookmarkStore();
        await(() -> uiCheck(() -> !store.getLoading() && store.getEntries().stream().anyMatch(x -> x.getUrl().equals("https://example.net/legacy-bookmark"))),
            "legacy production URL set migrated without losing its bookmark");
        searchTabPage(false,url,original);
        click(label(R.string.browser_menu));click(label(R.string.menu_bookmark_add));
        await(() -> uiCheck(() -> !store.getLoading() && store.getEntries().stream().anyMatch(x -> x.getUrl().equals(url) && x.getTitle().equals(original))),
            "explicit bookmark stores its real page title independently of history");
        click(label(R.string.browser_menu));scrollClick(label(R.string.bookmarks));
        await(() -> field(label(R.string.library_search_bookmarks))!=null,"real bookmark library opened");
        click(label(R.string.bookmark_new_folder));search(label(R.string.bookmark_folder_name),"Geçici Klasör");clickNativePositive();
        await(() -> uiCheck(() -> !store.getLoading() && store.getFolders().stream().anyMatch(x -> x.getName().equals("Geçici Klasör"))),"original folder saved");
        click(activity.getString(R.string.bookmark_open_folder,"Geçici Klasör"));click(activity.getString(R.string.bookmark_manage_folder,"Geçici Klasör"));click(label(R.string.bookmark_rename_folder));
        search(label(R.string.bookmark_folder_name),folder);clickNativePositive();
        await(() -> uiCheck(() -> !store.getLoading() && store.getFolders().stream().anyMatch(x -> x.getName().equals(folder))),"new folder persisted");
        click(label(R.string.bookmark_show_all));
        click(activity.getString(R.string.bookmark_item_options,original));click(label(R.string.bookmark_move));click(folder);
        await(() -> uiCheck(() -> !store.getLoading() && store.getEntries().stream().anyMatch(x -> x.getUrl().equals(url) && x.getFolderId()!=null)),"existing bookmark moved to the selected folder");
        click(activity.getString(R.string.bookmark_open_folder,folder));
        search(label(R.string.library_search_bookmarks),"legacy-bookmark");
        await(() -> find(label(R.string.history_no_results),false)!=null,"folder search cannot include a bookmark from outside the folder");
        click(label(R.string.search_clear));
        click(activity.getString(R.string.bookmark_item_options,original));click(label(R.string.bookmark_edit));
        search(label(R.string.bookmark_title),edited);search(label(R.string.bookmark_address),"javascript:alert(1)");clickNativePositive();
        require(field(label(R.string.bookmark_address))!=null,"unsafe bookmark edit is rejected without closing the editor");
        require(uiCheck(() -> store.getEntries().stream().anyMatch(x -> x.getUrl().equals(url))),"rejected edit preserves the original saved address");
        search(label(R.string.bookmark_address),editedUrl);clickNativePositive();
        await(() -> uiCheck(() -> !store.getLoading() && store.getEntries().stream().anyMatch(x -> x.getTitle().equals(edited) && x.getUrl().equals(editedUrl))),"title and address edit committed together");
        capture("bookmarks-folder.png",true);
        click(activity.getString(R.string.bookmark_manage_folder,folder));click(label(R.string.bookmark_delete_folder));clickNativePositive();
        await(() -> uiCheck(() -> !store.getLoading() && store.getFolders().isEmpty() && store.getEntries().stream().anyMatch(x -> x.getTitle().equals(edited) && x.getFolderId()==null)),
            "folder deletion keeps its edited bookmark at the library root");
        runOnMainSync(store::refresh);await(() -> uiCheck(() -> !store.getLoading()),"bookmark library reloaded from SQLite");
        try(android.database.sqlite.SQLiteDatabase db=android.database.sqlite.SQLiteDatabase.openDatabase(
                getTargetContext().getDatabasePath("browser-bookmarks.db").getPath(),null,android.database.sqlite.SQLiteDatabase.OPEN_READONLY);
            android.database.Cursor row=db.rawQuery("SELECT title,folder_id FROM bookmarks WHERE url=?",new String[]{editedUrl})) {
            require(row.moveToFirst() && edited.equals(row.getString(0)) && row.isNull(1),"edited title and preserved bookmark are durable SQLite records");
        }
        capture("bookmarks-all.png",true);
        click(label(R.string.back));
        tap(field(label(R.string.address_edit_label)));
        await(this::softwareKeyboardVisible,"real keyboard opened for local address suggestions");
        search(label(R.string.address_edit_label),edited);
        await(() -> find(activity.getString(R.string.address_suggestion_action,edited),false)!=null,"local suggestion row has actual accessible touch bounds");
        capture("address-suggestions.png",true);
        click(activity.getString(R.string.address_suggestion_action,edited));
        await(() -> editedUrl.equals(text(field(label(R.string.address_edit_label)))) && !softwareKeyboardVisible(),
            "local bookmark suggestion navigates to its stored address and closes the keyboard");
        click(label(R.string.browser_menu));scrollClick(label(R.string.private_tab));
        tap(field(label(R.string.address_edit_label)));
        await(this::softwareKeyboardVisible,"real keyboard opened for private address entry");
        search(label(R.string.address_edit_label),edited);
        require(find(activity.getString(R.string.address_suggestion_action,edited),false)==null,
            "private address editing does not expose normal bookmarks or history suggestions");
        systemBack();
        click(label(R.string.browser_menu));scrollClick(label(R.string.menu_new_normal_tab_accessibility));
    }
    private BrowserMedia mediaController() {
        return (BrowserMedia)((kotlin.Lazy<?>)controller("media$delegate")).getValue();
    }
    private android.app.Notification mediaNotification() {
        for(android.service.notification.StatusBarNotification item:getTargetContext().getSystemService(android.app.NotificationManager.class).getActiveNotifications())
            if(item.getId()==BrowserMediaService.NOTIFICATION)return item.getNotification();
        return null;
    }
    private android.media.session.MediaController androidMediaController() {
        android.app.Notification note=mediaNotification();
        if(note==null)return null;
        android.media.session.MediaSession.Token token=note.extras.getParcelable(android.app.Notification.EXTRA_MEDIA_SESSION,android.media.session.MediaSession.Token.class);
        return token==null?null:new android.media.session.MediaController(getTargetContext(),token);
    }
    private void foregroundBrowser() throws Exception {
        try(android.os.ParcelFileDescriptor result=getUiAutomation().executeShellCommand("am start -W -n com.eymen.browser.telefon/.MainActivity");
            java.io.InputStream stream=new android.os.ParcelFileDescriptor.AutoCloseInputStream(result)) {
            byte[] buffer=new byte[4096];while(stream.read(buffer)!=-1){}
        }
        await(() -> {
            // Android may show first-use immersive education while returning
            // from PiP. Acknowledge it before requiring application focus.
            dismissVisibleFullscreenEducation();
            return uiCheck(() -> activity.hasWindowFocus() && !activity.isInPictureInPictureMode());
        },"browser returned from real Android Home/PiP");
    }
    private org.mozilla.geckoview.GeckoSession prepareMedia(boolean privateMode, java.util.concurrent.atomic.AtomicInteger ticks) throws Exception {
        org.mozilla.geckoview.GeckoSession page=searchTabPage(privateMode,"https://example.com/?eymen-media-audit="+(privateMode?"private":"normal"),"Eymen media fixture");
        String base64;
        try(java.io.InputStream asset=getContext().getAssets().open("media-fixture.webm.b64")) {
            java.io.ByteArrayOutputStream bytes=new java.io.ByteArrayOutputStream();byte[] block=new byte[4096];int size;
            while((size=asset.read(block))!=-1)bytes.write(block,0,size);
            base64=bytes.toString("UTF-8").trim();
        }
        org.mozilla.geckoview.GeckoSession.ContentDelegate original=contentDelegate(page);
        String html="<video id='fixture' style='position:fixed;inset:0;width:100%;height:100%' loop playsinline src='data:video/webm;base64,"+base64+"'></video><button id='play' style='position:fixed;inset:0;width:100%;height:100%;z-index:1'>Eymen media play</button>";
        String js="javascript:void(function(){document.body.innerHTML="+org.json.JSONObject.quote(html)+";const v=document.getElementById('fixture'),b=document.getElementById('play');let ticks=0;v.addEventListener('timeupdate',()=>document.title='Eymen media tick '+(++ticks));navigator.mediaSession.metadata=new MediaMetadata({title:'Eymen Media Acceptance'});b.onclick=()=>v.play().then(()=>{b.textContent='Eymen media fullscreen';b.onclick=()=>v.requestFullscreen()});document.title='Eymen media ready';}())";
        runOnMainSync(() -> {
            page.setContentDelegate(new org.mozilla.geckoview.GeckoSession.ContentDelegate(){
                @Override public void onTitleChange(org.mozilla.geckoview.GeckoSession s,String title){
                    if(title!=null && title.startsWith("Eymen media tick "))ticks.set(Integer.parseInt(title.substring("Eymen media tick ".length())));
                    original.onTitleChange(s,title);
                }
                @Override public void onFullScreen(org.mozilla.geckoview.GeckoSession s,boolean full){original.onFullScreen(s,full);}
            });
            page.loadUri(js);
        });
        await(() -> find("Eymen media play",false)!=null,"actual synthetic video play control is laid out");
        click("Eymen media play");
        await(() -> ticks.get()>0 && uiCheck(() -> mediaController().isPlaying(page)) && androidMediaController()!=null,
            "real VP8/Vorbis playback produces Gecko events and an Android media notification");
        return page;
    }
    private boolean dismissVisibleFullscreenEducation() {
        for(android.view.accessibility.AccessibilityWindowInfo window:getUiAutomation().getWindows()){
            AccessibilityNodeInfo root=window.getRoot();
            if(root==null || !root.refresh() || window.getType()!=AccessibilityWindowInfo.TYPE_SYSTEM)continue;
            ArrayDeque<AccessibilityNodeInfo> queue=new ArrayDeque<>();queue.add(root);
            while(!queue.isEmpty()){
                AccessibilityNodeInfo node=queue.removeFirst();if(!node.refresh())continue;
                String pkg=String.valueOf(node.getPackageName());
                // Android 15 hosts this framework window in system_server
                // (package android); newer builds can host it in SystemUI.
                boolean systemPackage="android".equals(pkg)||"com.android.systemui".equals(pkg);
                if(systemPackage && "android:id/ok".equals(node.getViewIdResourceName()) &&
                    node.isVisibleToUser() && node.isClickable() && matches(node,"Got it")){
                    tap(node);waitForIdleSync();return true;
                }
                for(int i=0;i<node.getChildCount();i++){AccessibilityNodeInfo child=node.getChild(i);if(child!=null)queue.add(child);}
            }
        }
        return false;
    }
    private void dismissFullscreenEducation() throws Exception {
        long deadline=SystemClock.uptimeMillis()+1500;
        while(SystemClock.uptimeMillis()<deadline){if(dismissVisibleFullscreenEducation())return;Thread.sleep(50);}
    }
    private void mediaAndSystemTheme() throws Exception {
        openSettingsItem(R.string.settings_appearance);click(label(R.string.theme_system));
        await(() -> "system".equals(prefs().getString("theme_mode","")),"follow-system is an actual persisted theme choice");
        require(prefs().getBoolean("day_theme",false)==((activity.getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK)!=android.content.res.Configuration.UI_MODE_NIGHT_YES),
            "follow-system resolves the current Android light/night configuration");
        capture("theme-system.png",null);click(label(R.string.theme_day));click(label(R.string.back));click(label(R.string.back));
        java.util.concurrent.atomic.AtomicInteger ticks=new java.util.concurrent.atomic.AtomicInteger();
        org.mozilla.geckoview.GeckoSession page=prepareMedia(false,ticks);
        await(() -> androidMediaController()!=null && androidMediaController().getMetadata()!=null && "Eymen Media Acceptance".equals(androidMediaController().getMetadata().getString(android.media.MediaMetadata.METADATA_KEY_TITLE)),"Android media title comes from the actual page metadata");
        require(getUiAutomation().performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_HOME),"real Android Home action");
        await(() -> uiCheck(() -> !((androidx.activity.ComponentActivity)activity).getLifecycle().getCurrentState().isAtLeast(androidx.lifecycle.Lifecycle.State.STARTED)),"media test reaches the real Activity onStop boundary");
        int before=ticks.get();await(() -> ticks.get()>before+1,"actual media time continues while the browser is stopped behind Android Home");
        android.media.session.MediaController nativeMedia=androidMediaController();
        require(nativeMedia!=null,"native media session token remains available in background");
        nativeMedia.getTransportControls().pause();
        await(() -> uiCheck(() -> !mediaController().getPlaying()) && nativeMedia.getPlaybackState().getState()==android.media.session.PlaybackState.STATE_PAUSED,"Android media pause controls the real Gecko player");
        nativeMedia.getTransportControls().play();
        await(() -> uiCheck(() -> mediaController().getPlaying()) && nativeMedia.getPlaybackState().getState()==android.media.session.PlaybackState.STATE_PLAYING,"Android media resumes real playback from background");
        int resumed=ticks.get();await(() -> ticks.get()>resumed+1,"resumed media really advances its playback clock");
        capture("media-background.png",null);
        foregroundBrowser();click("Eymen media fullscreen");
        await(() -> uiCheck(() -> Boolean.TRUE.equals(controller("videoFullScreen"))),"real DOM fullscreen reaches the production video callback");
        dismissFullscreenEducation();
        require(getUiAutomation().performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_HOME),"Home leaves a playing fullscreen video");
        await(() -> uiCheck(() -> activity.isInPictureInPictureMode()) && uiCheck(() -> mediaController().isPlaying(page)),"actual Android PiP keeps the fullscreen video playing");
        capture("media-pip.png",null);foregroundBrowser();
        require(uiCheck(() -> currentSession()==page),"returning from real PiP preserves the same Gecko session");
        dismissFullscreenEducation();systemBack();
        await(() -> uiCheck(() -> !Boolean.TRUE.equals(controller("videoFullScreen"))) && find(label(R.string.browser_menu),false)!=null,
            "actual Android Back exits DOM fullscreen and restores usable browser controls after PiP");
        nativeMedia.getTransportControls().stop();
        await(() -> uiCheck(() -> mediaController().getOwner()==null) && mediaNotification()==null,"Stop releases playback, service and its notification");
        prepareMedia(true,new java.util.concurrent.atomic.AtomicInteger());
        await(() -> androidMediaController()!=null && androidMediaController().getMetadata()!=null && label(R.string.private_tab).equals(androidMediaController().getMetadata().getString(android.media.MediaMetadata.METADATA_KEY_TITLE)),"private media never exports the page title to Android media history");
        require(mediaNotification().visibility==android.app.Notification.VISIBILITY_SECRET,"private media notification is hidden on the lock screen");
        androidMediaController().getTransportControls().stop();
        await(() -> mediaNotification()==null,"private media stops and removes its Android notification");
        menuNavigation(R.string.menu_new_normal_tab_accessibility);
    }
    private Uri libraryFile(String name,String mime,String text) throws Exception {
        android.content.ContentValues values=new android.content.ContentValues();
        values.put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME,name);values.put(android.provider.MediaStore.MediaColumns.MIME_TYPE,mime);
        values.put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH,android.os.Environment.DIRECTORY_DOWNLOADS);values.put(android.provider.MediaStore.MediaColumns.IS_PENDING,1);
        Uri uri=getTargetContext().getContentResolver().insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI,values);
        require(uri!=null,"real local transfer fixture allocated");libraryFixtures.add(uri);
        try(java.io.OutputStream stream=getTargetContext().getContentResolver().openOutputStream(uri)){require(stream!=null,"real fixture output opened");stream.write(text.getBytes(java.nio.charset.StandardCharsets.UTF_8));}
        values.clear();values.put(android.provider.MediaStore.MediaColumns.IS_PENDING,0);getTargetContext().getContentResolver().update(uri,values,null,null);return uri;
    }
    private String libraryText(Uri uri) throws Exception {
        try(java.io.InputStream stream=getTargetContext().getContentResolver().openInputStream(uri)){
            require(stream!=null,"actual exported file can be read");return BookmarkExchange.INSTANCE.read(stream);
        }
    }
    private void libraryTransferAndReading() throws Exception {
        BrowserBookmarks store=bookmarkStore();
        require(uiCheck(() -> store.getEntries().stream().anyMatch(x -> x.getUrl().equals("https://example.org/v1-library") && x.getTitle().equals("V1 kitaplık başlığı"))),
            "real SQLite version-1 upgrade preserves an existing titled bookmark");
        String imported="https://example.org/?eymen-import=1";
        org.json.JSONObject data=new org.json.JSONObject().put("format","eymen-bookmarks").put("version",1)
            .put("folders",new org.json.JSONArray().put(new org.json.JSONObject().put("key","imported").put("name","İçe Aktarılanlar")))
            .put("bookmarks",new org.json.JSONArray().put(new org.json.JSONObject().put("url",imported).put("title","İçe Aktarım Deneyi").put("folder","imported")));
        Uri source=libraryFile("Eymen-bookmarks-import.json","application/json",data.toString());
        runOnMainSync(() -> store.importFile(source));
        await(() -> uiCheck(() -> !store.getLoading() && store.getImportAdded()==1 && store.getEntries().stream().anyMatch(x -> x.getUrl().equals(imported) && x.getFolderId()!=null)),"real JSON file imports title and folder atomically");
        int count=uiInt(() -> store.getEntries().size());
        runOnMainSync(() -> store.importFile(source));
        await(() -> uiCheck(() -> !store.getLoading() && store.getImportAdded()==0 && store.getImportSkipped()==1),"duplicate import preserves the existing entry");
        Uri json=libraryFile("Eymen-export.json","application/json","");
        runOnMainSync(() -> store.exportFile(json,false));
        await(() -> uiCheck(() -> !store.getLoading() && Integer.valueOf(R.string.bookmark_export_done).equals(store.getNotice())),"JSON export finished through real ContentResolver");
        org.json.JSONObject exported=new org.json.JSONObject(libraryText(json));
        require(exported.getJSONArray("bookmarks").length()==count && exported.getJSONArray("folders").length()==1,"JSON export contains the actual saved library and folder");
        org.json.JSONObject invalid=new org.json.JSONObject(data.toString()).put("version",999);
        Uri bad=libraryFile("Eymen-invalid.json","application/json",invalid.toString());
        runOnMainSync(() -> store.importFile(bad));
        await(() -> uiCheck(() -> !store.getLoading() && Integer.valueOf(R.string.bookmark_import_invalid).equals(store.getError())),"unknown JSON version is rejected");
        require(uiInt(() -> store.getEntries().size())==count,"failed import transaction preserves every existing bookmark");
        Uri html=libraryFile("Eymen-export.html","text/html","");
        runOnMainSync(() -> store.exportFile(html,true));
        await(() -> uiCheck(() -> !store.getLoading() && Integer.valueOf(R.string.bookmark_export_done).equals(store.getNotice())),"HTML export finished");
        String markup=libraryText(html);
        require(markup.contains("NETSCAPE-Bookmark-file-1") && markup.contains("<H3>") && markup.contains(imported),"HTML export contains real portable anchors and folder structure");
        Uri links=libraryFile("Eymen-browser-import.html","text/html","<DL><DT><A HREF='https://example.net/html-import'>HTML Başlık</A><DT><A HREF='javascript:alert(1)'>Unsafe</A></DL>");
        runOnMainSync(() -> store.importFile(links));
        await(() -> uiCheck(() -> !store.getLoading() && store.getImportAdded()==1 && store.getImportSkipped()==1),"HTML imports only its real safe HTTPS bookmark without executing content");
        String url="https://example.com/?eymen-reading=1",title="Eymen Okuma Deneyi";
        searchTabPage(false,url,title);click(label(R.string.browser_menu));scrollClick(label(R.string.reading_add));
        await(() -> field(label(R.string.reading_search))!=null && uiCheck(() -> !store.getLoading() && store.getReading().stream().anyMatch(x -> x.getUrl().equals(url) && !x.getRead())),"explicit reading-list action stores a real unread page");
        click(activity.getString(R.string.reading_mark_read,title));
        await(() -> uiCheck(() -> !store.getLoading() && store.getReading().stream().anyMatch(x -> x.getUrl().equals(url) && x.getRead())),"read state persisted");
        click(label(R.string.reading_unread));await(() -> find(label(R.string.reading_empty),false)!=null,"unread filter excludes the read page");
        click(label(R.string.reading_read));search(label(R.string.reading_search),"okuma");
        await(() -> find(activity.getString(R.string.reading_open,title),false)!=null,"saved title is searchable in the selected reading state");
        capture("reading-list.png",true);
        click(activity.getString(R.string.reading_mark_unread,title));click(label(R.string.tab_all));
        click(activity.getString(R.string.reading_open,title));
        await(() -> url.equals(text(field(label(R.string.address_edit_label)))) && uiCheck(() -> store.getReading().stream().anyMatch(x -> x.getUrl().equals(url) && x.getRead())),"opening saved page uses its actual address and marks it read");
        click(label(R.string.browser_menu));scrollClick(label(R.string.reading_list));
        await(() -> field(label(R.string.reading_search))!=null,"reading list reopened");
        click(activity.getString(R.string.reading_remove,title));await(() -> uiCheck(() -> !store.getLoading() && store.getReading().isEmpty()),"removing a reading entry preserves browser bookmarks");
        require(uiInt(() -> store.getEntries().size())==count+1,"reading-list deletion never deletes a bookmark");click(label(R.string.back));
    }
    private void shellComplete(String command) throws Exception {
        try(android.os.ParcelFileDescriptor result=getUiAutomation().executeShellCommand(command);
            java.io.InputStream stream=new android.os.ParcelFileDescriptor.AutoCloseInputStream(result)) {
            byte[] block=new byte[4096];while(stream.read(block)!=-1){}
        }
    }
    private PrivateTabLock tabLock() {return (PrivateTabLock)controller("privateTabsLock");}
    private void confirmTestDeviceCredential() throws Exception {
        await(() -> pinEditor()!=null,"actual Android device-credential password editor");
        tap(pinEditor());
        android.view.KeyEvent[] keys=android.view.KeyCharacterMap.load(android.view.KeyCharacterMap.VIRTUAL_KEYBOARD).getEvents("642857".toCharArray());
        require(keys!=null,"synthetic emulator credential key events");
        for(android.view.KeyEvent key:keys)require(getUiAutomation().injectInputEvent(key,true),"real Android credential key injected");
        long now=SystemClock.uptimeMillis();
        require(getUiAutomation().injectInputEvent(new android.view.KeyEvent(now,now,android.view.KeyEvent.ACTION_DOWN,android.view.KeyEvent.KEYCODE_ENTER,0),true),"confirm device PIN down");
        require(getUiAutomation().injectInputEvent(new android.view.KeyEvent(now,now+1,android.view.KeyEvent.ACTION_UP,android.view.KeyEvent.KEYCODE_ENTER,0),true),"confirm device PIN up");
    }
    private void privateLockWithRealAndroidCredential() throws Exception {
        // This credential fixture is strictly for the disposable AOSP emulator.
        // Never modify a physical device's lock or replace an existing credential.
        require("ranchu".equals(android.os.Build.HARDWARE)||"goldfish".equals(android.os.Build.HARDWARE),"credential test requires the disposable emulator");
        android.app.KeyguardManager keyguard=getTargetContext().getSystemService(android.app.KeyguardManager.class);
        require(!keyguard.isDeviceSecure(),"credential test never replaces an existing device lock");
        boolean ownCredential=false;
        try {
            shellComplete("locksettings set-pin 642857");
            await(keyguard::isDeviceSecure,"real Android test PIN enrollment");ownCredential=true;
            openSettingsItem(R.string.settings_shield);click(label(R.string.private_lock_title));confirmTestDeviceCredential();
            await(() -> uiCheck(() -> tabLock().getEnabled() && !tabLock().getLocked()),"only real Android credential success enables the private lock");
            capture("private-lock-settings.png",true);click(label(R.string.back));click(label(R.string.back));
            String url="https://example.com/?eymen-private-lock=1",title="Eymen Kilitli Gizli Sayfa";
            org.mozilla.geckoview.GeckoSession secret=searchTabPage(true,url,title);
            int tabs=realTabCount();
            require(uiCheck(() -> (activity.getWindow().getAttributes().flags & android.view.WindowManager.LayoutParams.FLAG_SECURE)!=0),"private page is protected from recents/screenshots while lock is enabled");
            require(getUiAutomation().performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_HOME),"real background transition locks private browsing");
            await(() -> uiCheck(() -> tabLock().getLocked()),"background sets the real private lock gate");foregroundBrowser();
            await(() -> find(label(R.string.private_locked_title),false)!=null && uiCheck(() -> {
                try{return browserView().getSession()==null;}catch(Exception e){throw new RuntimeException(e);}}),"locked private engine is detached and covered before viewing");
            require(field(label(R.string.address_edit_label))==null && find(title,false)==null,"locked private address/title cannot appear in the app accessibility tree");
            click(label(R.string.private_unlock));await(() -> pinEditor()!=null,"private unlock opens Android credential UI");systemBack();
            await(() -> find(label(R.string.private_locked_title),false)!=null && uiCheck(() -> tabLock().getLocked()),"cancelling Android authentication keeps private tabs locked");
            click(label(R.string.private_unlock));confirmTestDeviceCredential();
            await(() -> uiCheck(() -> !tabLock().getLocked()) && url.equals(text(field(label(R.string.address_edit_label)))),"real credential unlock restores the same private address");
            require(currentSession()==secret && realTabCount()==tabs,"authentication preserves the original Gecko session and tab count");
            menuNavigation(R.string.menu_new_normal_tab_accessibility);
            openSettingsItem(R.string.settings_shield);click(label(R.string.private_lock_title));confirmTestDeviceCredential();
            await(() -> uiCheck(() -> !tabLock().getEnabled()),"disabling the private lock also requires Android credential confirmation");
            click(label(R.string.back));click(label(R.string.back));
        } finally {
            if(ownCredential)shellComplete("locksettings clear --old 642857");
        }
    }
    private boolean printSpoolerVisible() {
        for(android.view.accessibility.AccessibilityWindowInfo window:getUiAutomation().getWindows()){
            AccessibilityNodeInfo root=window.getRoot();
            if(root!=null && root.refresh() && "com.android.printspooler".equals(String.valueOf(root.getPackageName())))return true;
        }return false;
    }
    private void secureDnsAndNativePrint() throws Exception {
        openSettingsItem(R.string.settings_shield);click(label(R.string.secure_dns_title));click(label(R.string.secure_dns_cloudflare));
        await(() -> uiCheck(() -> runtime().getSettings().getTrustedRecusiveResolverMode()==org.mozilla.geckoview.GeckoRuntimeSettings.TRR_MODE_ONLY &&
            "https://cloudflare-dns.com/dns-query".equals(runtime().getSettings().getTrustedRecursiveResolverUri())),"actual Gecko runtime uses selected Cloudflare DoH-only mode");
        require("cloudflare".equals(prefs().getString("secure_dns","")),"resolver choice persisted");
        capture("secure-dns.png",true);
        click(label(R.string.secure_dns_title));click(label(R.string.secure_dns_quad9));
        await(() -> uiCheck(() -> runtime().getSettings().getTrustedRecusiveResolverMode()==org.mozilla.geckoview.GeckoRuntimeSettings.TRR_MODE_ONLY &&
            "https://dns.quad9.net/dns-query".equals(runtime().getSettings().getTrustedRecursiveResolverUri())),"switching providers changes the actual runtime endpoint without enabling fallback");
        click(label(R.string.secure_dns_title));click(label(R.string.secure_dns_system));
        await(() -> uiCheck(() -> runtime().getSettings().getTrustedRecusiveResolverMode()==org.mozilla.geckoview.GeckoRuntimeSettings.TRR_MODE_DISABLED) &&
            "system".equals(prefs().getString("secure_dns","")),"device DNS option restores the actual runtime's explicit DoH-off mode");
        click(label(R.string.back));click(label(R.string.back));
        String url="https://example.com/?eymen-print-audit=1";
        org.mozilla.geckoview.GeckoSession page=searchTabPage(false,url,"Eymen Print Acceptance");
        int files=new DownloadRegistry(getTargetContext()).items().size();
        click(label(R.string.browser_menu));scrollClick(label(R.string.print_page));
        awaitLong(this::printSpoolerVisible,"real page PDF reaches Android PrintSpooler preview",45000);
        capture("native-print-preview.png",null);systemBack();
        await(() -> !printSpoolerVisible() && uiCheck(activity::hasWindowFocus) && url.equals(text(field(label(R.string.address_edit_label)))),
            "cancelling Android print restores the original browser page");
        require(currentSession()==page && new DownloadRegistry(getTargetContext()).items().size()==files,
            "print cancellation keeps the actual session and creates no user download record");
    }
    private BrowserPageStyles pageStyles() {return (BrowserPageStyles)((kotlin.Lazy<?>)controller("pageStyles$delegate")).getValue();}
    private BrowserPageCapture pageCapture() {return (BrowserPageCapture)((kotlin.Lazy<?>)controller("pageCapture$delegate")).getValue();}
    private void fullPageCaptureAndDarkPages() throws Exception {
        await(() -> uiCheck(() -> pageStyles().getReady()),"the APK-bundled page style component is installed in real Gecko");
        require(!prefs().getBoolean("dark_pages",false),"dark pages are opt-in by default");
        String url="https://example.com/?eymen-full-page-audit=1";
        org.mozilla.geckoview.GeckoSession page=searchTabPage(false,url,"Eymen full document");
        String html="<main style='height:2200px;background:white'><h1>Eymen full document top</h1><footer style='position:absolute;top:2000px;width:100%;height:200px;background:rgb(0,0,255)'>Eymen full document bottom</footer></main>";
        runOnMainSync(() -> page.loadUri("javascript:void(function(){document.documentElement.style.cssText='height:auto;overflow:auto';document.body.style.cssText='display:block;margin:0;padding:0;width:100%;max-width:none;height:auto;overflow:visible';document.body.innerHTML="+org.json.JSONObject.quote(html)+";window.scrollTo(0,0);}())"));
        await(() -> find("Eymen full document top",false)!=null,"the tall HTTPS document is actually rendered");
        int viewport=uiInt(() -> {try{return browserView().getHeight();}catch(Exception error){throw new RuntimeException(error);}});
        Uri output=libraryFile("Eymen-full-document.png","image/png","");
        runOnMainSync(() -> {try{
            long id=((BrowserTab)((java.util.List<?>)controller("tabs")).stream().filter(t -> ((BrowserTab)t).getGecko()==page).findFirst().get()).getId();
            java.lang.reflect.Method method=MainActivity.class.getDeclaredMethod("exportPageCapture",Uri.class,kotlin.Pair.class);method.setAccessible(true);
            method.invoke(activity,output,new kotlin.Pair<Long,String>(id,url));
        }catch(Exception error){throw new RuntimeException(error);}});
        awaitLong(() -> uiCheck(() -> !pageCapture().getBusy()),"actual full-document export completes",45000);
        try(java.io.InputStream input=getTargetContext().getContentResolver().openInputStream(output)){
            Bitmap bitmap=android.graphics.BitmapFactory.decodeStream(input);require(bitmap!=null,"exported file is an actual decodable PNG");
            try {
                require(bitmap.getHeight()>viewport*2,"full-page output includes content below more than two viewports");
                int pixel=bitmap.getPixel(bitmap.getWidth()/2,bitmap.getHeight()-20);
                require(Color.blue(pixel)>200 && Color.red(pixel)<50,"the off-screen document footer pixels are present in the actual exported PNG");
            }finally{bitmap.recycle();}
        }
        File folder=new File(activity.getCacheDir(),"browser-ui-audit");require(folder.isDirectory()||folder.mkdirs(),"capture evidence folder exists");
        try(java.io.InputStream input=getTargetContext().getContentResolver().openInputStream(output);java.io.OutputStream file=new FileOutputStream(new File(folder,"full-page-saved.png"))){
            require(input!=null,"real PNG evidence opened");byte[] block=new byte[8192];int count;while((count=input.read(block))!=-1)file.write(block,0,count);
        }
        require(currentSession()==page && url.equals(text(field(label(R.string.address_edit_label)))),"export keeps the original active page and session");
        openSettingsItem(R.string.settings_appearance);scrollClick(label(R.string.dark_pages));
        await(() -> prefs().getBoolean("dark_pages",false),"actual appearance toggle persists dark page choice");
        click(label(R.string.back));click(label(R.string.back));
        await(() -> find("Example Domain",false)!=null,"the selected dark-page preference reloads the real current document");
        runOnMainSync(() -> page.loadUri("javascript:void(function(){let n=0;const timer=setInterval(()=>{if(getComputedStyle(document.documentElement).filter.includes('invert')){clearInterval(timer);document.title='Eymen actual dark style';}else if(++n>100)clearInterval(timer);},100);}())"));
        await(() -> uiCheck(() -> ((java.util.List<?>)controller("tabs")).stream().anyMatch(t -> ((BrowserTab)t).getGecko()==page && "Eymen actual dark style".equals(((BrowserTab)t).getTitle()))),"the real HTTPS document receives its opt-in dark rendering style");
        require(uiCheck(() -> runtime().getSettings().getPreferredColorScheme()==org.mozilla.geckoview.GeckoRuntimeSettings.COLOR_SCHEME_LIGHT),"forced effect requests a light base rather than double-inverting an automatic dark page");
        capture("dark-page.png",true);
        openSettingsItem(R.string.settings_appearance);scrollClick(label(R.string.dark_pages));
        await(() -> !prefs().getBoolean("dark_pages",true),"dark-page effect can be disabled through its actual setting");
        click(label(R.string.back));click(label(R.string.back));
        await(() -> find("Example Domain",false)!=null,"disabling the effect reloads the page without the optional style");
        require(currentSession()==page && uiCheck(() -> runtime().getSettings().getPreferredColorScheme()==org.mozilla.geckoview.GeckoRuntimeSettings.COLOR_SCHEME_SYSTEM),"disabling restores system content preference and preserves the same session");
    }
    private void inactiveTabKeepsFormAndSession() throws Exception {
        org.mozilla.geckoview.GeckoSession page=searchTabPage(false,"https://example.com/?eymen-memory-audit=1","Eymen memory fixture");
        java.util.concurrent.atomic.AtomicReference<String> title=new java.util.concurrent.atomic.AtomicReference<>("");
        org.mozilla.geckoview.GeckoSession.ContentDelegate original=contentDelegate(page);
        runOnMainSync(() -> {
            page.setContentDelegate(new org.mozilla.geckoview.GeckoSession.ContentDelegate(){
                @Override public void onTitleChange(org.mozilla.geckoview.GeckoSession session,String value){title.set(value);original.onTitleChange(session,value);}
            });
            String html="<h1>Eymen tab memory</h1><textarea aria-label='Eymen tab memory note'></textarea>";
            page.loadUri("javascript:void(function(){document.body.innerHTML="+org.json.JSONObject.quote(html)+";window.__eymenMemoryToken='same live document';document.addEventListener('visibilitychange',()=>document.title=document.hidden?'Eymen memory hidden':'Eymen memory visible');document.title='Eymen memory visible';}())");
        });
        await(() -> field("Eymen tab memory note")!=null,"real web text area is available");
        tap(field("Eymen tab memory note"));sendStringSync("Eymen note kept");
        await(() -> "Eymen note kept".equals(text(field("Eymen tab memory note"))),"real keyboard writes a form value before tab deactivation");
        if(softwareKeyboardVisible())systemBack();
        searchTabPage(false,"https://example.com/?eymen-memory-other=1","Eymen other memory page");
        await(() -> "Eymen memory hidden".equals(title.get()),"releasing the display makes the real background document hidden");
        require(uiCheck(page::isOpen) && currentSession()!=page,"inactive tab releases the visible engine surface while keeping its actual session");
        // Gecko 159 schedules its native compositor memory-pressure signal at
        // ten seconds after deactivation. Exercise that real period as well.
        Thread.sleep(11000);
        click(label(R.string.tabs));search(label(R.string.tabs_search_hint),"eymen-memory-audit=1");
        await(() -> tabResult("Eymen memory hidden")!=null,"inactive tab remains searchable by its real address");
        tap(tabResult("Eymen memory hidden"));
        await(() -> currentSession()==page && "Eymen memory visible".equals(title.get()),"selecting the inactive card resumes the same actual Gecko document");
        require("Eymen note kept".equals(text(field("Eymen tab memory note"))),"user-entered form text survives the native inactive-memory cycle");
        runOnMainSync(() -> page.loadUri("javascript:void(document.title=window.__eymenMemoryToken==='same live document'?'Eymen memory RAM preserved':'Eymen memory RAM lost')"));
        await(() -> "Eymen memory RAM preserved".equals(title.get()),"JavaScript RAM state survives without a hidden reload or tab replacement");
        if(softwareKeyboardVisible())systemBack();capture("tab-memory-restored.png",true);
        runOnMainSync(() -> page.setContentDelegate(original));
    }
    private boolean systemShareChooserVisible(){
        for(AccessibilityWindowInfo window:getUiAutomation().getWindows()){
            AccessibilityNodeInfo root=window.getRoot();if(window.getType()!=AccessibilityWindowInfo.TYPE_APPLICATION||root==null||!root.refresh())continue;
            String name=String.valueOf(root.getPackageName());if("android".equals(name)||"com.android.intentresolver".equals(name))return true;
        }return false;
    }
    private void freeVpnSetup() throws Exception {
        Context app = getTargetContext();
        EymenVpnConfigStore store = new EymenVpnConfigStore(app);
        require(!store.exists(), "VPN setup starts without a provider profile");
        openSettingsItem(R.string.vpn_title);
        await(() -> find(label(R.string.vpn_free_title), false) != null, "free VPN account setup is available without a profile");
        capture("vpn-free-setup.png", true);
        scrollClick(label(R.string.vpn_free_open));
        await(() -> uiCheck(() -> {
            String url = String.valueOf(((androidx.compose.runtime.MutableState<?>)controller("currentUrl")).getValue());
            return url.startsWith("https://account.protonvpn.com/") || url.startsWith("https://account.proton.me/");
        }), "setup opens the official provider account in the browser");
        require(!store.exists(), "opening the provider website never creates credentials");

        // Ephemeral test-only keys and an RFC 5737 endpoint: no real VPN peer
        // is contacted, and importing must never start a VPN or ask for consent.
        com.wireguard.crypto.KeyPair client = new com.wireguard.crypto.KeyPair();
        String config = "[Interface]\nPrivateKey = " + client.getPrivateKey().toBase64()
            + "\nAddress = 10.91.0.2/32, fd91:1:2::2/128\nDNS = 10.91.0.1\n"
            + "ExcludedApplications = " + app.getPackageName() + "\n[Peer]\nPublicKey = "
            + new com.wireguard.crypto.KeyPair().getPublicKey().toBase64()
            + "\nEndpoint = 192.0.2.1:51820\nAllowedIPs = 0.0.0.0/0, ::/0\n";
        android.content.ContentValues values = new android.content.ContentValues();
        String filename = "eymen-ui-vpn.CONF";
        values.put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, filename);
        values.put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "application/octet-stream");
        values.put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS);
        values.put(android.provider.MediaStore.MediaColumns.IS_PENDING, 1);
        vpnFixtureUri = app.getContentResolver().insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
        require(vpnFixtureUri != null, "create test-only VPN download");
        byte[] bytes = config.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        try (java.io.OutputStream output = app.getContentResolver().openOutputStream(vpnFixtureUri)) {
            require(output != null, "open VPN fixture output"); output.write(bytes);
        } finally { java.util.Arrays.fill(bytes, (byte)0); }
        values.clear(); values.put(android.provider.MediaStore.MediaColumns.IS_PENDING, 0);
        app.getContentResolver().update(vpnFixtureUri, values, null, null);
        new DownloadRegistry(app).record(new SavedDownload(vpnFixtureUri.toString(), filename,
            "application/octet-stream", config.length(), System.currentTimeMillis()));
        click(label(R.string.browser_menu)); scrollClick(label(R.string.downloads));
        click(filename);
        await(() -> find(label(R.string.vpn_import_title), false) != null, "opening a conf download asks before importing");
        require(!store.exists(), "displaying the import confirmation does not modify the profile");
        click(label(android.R.string.cancel));
        require(!store.exists(), "cancelling a downloaded profile leaves VPN unconfigured");
        click(filename); click(label(R.string.vpn_import_confirm));
        await(() -> store.exists() && find(label(R.string.vpn_status_ready), false) != null,
            "confirmed download is imported and VPN settings show a ready disconnected profile");
        require(find(label(R.string.vpn_free_title), false) == null, "free provider setup hides after a profile exists");
        com.wireguard.config.Interface loaded = store.load().getInterface();
        require(loaded.getKeyPair().getPublicKey().equals(client.getPublicKey()), "the imported device key is preserved");
        require(loaded.getIncludedApplications().size() == 1
            && loaded.getIncludedApplications().contains(app.getPackageName())
            && loaded.getExcludedApplications().isEmpty(), "untrusted exclusions are replaced by browser-only routing");
        byte[] encrypted = java.nio.file.Files.readAllBytes(new File(app.getNoBackupFilesDir(), "eymen-wg-v1.bin").toPath());
        require(!new String(encrypted, java.nio.charset.StandardCharsets.UTF_8).contains(client.getPrivateKey().toBase64()),
            "VPN private key is absent from the encrypted profile file");
        java.util.Arrays.fill(encrypted, (byte)0);
        require(android.net.VpnService.prepare(app) != null, "profile import never obtains Android VPN permission");
        capture("vpn-profile-ready.png", true);
        scrollClick(label(R.string.vpn_remove)); click(label(android.R.string.ok));
        await(() -> !store.exists(), "deleting the VPN profile removes encrypted configuration");
        sendKeyDownUpSync(KeyEvent.KEYCODE_BACK); sendKeyDownUpSync(KeyEvent.KEYCODE_BACK);
        openSettingsItem(R.string.vpn_title);
        await(() -> find(label(R.string.vpn_free_title), false) != null, "removing the profile restores free setup");
        sendKeyDownUpSync(KeyEvent.KEYCODE_BACK); sendKeyDownUpSync(KeyEvent.KEYCODE_BACK);
        new DownloadRegistry(app).clearListOnly();
    }

    private void advancedSettings() throws Exception {
        BrowserSettingsServices services = (BrowserSettingsServices) controller("settingsServices");
        java.util.concurrent.atomic.AtomicReference<String> pageTitle = new java.util.concurrent.atomic.AtomicReference<>("");
        java.util.concurrent.atomic.AtomicInteger completedLoads = new java.util.concurrent.atomic.AtomicInteger();
        org.mozilla.geckoview.GeckoSession page = currentSession();
        runOnMainSync(() -> {
            org.mozilla.geckoview.GeckoSession.ProgressDelegate progress = page.getProgressDelegate();
            page.setProgressDelegate(new org.mozilla.geckoview.GeckoSession.ProgressDelegate() {
                @Override public void onPageStart(org.mozilla.geckoview.GeckoSession s, String url) {
                    progress.onPageStart(s, url);
                }
                @Override public void onPageStop(org.mozilla.geckoview.GeckoSession s, boolean success) {
                    // Replacing the still-starting publisher page can commit
                    // example.com without another PageStart. Count a successful
                    // stop only for the actual browser location, which also
                    // follows Gecko's LocationChange; cancelled/other pages do
                    // not satisfy the initial load or the privacy reload check.
                    if (success && "https://example.com/".equals(
                            ((androidx.compose.runtime.MutableState<?>) controller("currentUrl")).getValue()))
                        completedLoads.incrementAndGet();
                    progress.onPageStop(s, success);
                }
            });
            org.mozilla.geckoview.GeckoSession.ContentDelegate original = contentDelegate(page);
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
        scrollClick(label(R.string.shield_fingerprint));
        await(() -> Boolean.TRUE.equals(runtime().getSettings().getFingerprintingProtection()), "fingerprinting toggle changes actual Gecko runtime");
        scrollClick(label(R.string.shield_fingerprint));
        scrollClick(label(R.string.shield_https));
        await(() -> runtime().getSettings().getAllowInsecureConnections() == org.mozilla.geckoview.GeckoRuntimeSettings.ALLOW_ALL, "HTTPS-only setting changes real runtime");
        scrollClick(label(R.string.shield_https));
        await(() -> runtime().getSettings().getAllowInsecureConnections() == org.mozilla.geckoview.GeckoRuntimeSettings.HTTPS_ONLY, "HTTPS-only restored");
        scrollClick(label(R.string.cookie_policy_first));
        await(() -> runtime().getSettings().getContentBlocking().getCookieBehavior() == org.mozilla.geckoview.ContentBlocking.CookieBehavior.ACCEPT_FIRST_PARTY, "cookie policy changes real storage behavior");
        scrollClick(label(R.string.cookie_policy_partition));
        capture("privacy-settings.png", true);
        scrollClick(label(R.string.cookie_clear));
        await(()->nativePositive()!=null,"native styled data confirmation");capture("confirmation-light.png",null);
        clickNativePositive();
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
        clickNativePositive();
        awaitLong(() -> uiCheck(() -> !addons.getItems().isEmpty() && !addons.getBusy()), "real Mozilla-signed extension installed", 90000);
        String addOnName = addons.getItems().get(0).metaData.name;
        scrollClick(addOnName);
        await(() -> uiCheck(() -> !addons.getItems().isEmpty() && !addons.getItems().get(0).metaData.enabled), "installed extension disabled through Gecko");
        click(addOnName);
        await(() -> uiCheck(() -> !addons.getItems().isEmpty() && addons.getItems().get(0).metaData.enabled), "installed extension re-enabled through Gecko");
        capture("extensions-installed.png", true);
        scrollClick(label(R.string.addon_remove)); clickNativePositive();
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
        clickNativePositive();
    }
    private AccessibilityNodeInfo nativePositive() {
        ArrayDeque<AccessibilityNodeInfo> queue = roots();
        while (!queue.isEmpty()) {
            AccessibilityNodeInfo node = queue.removeFirst();
            if (!node.refresh()) continue;
            if (node.isVisibleToUser() && node.isEnabled() && node.isClickable() &&
                "android:id/button1".equals(node.getViewIdResourceName())) return node;
            for (int i = 0; i < node.getChildCount(); i++) {
                AccessibilityNodeInfo child = node.getChild(i);
                if (child != null) queue.add(child);
            }
        }
        return null;
    }
    private void clickNativePositive() throws Exception {
        // Android's dialog theme uppercases labels (Tamam -> TAMAM).
        // Use its real positive-button ID, independent of display casing.
        await(() -> nativePositive() != null, "native confirmation positive button");
        tap(nativePositive()); waitForIdleSync(); SystemClock.sleep(100);
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
        if(android.os.Looper.myLooper()==android.os.Looper.getMainLooper()){
            try{return browserView().getSession();}catch(Exception error){throw new RuntimeException(error);}
        }
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
        tapBrowserCenter();
        await(()->currentSession()!=parent,"second user-opened popup ready for Android Back");
        org.mozilla.geckoview.GeckoSession backPopup=currentSession();
        systemBack();await(()->currentSession()==parent,"Android Back in a popup returns to its opener");
        require(uiCheck(()->!backPopup.isOpen()),"Back closed only the popup session");
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
        if(android.os.Looper.myLooper()==android.os.Looper.getMainLooper())return condition.getAsBoolean();
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
    private AccessibilityNodeInfo tabResult(String label) {
        // UiAutomation also exposes the covered Gecko document. Anchor to the
        // actual search editor's Compose overlay, so a page title underneath
        // cannot masquerade as a tab card or invalidate scope-isolation checks.
        AccessibilityNodeInfo scope = field(label(R.string.tabs_search_hint));
        while (scope != null && !"androidx.compose.ui.platform.ComposeView".equals(String.valueOf(scope.getClassName())))
            scope = scope.getParent();
        require(scope != null, "tab result checks require the actual tab overview overlay");
        ArrayDeque<AccessibilityNodeInfo> queue = new ArrayDeque<>();
        queue.add(scope);
        return find(queue, label, false);
    }
    private AccessibilityNodeInfo find(String label, boolean editable) {
        return find(roots(), label, editable);
    }
    private AccessibilityNodeInfo find(ArrayDeque<AccessibilityNodeInfo> queue, String label, boolean editable) {
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
        StringBuilder diagnostic=new StringBuilder();
        runOnMainSync(()->{
            try {
                diagnostic.append("uptime=").append(SystemClock.uptimeMillis()).append('\n');
                for(String name:new String[]{"atHome","imeVisible","addressEditing","menuShowing","settingsShowing","overviewShowing","shieldShowing","downloadsShowing","libraryShowing","videoFullScreen"}){
                    java.lang.reflect.Field flag;
                    try{flag=MainActivity.class.getDeclaredField(name);}catch(NoSuchFieldException e){flag=MainActivity.class.getDeclaredField(name+"$delegate");}
                    flag.setAccessible(true);Object value=flag.get(activity);
                    if(value instanceof androidx.compose.runtime.State<?>)value=((androidx.compose.runtime.State<?>)value).getValue();
                    diagnostic.append(name).append('=').append(value).append('\n');
                }
                java.lang.reflect.Method gate=MainActivity.class.getDeclaredMethod("canCollapseChrome");gate.setAccessible(true);
                diagnostic.append("canCollapse=").append(gate.invoke(activity)).append('\n');
                diagnostic.append("pin=").append(browserView().shouldPinOnScreen()).append(" engineHeight=").append(browserView().getHeight())
                    .append(" engineY=").append(browserView().getTranslationY()).append(" chromeVisibility=").append(chromeView().getVisibility())
                    .append(" chromeY=").append(chromeView().getTranslationY()).append('\n');
                Object motion=controller("toolbarMotion");
                for(java.lang.reflect.Field f:motion.getClass().getDeclaredFields())if(f.getType().isPrimitive()){
                    f.setAccessible(true);diagnostic.append("motion.").append(f.getName()).append('=').append(f.get(motion)).append('\n');
                }
                java.lang.reflect.Field pf=motion.getClass().getDeclaredField("policy");pf.setAccessible(true);Object policy=pf.get(motion);
                for(java.lang.reflect.Field f:policy.getClass().getDeclaredFields())if(f.getType().isPrimitive()){
                    f.setAccessible(true);diagnostic.append("policy.").append(f.getName()).append('=').append(f.get(policy)).append('\n');
                }
            }catch(Exception e){diagnostic.append(e.getClass().getSimpleName()).append(':').append(e.getMessage());}
        });
        try(FileOutputStream out=new FileOutputStream(new File(directory,"chrome-diagnostics.txt"))){out.write(diagnostic.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));}
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
            tree.append(node.getClassName()).append(" id=").append(node.getViewIdResourceName()).append(" visible=").append(node.isVisibleToUser())
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
    private AccessibilityNodeInfo systemCancelButton() {
        String systemCancel=android.content.res.Resources.getSystem().getString(android.R.string.cancel);
        String appCancel=getTargetContext().getString(android.R.string.cancel);
        ArrayDeque<AccessibilityNodeInfo> queue=roots();
        while(!queue.isEmpty()) {
            AccessibilityNodeInfo node=queue.removeFirst();
            if(!node.refresh())continue;
            if(node.isVisibleToUser()&&node.isEnabled()&&!getTargetContext().getPackageName().contentEquals(String.valueOf(node.getPackageName()))
                &&("android:id/button2".equals(node.getViewIdResourceName())||text(node).equalsIgnoreCase(systemCancel)||text(node).equalsIgnoreCase(appCancel)))return node;
            for(int i=0;i<node.getChildCount();i++) {AccessibilityNodeInfo child=node.getChild(i);if(child!=null)queue.add(child);}
        }
        return null;
    }
    private AccessibilityNodeInfo systemBrowserChoice() {
        String appName=getTargetContext().getApplicationInfo().loadLabel(getTargetContext().getPackageManager()).toString();
        ArrayDeque<AccessibilityNodeInfo> queue=roots();
        while(!queue.isEmpty()) {
            AccessibilityNodeInfo node=queue.removeFirst();
            if(!node.refresh())continue;
            if(node.isVisibleToUser()&&!getTargetContext().getPackageName().contentEquals(String.valueOf(node.getPackageName()))
                &&matches(node,appName))return node;
            for(int i=0;i<node.getChildCount();i++) {
                AccessibilityNodeInfo child=node.getChild(i);
                if(child!=null)queue.add(child);
            }
        }
        return null;
    }
    private AccessibilityNodeInfo clickTarget(String label) {
        java.util.List<AccessibilityWindowInfo> windows = new java.util.ArrayList<>(getUiAutomation().getWindows());
        windows.sort((left, right) -> Integer.compare(right.getLayer(), left.getLayer()));
        for (AccessibilityWindowInfo window : windows) {
            if (window.getType() != AccessibilityWindowInfo.TYPE_APPLICATION) continue;
            AccessibilityNodeInfo root = window.getRoot();
            if (root != null && root.refresh() && root.isVisibleToUser()) return clickTarget(root, label);
        }
        return null;
    }
    private AccessibilityNodeInfo clickTarget(AccessibilityNodeInfo root, String label) {
        // Labels and hit areas must belong to the same front window. A modal
        // can cover an identically named background action or editor.
        ArrayDeque<AccessibilityNodeInfo> queue = new ArrayDeque<>();
        queue.add(root);
        java.util.List<AccessibilityNodeInfo> labels = new java.util.ArrayList<>();
        java.util.List<Rect> buttons = new java.util.ArrayList<>();
        java.util.List<AccessibilityNodeInfo> editors = new java.util.ArrayList<>();
        while (!queue.isEmpty()) {
            AccessibilityNodeInfo node = queue.removeFirst();
            if (!node.refresh()) continue;
            if (node.isVisibleToUser()) {
                Rect bounds = new Rect(); node.getBoundsInScreen(bounds);
                boolean input = node.isEditable() || "android.widget.EditText".equals(String.valueOf(node.getClassName()));
                if (input) editors.add(node);
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
            // Skip only a label that really belongs to an editor (its own
            // description or a child of the EditText virtual node). A bounds
            // overlap is not ownership: the home address field under the
            // settings overlay and a page search box under the menu share
            // screen space with real buttons such as "Web sitesi".
            boolean inEditor = false;
            for (AccessibilityNodeInfo parent = node; parent != null && !inEditor; parent = parent.getParent())
                for (AccessibilityNodeInfo input : editors) if (input.equals(parent)) inEditor = true;
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
        java.util.concurrent.atomic.AtomicReference<Rect> hit = new java.util.concurrent.atomic.AtomicReference<>();
        await(() -> {
            AccessibilityNodeInfo node = clickTarget(label);
            if (node == null) return false;
            Rect bounds = new Rect(); node.getBoundsInScreen(bounds);
            if (bounds.isEmpty()) return false;
            hit.set(bounds); return true;
        }, "clickable control: " + label);
        // Keep the same ready hit target. A second tree lookup can race a
        // Compose semantics update between the wait and physical touch.
        Rect bounds = hit.get();
        tapAt(bounds.centerX(), bounds.centerY());
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
        tapAt(x,y,40);
    }
    private void tapAt(int x,int y,long holdMillis) {
        long time = SystemClock.uptimeMillis();
        MotionEvent down = MotionEvent.obtain(time, time, MotionEvent.ACTION_DOWN, x, y, 0);
        down.setSource(InputDevice.SOURCE_TOUCHSCREEN);
        require(getUiAutomation().injectInputEvent(down, true), "touch down");
        down.recycle(); SystemClock.sleep(holdMillis);
        MotionEvent up = MotionEvent.obtain(time, SystemClock.uptimeMillis(), MotionEvent.ACTION_UP, x, y, 0);
        up.setSource(InputDevice.SOURCE_TOUCHSCREEN);
        require(getUiAutomation().injectInputEvent(up, true), "touch up"); up.recycle();
    }
    private void capture(String name, Boolean light) throws Exception {
        waitForIdleSync(); SystemClock.sleep(250);
        Bitmap bitmap = getUiAutomation().takeScreenshot();
        require(bitmap != null, "real window screenshot");
        int pixel = bitmap.getPixel(8, bitmap.getHeight() / 2);
        require(light == null || (light ? Color.red(pixel) > 180 && Color.green(pixel) > 180 : Color.red(pixel) < 80),
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
