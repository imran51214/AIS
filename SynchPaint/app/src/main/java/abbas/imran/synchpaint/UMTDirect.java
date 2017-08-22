package abbas.imran.synchpaint;


//Broadcast Receiver
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;

//wifi p2p
import android.icu.util.IslamicCalendar;
import android.net.wifi.WifiManager;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest;

import android.content.Context;
import android.os.Handler;
import android.widget.Toast;

import java.lang.reflect.Method;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;


/**
 * Created by imran on 23-May-17.
 */


public class UMTDirect implements WifiP2pManager.ConnectionInfoListener,WifiP2pManager.PeerListListener {


    // Service Details
    private static final String SERVICE_REG_TYPE = "_presence._tcp";
    private static final String TXTRECORD_PROP_AVAILABLE = "available";
    private int SERVER_PORT;
    private String Service_Name;
    private WifiP2pDnsSdServiceInfo service;
    private boolean isServiceHost=false;
    private boolean isGroupOwner=false;


    //Socket  Info
    private boolean isClientRegistrationStarted=false;
    private boolean isClientConnected=false;

    // Service Request
    WifiP2pDnsSdServiceRequest serviceRequest;
    private boolean isServiceRequested=false;



    UMTCallback umtCallbackConnectToService;


    //wifi p2p
    private WifiP2pManager manager;
    private WifiP2pManager.Channel channel;


    private static WifiManager wifiManager;

    private Context mContext;


    // Broadcast Receiver
    private final IntentFilter mIntentFilter = new IntentFilter();
    BroadcastReceiver mReceiver;

    //Data Container of Clients
    TreeMap<Integer, String> dataContainer;



    // Available Peers List
    private List<WifiP2pDevice> availablePeers = new ArrayList<WifiP2pDevice>();

    // Registered Peers List
    //private List<WifiP2pDevice> registeredPeers = new ArrayList<WifiP2pDevice>();

    private Collection<WifiP2pDevice> registeredPeers;


    //Group Members
    TreeMap<Integer, String> groupMembers;



    //Device Details
    WifiP2pDevice CurrentDevice;
   // private String DeviceMacAddress=null;


    Handler mainHandler;

    UMTConnection umtConnection;


    public UMTDirect(Context context, Handler handler){

        mainHandler=handler;
        mContext=context;

        //Enable WIFI, if disabled
        if (!UMTDirect.isWiFiEnabled(mContext))
        {
            UMTDirect.enableWiFi(mContext);
        }

        initializeDirect();

        //Get Device Mac Address
       // WifiManager wiman = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
       // DeviceMacAddress = wiman.getConnectionInfo().getMacAddress();

    }

    private void initializeDirect(){


        if (isServiceHost()){

            // Remove Service

            removeService(new UMTCallback() {
                @Override
                public void response(boolean cmdStatus, int failureReason) {
                    if (cmdStatus){

                        Toast.makeText(mContext, "Service removed successfully", Toast.LENGTH_SHORT).show();
                    }
                    else{
                        Toast.makeText(mContext, "Failed to Remove a service", Toast.LENGTH_SHORT).show();

                    }

                }
            });

            // Remove Service

            WifiP2pManager.ActionListener doNothing = new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {

                }

                @Override
                public void onFailure(int reason) {

                }
            };



            // Cancel any ongoing p2p group negotiation
            manager.cancelConnect(channel, doNothing);

            manager.clearLocalServices(channel,doNothing);
            manager.clearServiceRequests(channel, doNothing);


            if (isServiceRequested) {
                //Remove a specified service discovery request added with addServiceRequest
                manager.removeServiceRequest(channel, serviceRequest, doNothing);
            }


        }



        if (umtConnection != null)
      {
          umtConnection.tearDown();
      }



        umtConnection=new UMTConnection(mainHandler,mContext);


