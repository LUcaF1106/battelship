package com.itis._5a.frasson.busanello.client;

import com.itis._5a.frasson.busanello.common.*;
import com.itis._5a.frasson.busanello.common.Message.Message;
import lombok.Getter;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.net.Socket;

public class SocketClient implements Runnable{
    private static final Logger logger = LogManager.getLogger(SocketClient.class);

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
        logger.info("Attempting to connect to " + host + ":" + port);

        try {
            socket = new Socket(host, port);
            out = socket.getOutputStream();

            objectOut = new ObjectOutputStream(out);
            objectOut.flush();

            in = socket.getInputStream();
            objectIn = new ObjectInputStream(in);

            isconnected = true;
            aesKey = new AES();
            logger.info("Successfully connected to server");
            setupKey();
            return true;
        } catch (IOException e) {
            logger.error("Connection error: " + e.getMessage(), e);
            return false;
        }
    }

    public void sendMessage(byte[] message) throws Exception {
        logger.debug("Sending message: " + message.getClass().getSimpleName());
        if (isconnected && objectOut != null) {
            byte[] encryptMsg = aesKey.encrypt(message);

            objectOut.writeObject(encryptMsg);
            objectOut.flush();
            logger.debug("Message sent successfully");
        }
    }

    public <T> T receiveMessage(Class<T> tClass)throws Exception {
        logger.debug("Waiting to receive message of type: " + tClass.getSimpleName());

        if(!socket.isClosed()) {


            byte[] encmsg = (byte[]) objectIn.readObject();
            logger.debug("Message received: " + tClass.getSimpleName());

            return Json.deserializedSpecificMessage(aesKey.decrypt(encmsg), tClass);

        }
        return null;
    }

    public void disconnect() {
        logger.info("Disconnecting from server");

        try {
            sendMessage(Json.serializedMessage(new Message("EXIT")));
        } catch (Exception e) {
            logger.error("Error during disconnect: " + e.getMessage(), e);
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
            logger.error("Error during disconnect: " + e.getMessage(), e);        }
    }

    public <T> T sendAndReceive(byte[] message, Class<T> tClass) throws Exception {
        sendMessage(message);
        return receiveMessage(tClass);
    }

    public void setupKey() {
        logger.info("Setting up secure communication");

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
            logger.info("Secure communication established");

        } catch (Exception e) {
            logger.error("Error setting up secure communication: " + e.getMessage(), e);
            e.printStackTrace();
            disconnect();
        }
    }

 }
