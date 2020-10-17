declare module '@capacitor/core/dist/esm/core-plugin-definitions' {
  interface PluginRegistry {
    BluetoothFileTransfer: BluetoothFileTransferPlugin;
  }
}

export interface BluetoothFileTransferPlugin {
  echo(options: { value: string }): Promise<{ value: string }>;
  sendObject(options: { filename: string, data: Object }): Promise<any>;
}
