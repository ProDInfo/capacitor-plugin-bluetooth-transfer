import { WebPlugin } from '@capacitor/core';
import { BluetoothFileTransferPlugin } from './definitions';

export class BluetoothFileTransferWeb extends WebPlugin implements BluetoothFileTransferPlugin {
  constructor() {
    super({
      name: 'BluetoothFileTransfer',
      platforms: ['web'],
    });
  }

  async echo(options: { value: string }): Promise<{ value: string }> {
    console.log('ECHO', options);
    return options;
  }

  async sendObject(options: { filename: string, data: Object }): Promise<any> {
    console.log('sendObject', options);
    return null;
  }
}

const BluetoothFileTransfer = new BluetoothFileTransferWeb();

export { BluetoothFileTransfer };

import { registerWebPlugin } from '@capacitor/core';
registerWebPlugin(BluetoothFileTransfer);
