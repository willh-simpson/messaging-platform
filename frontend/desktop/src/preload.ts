import { contextBridge, ipcRenderer } from "electron";

contextBridge.exposeInMainWorld("electron", {
  platform: process.platform,
  version: process.env.npm_package_version ?? "unknown",
  notify: (title: string, body: string) => {
    ipcRenderer.send("show-notification", { title, body });
  },
});
