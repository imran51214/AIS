package abbas.imran.synchpaint;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;


public class UMTConnection {

    private int SERVER_PORT;
    private InetAddress mAddress;
    private String DeviceMacAddress;
    private UMTServer mUMTServer;
    private UMTClient mUMTClient;
    private Hashtable registeredClients = null;
    TreeMap<Integer, String> groupMembers;
    TreeMap<Integer, String> dataContainer;
    Integer Priority=1;
    private static final String TAG = "DocConnection";
    private Socket mSocket = null;
    PrintWriter prClient=null;
    private Boolean isClient=false;
    private Boolean serverSocketCreated=false;
    private Boolean clientSocketCreated=false;
    private Integer globalTimeStamp=0;
    private Integer timeStamp=0;


    private Context mContext;
    Handler mainHandler;


    public UMTConnection(Handler handler,Context context) {

        mainHandler=handler;
        mContext=context;

        registeredClients = new Hashtable();
        dataContainer= new TreeMap<Integer, String>();
        groupMembers = new TreeMap<Integer, String>();

    }




    public void startServerSocket(Integer portNumber){

        SERVER_PORT=portNumber;
        mUMTServer = new UMTServer();
        serverSocketCreated=true;


    }


    synchronized public Integer GenerateTimeStamp(){

        globalTimeStamp=globalTimeStamp+1;
        return globalTimeStamp;
    }


    public Integer getTimeStamp(){

        timeStamp=0;

        if (isClient)
        {
            if (prClient != null) {
                prClient.println("TimeStampRequest");
            }
        }

        else {
              timeStamp=GenerateTimeStamp();
        }


        while (timeStamp.equals(0)){
        }

        return timeStamp;

    }

    public TreeMap getGroupMembers(){

        return groupMembers;

    }


    public void synchActiveRegisteredPeersSockets(ArrayList<String> registeredPeersAddresses){


        Enumeration clients = registeredClients.keys();



        while(clients.hasMoreElements()){

            String deviceMac;
            deviceMac=clients.nextElement().toString();


            try {

                if (!registeredPeersAddresses.contains(deviceMac)) {

                    //After disconnect close the client socket and remove it from the list
                    Socket clientSocket;
                    clientSocket= (Socket) registeredClients.get(deviceMac);
                    clientSocket.close();
                    registeredClients.remove(deviceMac);

                    Toast.makeText(mContext, deviceMac + " Removed", Toast.LENGTH_SHORT).show();


                }

            }
            catch (Exception e){

            }


        }


    }


    //Synch Doc

    public void synchDoc(String Message){


        if (isClient){

            updateServer(Message);

        }
        else {

            informAll(Message);

        }

    }

    //Synch Doc


    //Send group members information to all clients

    public void sendGroupInfoToClients(){
        UMTData umtDataSend=new UMTData();
        List<List<String >> groupMembersList= new ArrayList<List<String>>();
        List<String> groupMember;

        umtDataSend.Put("type","GroupInfo");

        Set set = groupMembers.entrySet();

        Iterator iterator = set.iterator();

        while(iterator.hasNext()) {

           Map.Entry mentry = (Map.Entry)iterator.next();

           Integer Priority = Integer.parseInt(mentry.getKey().toString());
           String MacAddress = mentry.getValue().toString();

            groupMember=new ArrayList();

            groupMember.add(Integer.toString(Priority));
            groupMember.add(MacAddress);
            groupMembersList.add(groupMember);

        }


        umtDataSend.Put("GroupMembers",groupMembersList);
        String umtDataString = umtDataSend.getDataString();
        informAll(umtDataString);

    }

    //Send group members information to all clients

    // Informs all the connected clients about the latest update
    public void informAll( String Message) {



        UMTData umtDataReceive;
        umtDataReceive = new UMTData(Message);

        if(!umtDataReceive.get("type").equals("GroupInfo"))
        {
            dataContainer.put(Integer.valueOf(umtDataReceive.get("timeStamp").toString()),Message);
        }


        try {
            Enumeration clients = registeredClients.elements();


            while(clients.hasMoreElements()){

                Socket recipient = (Socket)clients.nextElement();
                 new PrintWriter(recipient.getOutputStream(),true).println(Message);

            }

        }
        catch (Exception e){

        }

    }

    //Send Update to Server

    public void updateServer(String updMsg){
        if (prClient != null) {
            prClient.println(updMsg);
        }
    }



    public void tearDown() {

        if (serverSocketCreated){
            mUMTServer.tearDown();
            serverSocketCreated=false;
        }

       if(clientSocketCreated){
           mUMTClient.tearDown();
           clientSocketCreated=false;
       }

       registeredClients.clear();

    }


    //Start UMTServer
    private class UMTServer {
        ServerSocket mServerSocket = null;
        Thread mThread = null;

        public UMTServer()  {

            mThread=new Thread(new ServerThread());
            mThread.start();
        }

        public void tearDown() {

                serverSocketCreated=false;
                mThread.interrupt();
                try {
                    mServerSocket.close();
                } catch (IOException ioe) {
                    Log.e(TAG, "Error when closing server socket.");
                }

        }


        // Start ServerThread Class
        class ServerThread implements Runnable{
            Thread mThread = null;

