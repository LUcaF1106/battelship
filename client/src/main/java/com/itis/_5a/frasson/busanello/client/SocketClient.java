package com.itis._5a.frasson.busanello.client;

import com.itis._5a.frasson.busanello.common.*;
import lombok.Getter;

import java.io.*;
import java.net.Socket;

public class SocketClient {
    private static SocketClient instance;
    private AES aesKey;

    private Socket socket;
    private DataOutputStream out;
    private DataInputStream in;


    @Getter
    private boolean isconnected = false;

    public static synchronized SocketClient getInstance() {
        if (instance == null) {
            instance = new SocketClient();
        }
        return instance;
    }

    public boolean connect(String host, int port) {
        try {
            socket = new Socket(host, port);
            out = new DataOutputStream(socket.getOutputStream());
            in = new DataInputStream(socket.getInputStream());
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
        if (isconnected && out != null) {
            byte[] encryptMsg = aesKey.encrypt(message);
            ObjectOutputStream objectOut = new ObjectOutputStream(out);
            objectOut.writeObject(encryptMsg);
            objectOut.flush();
            System.out.println("Send message");
        }
    }

    public <T> T receiveMessage(Class<T> tClass) {
        try {
            System.out.println("ciao ciao");
            ObjectInputStream objectIn = new ObjectInputStream(in);
            byte[] encmsg = (byte[]) objectIn.readObject();

            System.out.println(encmsg);
            return Json.deserializedSpecificMessage(aesKey.decrypt(encmsg), tClass);
        } catch (IOException e) {
            System.err.println("Error receiving message: " + e.getMessage());
        } catch (ClassNotFoundException e) {
            System.err.println(e);
            throw new RuntimeException(e);
        } catch (Exception e) {
            System.err.println(e);
            throw new RuntimeException(e);
        }
        return null;
    }

    public void disconnect() {
        isconnected = false;
        try {
            if (out != null) out.close();
            if (in != null) in.close();
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
            // genera chiavi
            KeyExchange keyExchange = new KeyExchange();
            keyExchange.generateDHKeys();

            // scambio chiavi con server
            int serverKeyLength = in.readInt();
            System.out.println(serverKeyLength);
            byte[] serverPublicKey = new byte[serverKeyLength];
            in.readFully(serverPublicKey);
            keyExchange.setOtherPublicKey(serverPublicKey);

            out.writeInt(keyExchange.getPublicKeyBytes().length);
            out.write(keyExchange.getPublicKeyBytes());

            aesKey.setupAESKeys(keyExchange.generateSecret());
            System.out.println("Setup aes key");
        } catch (Exception e) {
            System.err.println("Errore creazione comunicazione sicura");
            disconnect();
        }
    }
}