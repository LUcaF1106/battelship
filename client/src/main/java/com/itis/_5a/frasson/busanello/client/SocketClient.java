package com.itis._5a.frasson.busanello.client;

import com.itis._5a.frasson.busanello.common.*;
import com.itis._5a.frasson.busanello.common.Message.Message;
import lombok.Getter;


import java.io.*;
import java.net.Socket;

public class SocketClient implements Runnable{

    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 12345;
    private static SocketClient instance;
    private AES aesKey;

    private Socket socket;
    private OutputStream out;
    private InputStream in;
    private ObjectInputStream objectIn;
    private ObjectOutputStream objectOut;

    @Getter
    private boolean isconnected = false;

    public static synchronized SocketClient getInstance() {
        if (instance == null) {
            instance = new SocketClient();
        }
        return instance;
    }

    @Override
    public void run() {
        connect(SERVER_HOST, SERVER_PORT);
    }

    public boolean connect(String host, int port) {
        try {
            socket = new Socket(host, port);
            out = socket.getOutputStream();

            objectOut = new ObjectOutputStream(out);
            objectOut.flush();

            in = socket.getInputStream();
            objectIn = new ObjectInputStream(in);

            isconnected = true;
            aesKey = new AES();
            setupKey();
            return true;
        } catch (IOException e) {
            System.err.println("Connection error: " + e.getMessage());
            return false;
        }
    }

    public void sendMessage(byte[] message) throws Exception {
        if (isconnected && objectOut != null) {
            byte[] encryptMsg = aesKey.encrypt(message);

            objectOut.writeObject(encryptMsg);
            objectOut.flush();
            System.out.println("Send message");
        }
    }

    public <T> T receiveMessage(Class<T> tClass)throws Exception {

        if(!socket.isClosed()) {


            byte[] encmsg = (byte[]) objectIn.readObject();
            return Json.deserializedSpecificMessage(aesKey.decrypt(encmsg), tClass);
        }
        return null;
    }

    public void disconnect() {
        try {
            sendMessage(Json.serializedMessage(new Message("EXIT")));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        isconnected = false;
        try {
            if (out != null) out.close();
            if (in != null) in.close();
            if(objectIn !=null) objectIn.close();
            if(objectOut!=null) objectOut.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            System.err.println("Error during disconnect: " + e.getMessage());
        }
    }

    public <T> T sendAndReceive(byte[] message, Class<T> tClass) throws Exception {
        sendMessage(message);
        return receiveMessage(tClass);
    }

    public void setupKey() {
        try {

            DataOutputStream dout = new DataOutputStream(out);
            DataInputStream din = new DataInputStream(in);


            KeyExchange keyExchange = new KeyExchange();
            keyExchange.generateDHKeys();


            int serverKeyLength = din.readInt();

            byte[] serverPublicKey = new byte[serverKeyLength];
            din.readFully(serverPublicKey);
            keyExchange.setOtherPublicKey(serverPublicKey);

            dout.writeInt(keyExchange.getPublicKeyBytes().length);
            dout.write(keyExchange.getPublicKeyBytes());
            dout.flush();
            aesKey.setupAESKeys(keyExchange.generateSecret());
            System.out.println("Setup aes key");

        } catch (Exception e) {
            System.err.println("Errore creazione comunicazione sicura: " + e.getMessage());
            e.printStackTrace();
            disconnect();
        }
    }

 }
