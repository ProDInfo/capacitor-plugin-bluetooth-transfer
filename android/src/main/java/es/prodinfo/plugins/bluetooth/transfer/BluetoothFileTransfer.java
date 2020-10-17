package es.prodinfo.plugins.bluetooth.transfer;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import com.getcapacitor.JSObject;
import com.getcapacitor.NativePlugin;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

@NativePlugin(
        permissions={ Manifest.permission.BLUETOOTH_ADMIN },
        requestCodes={ BluetoothFileTransfer.SEND_FILE_BLUETOOTH } // register request code(s) for intent results
)
public class BluetoothFileTransfer extends Plugin {
    //protected static final int SEND_FILE  = 3;
    protected static final int SEND_FILE_BLUETOOTH = 29500; // Unique request code
    protected static final String TAG = "BluetoothFileTransfer";

    @PluginMethod
    public void echo(PluginCall call) {
        String value = call.getString("value");
        Toast.makeText(getContext(), value, Toast.LENGTH_LONG).show();
        JSObject ret = call.getData();
        call.success(ret);
    }

    @PluginMethod
    public void sendObject(PluginCall call) {
        if (!hasRequiredPermissions()) {
            saveCall(call);
            pluginRequestAllPermissions();
        } else {
            sendFileObject(call);
        }
    }

    //@SuppressWarnings("MissingPermission")
    private void sendFileObject(PluginCall call) {
        try {
            String filename = call.getString("filename");
            JSObject jsonData = call.getObject("data");

            //Create a directory
            File directory = new File(this.getContext().getFilesDir().getPath()+File.separator+"bluetooth");
            if(!directory.exists())
                directory.mkdir();

            //Create a file
            File file = new File(directory, filename);
            if(!file.exists()){
                try {
                    file.createNewFile();
                } catch (IOException e) {
                    throw new Exception("Can't create file to send");
                }
            }
            Log.d(TAG,"file " + file.getPath() + " created.");

            if (!file.canWrite()) {
                throw new Exception("Permissions to save file denied");
            }

            //write the String to file
            BufferedWriter out;
            FileWriter fileWriter = new FileWriter(file.getPath());
            out = new BufferedWriter(fileWriter);
            out.write(jsonData.toString());
            out.close();

            //get Uri for share to bluetooth app
            Uri fileUri = FileProvider.getUriForFile(this.getContext(),  this.getContext().getPackageName() + ".opener.provider", file);

            // Generate Intent
            Intent i = new Intent();
            i.setAction(Intent.ACTION_SEND);
            i.putExtra(Intent.EXTRA_TEXT, jsonData.toString());
            i.putExtra(Intent.EXTRA_STREAM, fileUri);
            i.setType("text/plain");

            Context ctx = this.getContext();
            final PackageManager pm = ctx.getPackageManager();
            List<ResolveInfo> list = pm.queryIntentActivities(i, 0);

            if (list.size() > 0) {
                String packageName = null;
                String className = null;
                boolean found = false;

                for (ResolveInfo info : list) {
                    packageName = info.activityInfo.packageName;
                    if (packageName.equals("com.android.bluetooth")) {
                        className = info.activityInfo.name;
                        found = true;
                        break;
                    }
                }
                //CHECK BLUETOOTH available or not
                if (!found) {
                    throw new Exception("Bluetooth not been found");
                }

                i.setClassName(packageName, className);
                this.getContext().grantUriPermission(packageName, fileUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                saveCall(call);
                this.startActivityForResult(call, i, SEND_FILE_BLUETOOTH);
            }
        } catch (Exception ex) {
            String messageError = ex.toString();
            if (ex.getMessage() != null) {
                messageError = ex.getMessage();
            }
            Log.d(TAG,  messageError);
            Toast.makeText(getContext(), messageError, Toast.LENGTH_LONG).show();
            call.error(messageError);
        }
    }
    // in order to handle the intents result, you have to @Override handleOnActivityResult

    @Override
    protected void handleOnActivityResult(int requestCode, int resultCode, Intent data) {
        super.handleOnActivityResult(requestCode, resultCode, data);

        Log.d(TAG, "handle");

        // Get the previously saved call
        PluginCall savedCall = getSavedCall();

        if (savedCall == null) {
            Log.d(TAG,"handleOnActivityResult savedCall is null.");
            return;
        }

        if (requestCode == SEND_FILE_BLUETOOTH) {
            Log.d(TAG,"Transfer finished.");
            savedCall.resolve();
        } else {
            Log.d(TAG,"handleOnActivityResult requestCode: "+Integer.toString(requestCode));
            Log.d(TAG,"handleOnActivityResult resultCode: "+Integer.toString(resultCode));
            Log.d(TAG,"Transfer is cancelled");
            Toast.makeText(getContext(), "Transfer is cancelled", Toast.LENGTH_LONG).show();
            savedCall.error("Transfer is cancelled");
        }
    }


    @Override
    protected void handleRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.handleRequestPermissionsResult(requestCode, permissions, grantResults);

        PluginCall savedCall  = getSavedCall();
        if (savedCall  == null) {
            Log.d(TAG, "No stored plugin call for permissions request result");
            return;
        }
 
        for(int result : grantResults) {
            if (result == PackageManager.PERMISSION_DENIED) {
                Log.d(TAG, "User denied permission");
                savedCall.error("User denied permission");
                return;
            }
        }

        if (savedCall.getMethodName().equals("sendObject")) {
            sendFileObject(savedCall);
        } else {
            savedCall.resolve();
            savedCall.release(bridge);
        }
    }
}