        //  Indicates a change in the Wi-Fi P2P status.
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);

        // Indicates a change in the list of available peers.
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);

        // Indicates the state of Wi-Fi P2P connectivity has changed.
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);

        // Indicates this device's details have changed.
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);


        //////////////////////////////////////////////////////////////////////////
        manager = (WifiP2pManager) mContext.getSystemService(Context.WIFI_P2P_SERVICE);

        // This method initialize() returns a WifiP2pManager.Channel object, which you'll use later to connect your app to the Wi-Fi P2P framework.
        channel = manager.initialize(mContext, mContext.getMainLooper(), null);

        mReceiver = new UMTWiFiDirectBroadcastReceiver(manager, channel, this,mContext);

        // Register the intent filter and broadcast receiver
        mContext.registerReceiver(mReceiver,mIntentFilter);
        //////////////////////////////////////////////////////////////////////////


        isServiceHost=false;
        isGroupOwner=false;
        isClientRegistrationStarted=false;
        isClientConnected=false;

    }


    void setRegisteredPeers(Collection<WifiP2pDevice> registeredPeers){

        List<String>  registeredPeersAddresses = new ArrayList<String>();

        this.registeredPeers=registeredPeers;
        int i=0;

        for (WifiP2pDevice device : registeredPeers)
        {
           registeredPeersAddresses.add(i,device.deviceAddress.toLowerCase());
            i=i+1;
        }

        if (isServiceHost && isClientRegistrationStarted){
            umtConnection.synchActiveRegisteredPeersSockets((ArrayList<String>) registeredPeersAddresses);
        }

    }




   void setCurrentDevice(WifiP2pDevice deviceInfo){

        CurrentDevice=deviceInfo;

    }

    public static boolean isWiFiEnabled(Context context) {

        wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        return wifiManager.isWifiEnabled();

    }



    public static void enableWiFi(Context context) {
        wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        wifiManager.setWifiEnabled(true);

    }

    public static void disableWiFi(Context context) {
        wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        wifiManager.setWifiEnabled(false);

    }

    public  boolean isServiceHost() {

        return isServiceHost;

    }




    public void discoverPeers(final UMTCallback callback){

        //Discover Peers will initiate peer discovery
        manager.discoverPeers(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                callback.response(true,0);

            }

            @Override
            public void onFailure(int reason) {

                callback.response(false,reason);

            }
        });
        //Discover Peers

    }



    public void broadcastService(final String serviceInstance,Integer portNumber,final UMTCallback callback){

         SERVER_PORT=portNumber;

        // Register service
        Map<String, String> record = new HashMap<String, String>();
        //record.put(TXTRECORD_PROP_AVAILABLE, "visible");
        record.put("SERVICE_PORT", Integer.toString(SERVER_PORT));

        service = WifiP2pDnsSdServiceInfo.newInstance(
                serviceInstance, SERVICE_REG_TYPE, record);


                // Register a local service for service discovery.
                manager.addLocalService(channel, service, new WifiP2pManager.ActionListener() {

                    @Override
                    public void onSuccess() {
                        isServiceHost=true;
                        callback.response(true,0);

                    }

                    @Override
                    public void onFailure(int error) {
                        isServiceHost=false;
                        callback.response(false,error);

                    }
                });
                // Register a local service for service discovery.

    }


    private void connectToDevice(String deviceAddress,Integer GroupIntent) {

        WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = deviceAddress;
        config.wps.setup = WpsInfo.PBC;
        config.groupOwnerIntent=GroupIntent;


        manager.connect(channel, config, new WifiP2pManager.ActionListener() {

            @Override
            public void onSuccess() {
                //callback.response(true,0);
            }

            @Override
            public void onFailure(int reason) {
               // callback.response(false,reason);
            }
        });
    }


    public void createGroup(final UMTCallback callback){

        //Create a p2p group with the current device as the group owner.
        manager.createGroup(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                callback.response(true,0);
            }

            @Override
            public void onFailure(int reason) {
                callback.response(false,reason);
            }
        });


    }



    public void connectToService(final String serviceInstance,final UMTCallback umtCallback){


        umtCallbackConnectToService=umtCallback;


        //Discover that the Service is available
 /*
         * Register listeners for DNS-SD services. These are callbacks invoked
         * by the system when a service is actually discovered.
         */

        final HashMap<String, String> devices = new HashMap<String, String>();

        manager.setDnsSdResponseListeners(channel,
                new WifiP2pManager.DnsSdServiceResponseListener() {

                    @Override
                    public void onDnsSdServiceAvailable(String instanceName,
                                                        String registrationType, WifiP2pDevice srcDevice) {

                        // A service has been discovered. Is this our app?
                        String str="";

                        if (instanceName.equalsIgnoreCase(serviceInstance)) {


                           String serviceAddress;

                           serviceAddress=srcDevice.deviceAddress;

                           Toast.makeText(mContext, instanceName.toString() + " Service Found on " + srcDevice.deviceName.toString() + " Mac:" + serviceAddress ,Toast.LENGTH_SHORT).show();

                            if (devices.containsKey(srcDevice.deviceAddress)){

                               SERVER_PORT= Integer.parseInt(devices.get(srcDevice.deviceAddress));
                               Service_Name=serviceInstance;

                            }

                            connectToDevice(srcDevice.deviceAddress,0);


                        }

                    }
                }, new WifiP2pManager.DnsSdTxtRecordListener() {

                    /**
                     * A new TXT record is available. Pick up the advertised
                     * buddy name.
                     */
                    @Override
                    public void onDnsSdTxtRecordAvailable(
                            String fullDomainName, Map<String, String> record,
                            WifiP2pDevice device) {

                        devices.put(device.deviceAddress,record.get("SERVICE_PORT"));
                        //Log.d(TAG, device.deviceName + " is "  + record.get(TXTRECORD_PROP_AVAILABLE));
                    }
                });

        // After attaching listeners, create a service request and initiate
        //

        serviceRequest = WifiP2pDnsSdServiceRequest.newInstance();

        //Add a service discovery request.
        manager.addServiceRequest(channel, serviceRequest,
                new WifiP2pManager.ActionListener() {

                    @Override
                    public void onSuccess() {

                        isServiceRequested=true;
                        Toast.makeText(mContext, "Added service discovery request", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFailure(int arg0) {
                        Toast.makeText(mContext, "Failed adding service discovery request", Toast.LENGTH_SHORT).show();

                    }
                });


        manager.discoverServices(channel, new WifiP2pManager.ActionListener() {

            @Override
            public void onSuccess() {
               // umtCallback.response(true,0);
            }

            @Override
            public void onFailure(int arg0) {
               // umtCallback.response(false ,arg0);
            }
        });



    }


    public void disconnectWithService(final UMTCallback umtCallback){

        // Remove Service
        if (isServiceHost()){


            removeService(new UMTCallback() {
                @Override
                public void response(boolean cmdStatus, int failureReason) {
                    if (cmdStatus){

                        Toast.makeText(mContext, "Service removed successfully", Toast.LENGTH_SHORT).show();
                    }
                    else{
                        Toast.makeText(mContext, "Failed to Remove a service", Toast.LENGTH_SHORT).show();

                    }

                }
            });

        }
        // Remove Service


       // Remove the current p2p group.
         manager.removeGroup(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                umtCallback.response(true,0);
                initializeDirect();

            }


            @Override
            public void onFailure(int reason) {
                umtCallback.response(false,1);

            }
        });
    // Remove the current p2p group.


    }




    private void disconnectWithGroup(final UMTCallback callback){

        // Remove the current p2p group.
        manager.removeGroup(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                callback.response(true,0);

            }

            @Override
            public void onFailure(int reason) {
                callback.response(false,1);

            }
        });
        // Remove the current p2p group.



    }

    private void removeService(final UMTCallback callback){


        WifiP2pManager.ActionListener doNothing = new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {

            }

            @Override
            public void onFailure(int reason) {

            }
        };

        // Clear all registered local services of service discovery.
        manager.clearLocalServices(channel,doNothing);


       if (isServiceHost()){


            manager.removeLocalService(channel, service, new WifiP2pManager.ActionListener() {

                @Override
                public void onSuccess() {
                    callback.response(true,0);

                }

                @Override
                public void onFailure(int error) {
                    callback.response(false,error);


                }
            });


        }

        else {
            callback.response(false,0);

        }



    }


    public Integer getTimeStamp(){
        return umtConnection.getTimeStamp();
    }

    public void broadcastMessage(String msg){

        umtConnection.synchDoc(msg);

    }


    public void teardown(){

        //UnRegister Receiver
        mContext.unregisterReceiver(mReceiver);
        umtConnection.tearDown();

        if (isServiceHost()){
            serviceHostTearDown();
        }
        else {
            serviceClientTearDown();
        }

    }


    private void serviceHostTearDown()
    {

        // Remove Service
        if (isServiceHost()){


            removeService(new UMTCallback() {
                @Override
                public void response(boolean cmdStatus, int failureReason) {
                    if (cmdStatus){

                        Toast.makeText(mContext, "Service removed successfully", Toast.LENGTH_SHORT).show();
                    }
                    else{
                        Toast.makeText(mContext, "Failed to Remove a service", Toast.LENGTH_SHORT).show();

                    }

                }
            });

        }
        // Remove Service


        WifiP2pManager.ActionListener doNothing = new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {

            }

            @Override
            public void onFailure(int reason) {

            }
        };


        // Clear all registered local services of service discovery.
        manager.clearLocalServices(channel,doNothing);


        // Cancel any ongoing p2p group negotiation
        manager.cancelConnect(channel, doNothing);

        //Stop an ongoing peer discovery
        manager.stopPeerDiscovery(channel, doNothing);


        disconnectWithGroup(new UMTCallback() {
            @Override
            public void response(boolean cmdStatus, int failureReason) {
                if (cmdStatus) {
                    Toast.makeText(mContext, "Group Disconnection Successfull", Toast.LENGTH_SHORT).show();
                } else {

                   // Toast.makeText(mContext, "Group Disconnection Failed", Toast.LENGTH_SHORT).show();

                }

            }
        });
    }

    private void serviceClientTearDown()
    {


        WifiP2pManager.ActionListener doNothing = new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {

            }

            @Override
            public void onFailure(int reason) {

            }
        };



        if (isServiceRequested) {

            //Remove a specified service discovery request added with addServiceRequest
            manager.removeServiceRequest(channel, serviceRequest, doNothing);

        }


        // Cancel any ongoing p2p group negotiation
        manager.cancelConnect(channel, doNothing);

        //Clear all registered service discovery requests.
        manager.clearServiceRequests(channel, doNothing);

        //Stop an ongoing peer discovery
        manager.stopPeerDiscovery(channel, doNothing);


        disconnectWithGroup(new UMTCallback() {
            @Override
            public void response(boolean cmdStatus, int failureReason) {
                if (cmdStatus) {
                    Toast.makeText(mContext, "Group Disconnection Successfull", Toast.LENGTH_SHORT).show();
                } else {

                   // Toast.makeText(mContext, "Group Disconnection Failed", Toast.LENGTH_SHORT).show();

                }

            }
        });

    }


    public  void teardownSockets(Boolean groupAlive){

        groupMembers = new TreeMap<Integer, String>();

        if (isClientConnected){
            Toast.makeText(mContext, "Group Owner Left/Group Disolved", Toast.LENGTH_SHORT).show();


            groupMembers=umtConnection.getGroupMembers();

        }
        
        if (isClientRegistrationStarted) {
            initializeDirect();
        }
        else {
            umtConnection.tearDown();
            isServiceHost=false;
            isGroupOwner=false;
            isClientRegistrationStarted=false;
            isClientConnected=false;

            if (groupMembers.size()>1) {

                reformGroup();
            }


        }




    }


    private void reformGroup(){

        String GroupOwnerAddress;

        GroupOwnerAddress=groupMembers.lastEntry().getValue();

        if(GroupOwnerAddress.equals(CurrentDevice.deviceAddress)){
            initializeDirect();


          broadcastService(Service_Name,SERVER_PORT,new UMTCallback() {
              @Override
              public void response(boolean cmdStatus, int failureReason) {

                  if (cmdStatus) {
                      Toast.makeText(mContext, "Service Registered", Toast.LENGTH_SHORT).show();

                      createGroup(new UMTCallback() {
                          @Override
                          public void response(boolean cmdStatus, int failureReason) {

                              if (cmdStatus){
                                  Toast.makeText(mContext, "Group Created Successfully", Toast.LENGTH_SHORT).show();
                              }

                          }
                      });
                  } else {

                      Toast.makeText(mContext, "Service Not Registered", Toast.LENGTH_SHORT).show();

                  }

              }
          });

        }

        else {

            connectToService(Service_Name, new UMTCallback() {
                @Override
                public void response(boolean cmdStatus, int failureReason) {
                    if (cmdStatus) {
                        Toast.makeText(mContext, "Connected with the service", Toast.LENGTH_SHORT).show();
                    }
                    else {
                        Toast.makeText(mContext, "Service discovery failed", Toast.LENGTH_SHORT).show();

                    }

                }
            });

        }

    }


    private void removeGroup(){


       //Disconnect Device

        if (manager != null && channel != null) {
            manager.requestGroupInfo(channel, new WifiP2pManager.GroupInfoListener() {
                @Override
                public void onGroupInfoAvailable(WifiP2pGroup group) {
                    if (group != null && manager != null && channel != null
                            && group.isGroupOwner()) {
                        //Remove the current p2p group.
                        manager.removeGroup(channel, new WifiP2pManager.ActionListener() {

                            @Override
                            public void onSuccess() {
                               // Log.d(TAG, "removeGroup onSuccess -");
                            }

                            @Override
                            public void onFailure(int reason) {
                             // Log.d(TAG, "removeGroup onFailure -" + reason);
                            }
                        });
                    }
                }
            });
        }


        //Disconnect Device
    }

    @Override
    public void onConnectionInfoAvailable(WifiP2pInfo info) {
         /* This method is automatically called when we connect to a device.
         * The group owner accepts connections using a server socket and then spawns a
         * client socket for every client.*/



                if (info.isGroupOwner && !isClientRegistrationStarted){

                    umtConnection.startServerSocket(SERVER_PORT);
                    isClientRegistrationStarted=true;
                    isGroupOwner=true;

                    Toast.makeText(mContext, "Group Owner", Toast.LENGTH_SHORT).show();

                }
                else if (!isClientConnected & !info.isGroupOwner & info.groupFormed) {

                    umtCallbackConnectToService.response(true,0);

                    InetAddress mAddress;
                    mAddress=info.groupOwnerAddress;
                    isGroupOwner=false;

                    umtConnection.connectToServer(mAddress,SERVER_PORT ,CurrentDevice.deviceAddress);
                    isClientConnected=true;

                    Toast.makeText(mContext, "Its Client", Toast.LENGTH_SHORT).show();

                }


    }



    @Override
    public void onPeersAvailable(WifiP2pDeviceList peerList) {

        Collection<WifiP2pDevice> refreshedPeers = peerList.getDeviceList();

        if (!refreshedPeers.equals(availablePeers)) {
            availablePeers.clear();
            availablePeers.addAll(refreshedPeers);

        }


    }
}