            @Override
            public void run() {

                try {
                    mServerSocket = new ServerSocket(SERVER_PORT);
                    String str;
                    str="";

                    while (true){
                        String DeviceMacAddress;

                        mSocket = mServerSocket.accept();
                        DeviceMacAddress=new BufferedReader(new InputStreamReader(mSocket.getInputStream())).readLine();
                        registeredClients.put(DeviceMacAddress,mSocket);


                        groupMembers.put(Priority,DeviceMacAddress);
                        Priority=Priority+1;
                        sendGroupInfoToClients();



                        // Synchronize the new client data state
                        try {

                            Set set = dataContainer.entrySet();
                            Iterator iterator = set.iterator();

                            while(iterator.hasNext()){

                                Map.Entry mentry = (Map.Entry)iterator.next();
                                new PrintWriter(mSocket.getOutputStream(), true).println(mentry.getValue());

                            }
                        }
                        catch (Exception e){

                        }



                        mThread=new Thread(new ServerClientThread(mSocket,DeviceMacAddress));

                       // registeredClientsThreads.put(DeviceMacAddress,mThread);

                        mThread.start();

                    }



                } catch (Exception e) {
                    String str;
                    str=e.getMessage().toString();


                }

            }
        }
        // End ServerThread Class


        class ServerClientThread implements Runnable{

            private Socket client = null;
            private BufferedReader reader = null;
            private String DeviceMacAddress;


            public ServerClientThread(Socket socket,String DeviceMacAddress){

                try {
                    this.DeviceMacAddress=DeviceMacAddress;
                    client=socket;
                    reader = new BufferedReader(new InputStreamReader(client.getInputStream()));
                }
                catch (Exception e) {

                }

            }

            @Override
            public void run() {


                String msg = "";
                try {


                        while (!(msg = reader.readLine()).equals("QUIT") ){


                            if (msg.equals("TimeStampRequest")){

                                new PrintWriter(client.getOutputStream(), true).println("TimeStamp:" + GenerateTimeStamp());

                            }
                            else {

                                Bundle messageBundle = new Bundle();
                                messageBundle.putString("msg", msg);
                                Message message = new Message();
                                message.setData(messageBundle);
                                mainHandler.sendMessage(message);
                                informAll(msg);

                            }



                        }



                }
                catch (Exception e){

                }


                endSession(DeviceMacAddress);


            }

            private void endSession(String DeviceMacAddress){
                try{

                    registeredClients.remove(DeviceMacAddress);
                    reader.close();

                }catch(Exception e){

                }
            }

        }






    }//End UMTServer



    public void connectToServer(InetAddress IPAddress,Integer portNumber,String DeviceMacAddress) {

        mAddress=IPAddress;
        SERVER_PORT=portNumber;
        this.DeviceMacAddress=DeviceMacAddress;
        mUMTClient = new UMTClient();
        isClient=true;
        clientSocketCreated=true;

    }


    //Start UMTClient
    private  class UMTClient{
        Socket socket = null;
        private BufferedReader reader = null;
       //The current users id
        private String UID = "";

        public UMTClient(){

            Thread thread=new Thread(new ClientThread());
            thread.start();

        }


        //Start Client Thread
        class ClientThread implements Runnable {

            @Override
            public void run() {
                try{
                    socket = new Socket(mAddress,SERVER_PORT);
                    reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    prClient = new PrintWriter(socket.getOutputStream(),true);

                    prClient.println(DeviceMacAddress);

                    String msg = "";

                    while (!(msg = reader.readLine()).equals("QUIT")) {

                        if (msg.contains("TimeStamp:")){

                            String str = "";
                            java.util.StringTokenizer tokenizer = new java.util.StringTokenizer(msg,":");
                            str=tokenizer.nextToken();
                            timeStamp=Integer.parseInt(tokenizer.nextToken());

                        }
                        else if (msg.contains("GroupInfo"))  {

                            UMTData umtDataReceive=new UMTData(msg);

                            List<List<String >> groupMembersReceived= new ArrayList<List<String>>();
                            groupMembersReceived = umtDataReceive.getArray("GroupMembers");

                            groupMembers.clear();

                            for (int i = 0; i < groupMembersReceived.size(); i++) {
                                List<String> member = groupMembersReceived.get(i);
                                groupMembers.put(Integer.parseInt(member.get(0)), member.get(1));

                            }



                        }
                        else {

                          //Data Container to maintain the history of data packets Received
                           UMTData umtDataReceive;
                           umtDataReceive = new UMTData(msg);
                          dataContainer.put(Integer.valueOf(umtDataReceive.get("timeStamp").toString()),msg);
                          // Data Container to maintain the history of data packets



                            Bundle messageBundle = new Bundle();
                            messageBundle.putString("msg", msg);
                            Message message = new Message();
                            message.setData(messageBundle);
                            mainHandler.sendMessage(message);

                        }


                    }


                }

                catch (Exception e){
                    String str;
                    str=e.getMessage().toString();
                }

            }
        }//End ClientThread


        public void tearDown() {


            try {
                clientSocketCreated=false;
                socket.close();
            } catch (IOException ioe) {

            }
        }


    }     //End UMTClient



} // End DocConnection
