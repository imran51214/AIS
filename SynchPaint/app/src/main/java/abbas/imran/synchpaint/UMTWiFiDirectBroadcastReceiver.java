package abbas.imran.synchpaint;


import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pManager;
import android.util.Log;
import android.widget.Toast;
import android.widget.Toast;

import java.util.Collection;
import java.util.Iterator;

/**
 * Created by imran on 07-Mar-17.
 */
//BroadcastReceiver class that you'll use to listen for changes to the System's Wi-Fi P2P state.

public class UMTWiFiDirectBroadcastReceiver extends BroadcastReceiver {

    private WifiP2pManager mManager;
    private WifiP2pManager.Channel mChannel;
    private UMTDirect mUMTDirect;
    private Context mContext;




    public UMTWiFiDirectBroadcastReceiver(WifiP2pManager manager, WifiP2pManager.Channel channel,
                                          UMTDirect activity, Context mContext) {
        super();
        this.mManager = manager;
        this.mChannel = channel;
        this.mUMTDirect = activity;
        this.mContext=mContext;



    }



    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();



        // Check to see if Wi-Fi is enabled and notify appropriate activity
        if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {

            // Check to see if Wi-Fi is enabled and notify appropriate activity
            // UI update to indicate wifi p2p status.
            int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
            if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                // Wifi Direct mode is enabled
               // mActivity.setIsWifiP2pEnabled(true);
            } else {

              //  WifiP2pDevice device = intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE);
              //   Toast.makeText(mContext, "Device Disconnect " + device.deviceName +  device.deviceAddress.toString()  , Toast.LENGTH_SHORT).show();

                // It's a disconnect
                // mActivity.deviceDisconnect(device.deviceAddress.toString());

               // mActivity.setIsWifiP2pEnabled(false);
                //mActivity.resetData();

            }
           // Log.d(mActivity.TAG, "P2P state changed - " + state);



        }
        // Call WifiP2pManager.requestPeers() to get a list of current peers
        else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {


            if (mManager != null) {
              mManager.requestPeers(mChannel, (WifiP2pManager.PeerListListener) mUMTDirect);
            }


          //  Log.d(mActivity.TAG, "P2P Peer Discoverd - " + action);


        }
        // Respond to new connection or disconnections
        else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {


            if (mManager == null) {
                return;
            }

            NetworkInfo networkInfo = (NetworkInfo) intent
                    .getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);


            WifiP2pGroup groupInfo = ( WifiP2pGroup) intent
                    .getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_GROUP);



            //Get the list of clients currently part of the p2p group
            Collection<WifiP2pDevice> groupMembers=groupInfo.getClientList();

            mUMTDirect.setRegisteredPeers(groupMembers);


           // Toast.makeText(mContext, "Group Members:" + groupMembers , Toast.LENGTH_SHORT).show();


            if (mUMTDirect.isServiceHost())
            {

                String deviceInfo="";

                for (WifiP2pDevice device : groupMembers)
                {
                   deviceInfo=deviceInfo + device.deviceName;
                }

                if (!deviceInfo.equals("")){
                       Toast.makeText(mContext, "Registered Members:" + Integer.toString(groupMembers.size()) +  " " + deviceInfo, Toast.LENGTH_SHORT).show();
                }


            }


            if (networkInfo.isConnected()) {

                // we are connected with the other device, request connection
                // info to find group owner IP

                mManager.requestConnectionInfo(mChannel,
                        (WifiP2pManager.ConnectionInfoListener) mUMTDirect);

               //Toast.makeText(mContext, "Request Connection Information:WiFiDirectBroadcastReceiver", Toast.LENGTH_SHORT).show();

            } else {




                String reason=networkInfo.getReason();


                if (groupMembers.isEmpty()){
                    mUMTDirect.teardownSockets(false);
                }
                else {
                    mUMTDirect.teardownSockets(true);
                }




            }



        }
        // Respond to this device's wifi state changing
        else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {

           WifiP2pDevice device = intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE);
            mUMTDirect.setCurrentDevice(device);


        }

    }


}
