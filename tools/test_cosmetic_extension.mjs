// Self-contained execution tests for the shipped GeckoView content script.
// No third-party dependencies, website requests, browser accounts or telemetry.
import assert from "node:assert/strict";
import fs from "node:fs";
import vm from "node:vm";

const source = fs.readFileSync(
  "android/app-telefon/src/main/assets/eymen-cosmetic/cosmetic.js", "utf8"
);
async function check({enabled, rejectCookies = false, knownButton = false, hiddenButton = false, shouldReject = false, hasRoot = true, isFrame = false}) {
  const appended = [];
  const listeners = [];
  let rejected = 0;
  const root = {appendChild(node) { appended.push(node); }};
  const doc = {
    head: hasRoot ? root : null,
    documentElement: hasRoot ? root : null,
    getElementById() { return null; },
    querySelector(selector) { return knownButton && selector === "button#onetrust-reject-all-handler" ? {
      disabled:false, getClientRects(){return hiddenButton ? [] : [{}];}, click(){rejected++;}
    } : null; },
    createElement(tag) { return {tag, id: "", textContent: ""}; },
    addEventListener(name, callback, options) { listeners.push({name, callback, options}); }
  };
  const window = {};
  window.top = isFrame ? {} : window;
  const browser = {
    runtime: {
      sendNativeMessage(app, request) {
        assert.equal(app, "eymen_cosmetic");
        assert.deepEqual(JSON.parse(JSON.stringify(request)),
          {type:"cosmetic-policy-v1"});
        return shouldReject ? Promise.reject(new Error("offline")) :
          Promise.resolve({enabled,rejectCookies});
      }
    }
  };
  const context = {window, document:doc, location:{protocol:"https:"}, browser};
  vm.runInNewContext(source, context, {timeout:1000});
  await new Promise(resolve => setImmediate(resolve));
  return {appended, listeners, root, doc, rejected};
}
const denied = await check({enabled:false});
assert.equal(denied.appended.length, 0, "disabled leaves DOM intact");
const allowed = await check({enabled:true});
assert.equal(allowed.appended.length, 1, "enabled adds one CSS style");
assert.equal(allowed.appended[0].tag, "style");
assert.match(allowed.appended[0].textContent, /ins\.adsbygoogle/);
assert.doesNotMatch(allowed.appended[0].textContent, /iframe\s*\{/);
const offline = await check({enabled:true, shouldReject:true});
assert.equal(offline.appended.length, 0, "native messaging failure fails closed");
const frame = await check({enabled:true, isFrame:true});
assert.equal(frame.appended.length, 0, "iframe never gets an injected style");
const early = await check({enabled:true, hasRoot:false});
assert.equal(early.appended.length, 0);
assert.equal(early.listeners.length, 1);
assert.equal(early.listeners[0].name, "DOMContentLoaded");
early.doc.head = early.root;
early.listeners[0].callback();
assert.equal(early.appended.length, 1, "waiting for DOM root is supported");
console.log("Bundled cosmetic JS: allowed, denied, offline, iframe and early-DOM tests passed.");
const rejectOnly = await check({enabled:false,rejectCookies:true,knownButton:true});
assert.equal(rejectOnly.rejected,1,"cookie rejection works independently of ad CSS");
assert.equal(rejectOnly.appended.length,0,"cookie rejection does not imply cosmetic filtering");
assert.equal((await check({enabled:false,rejectCookies:false,knownButton:true})).rejected,0,"disabled rejection never clicks");
assert.equal((await check({enabled:false,rejectCookies:true,knownButton:true,hiddenButton:true})).rejected,0,"hidden controls never clicked");
assert.equal((await check({enabled:false,rejectCookies:true,knownButton:false})).rejected,0,"unknown consent controls never clicked");
console.log("Cookie rejection: explicit visible reject only, independently enabled and fail-closed tests passed.");
