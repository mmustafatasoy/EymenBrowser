import { app, BrowserWindow, WebContentsView, session } from "electron";
import path from "node:path";

let window: BrowserWindow | null = null;
let page: WebContentsView | null = null;

function trialUrl(raw: string | undefined): string {
  try {
    const url = new URL(raw ?? "https://example.org/");
    return url.protocol === "https:" && !url.username && !url.password
      ? url.href : "https://example.org/";
  } catch {
    return "https://example.org/";
  }
}

function updateBounds(): void {
  if (!window || !page || window.isDestroyed()) return;
  const [width, height] = window.getContentSize();
  page.setBounds({ x: 0, y: 0, width, height });
}

function open(): void {
  window = new BrowserWindow({
    width: 1200,
    height: 780,
    show: true,
    webPreferences: {
      nodeIntegration: false,
      contextIsolation: true,
      sandbox: true,
      webSecurity: true,
      navigateOnDragDrop: false
    }
  });

  window.webContents.setWindowOpenHandler(() => ({ action: "deny" }));
  window.webContents.on("will-navigate", event => event.preventDefault());
  window.loadFile(path.join(__dirname, "../dist/index.html")).catch(() => {});

  page = new WebContentsView({
    webPreferences: {
      partition: "persist:eymen-motor-denemesi",
      nodeIntegration: false,
      contextIsolation: true,
      sandbox: true,
      webSecurity: true
    }
  });
  window.contentView.addChildView(page);
  updateBounds();
  page.webContents.setWindowOpenHandler(() => ({ action: "deny" }));
  page.webContents.on("will-navigate", (event, requested) => {
    try {
      if (new URL(requested).protocol !== "https:") event.preventDefault();
    } catch {
      event.preventDefault();
    }
  });
  page.webContents.loadURL(trialUrl(process.env.EYMEN_TEST_URL)).catch(() => {});

  window.on("resize", updateBounds);
  window.on("closed", () => {
    if (page) {
      page.webContents.close();
      page = null;
    }
    window = null;
  });
}

app.whenReady().then(() => {
  const browserSession = session.fromPartition("persist:eymen-motor-denemesi");
  browserSession.setPermissionRequestHandler((_contents, _permission, callback) => callback(false));
  browserSession.setPermissionCheckHandler(() => false);
  open();
});
app.on("window-all-closed", () => {
  if (process.platform !== "darwin") app.quit();
});
app.on("activate", () => {
  if (BrowserWindow.getAllWindows().length === 0) open();
});
