import { app, BrowserWindow, shell } from "electron";
import path from "path";

// indicates whether to load from vite dev server or from build files (production)
const IS_DEV = process.env.ELECTRON_DEV === "development";

function createWindow(): BrowserWindow {
  const win = new BrowserWindow({
    width: 1280,
    height: 800,
    minWidth: 800,
    minHeight: 600,
    title: "Messaging Platform",

    // removes default OS chrome title bar on macOS. default can be used for windows/linux.
    titleBarStyle: process.platform === "darwin" ? "hiddenInset" : "default",

    webPreferences: {
      contextIsolation: true,
      nodeIntegration: false,
      preload: path.join(__dirname, "preload.js"),
      sandbox: !IS_DEV,
    },

    backgroundColor: "#0a0c11",
    show: false, // avoids white flash by showing only after content is ready
  });

  if (IS_DEV) {
    win.loadURL("http://localhost:3000");
    win.webContents.openDevTools();
  } else {
    const indexPath = path.join(process.resourcesPath, "web", "index.html");

    win.loadFile(indexPath);
  }

  win.once("ready-to-show", () => {
    win.show();
  });

  win.webContents.setWindowOpenHandler(({ url }) => {
    if (!url.startsWith("http://localhost") && !url.startsWith("file://")) {
      shell.openExternal(url);

      return { action: "deny" };
    }

    return { action: "allow" };
  });

  return win;
}

app.whenReady().then(() => {
  createWindow();

  app.on("activate", () => {
    if (BrowserWindow.getAllWindows().length === 0) {
      createWindow();
    }
  });
});

app.on("window-all-closed", () => {
  if (process.platform !== "darwin") {
    app.quit();
  }
});
