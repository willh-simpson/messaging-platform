declare module "*.module.css" {
  const classes: Record<string, string>;
  export default classes;
}

interface ElectronBridge {
  platform: "win32" | "darwin" | "linux";
  version: string;
  notify: (title: string, body: string) => void;
}

interface Window {
  electron?: ElectronBridge;
}
