package com.itis._5a.frasson.busanello.client;

import com.itis._5a.frasson.busanello.common.*;
import lombok.Getter;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;

public class SocketClient {
    private static SocketClient instance;
    private AES aesKey;

    private Socket socket;
    private InputStream rawIn;
    private OutputStream rawOut;
    private ObjectInputStream objectIn;
    private ObjectOutputStream objectOut;

    @Getter
    private boolean isConnected = false;

    public static synchronized SocketClient getInstance() {
        if (instance == null) {
            instance = new SocketClient();
        }
        return instance;
    }

    public boolean connect(String host, int port, int timeout) throws IOException {
        disconnect(); // Clean up any existing connection

        try {
            socket = new Socket();
            socket.connect(new InetSocketAddress(host, port), timeout);

            rawIn = socket.getInputStream();
            rawOut = socket.getOutputStream();

            // Perform key exchange with timeout
            socket.setSoTimeout(timeout);
            aesKey = new AES();
            setupKey();
            socket.setSoTimeout(0); // Reset timeout after key exchange

            // Initialize object streams
            objectOut = new ObjectOutputStream(rawOut);
            objectOut.flush();
            objectIn = new ObjectInputStream(rawIn);

            isConnected = true;
            return true;
        } catch (SocketTimeoutException e) {
            System.err.println("Connection timed out");
            disconnect();
            throw e;
        } catch (IOException e) {
            System.err.println("Connection failed: " + e.getMessage());
            disconnect();
            throw e;
        }
    }

    public void sendMessage(byte[] message) throws IOException {
        if (!isConnected || objectOut == null) {
            throw new IOException("Not connected or output stream not initialized");
        }

        try {
            byte[] encryptMsg = aesKey.encrypt(message);
            objectOut.writeObject(encryptMsg);
            objectOut.flush();
        } catch (Exception e) {
            isConnected = false;
            throw new IOException("Failed to send message", e);
        }
    }

    public <T> T receiveMessage(Class<T> tClass) throws IOException {
        if (!isConnected || objectIn == null) {
            throw new IOException("Not connected or input stream not initialized");
        }

        try {
            byte[] encmsg = (byte[]) objectIn.readObject();
            byte[] decryptedMsg = aesKey.decrypt(encmsg);
            return Json.deserializedSpecificMessage(decryptedMsg, tClass);
        } catch (Exception e) {
            isConnected = false;
            throw new IOException("Failed to receive message", e);
        }
    }

    public void disconnect() {
        isConnected = false;
        try {
            if (objectOut != null) objectOut.close();
            if (objectIn != null) objectIn.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
            System.err.println("Error during disconnect: " + e.getMessage());
        } finally {
            objectOut = null;
            objectIn = null;
            socket = null;
        }
    }

    public <T> T sendAndReceive(byte[] message, Class<T> tClass) throws IOException {
        sendMessage(message);
        return receiveMessage(tClass);
    }

    private void setupKey() throws IOException {
        try {
            DataOutputStream dataOut = new DataOutputStream(rawOut);
            DataInputStream dataIn = new DataInputStream(rawIn);

            KeyExchange keyExchange = new KeyExchange();
            keyExchange.generateDHKeys();

            int serverKeyLength = dataIn.readInt();
            byte[] serverPublicKey = new byte[serverKeyLength];
            dataIn.readFully(serverPublicKey);
            keyExchange.setOtherPublicKey(serverPublicKey);

            dataOut.writeInt(keyExchange.getPublicKeyBytes().length);
            dataOut.write(keyExchange.getPublicKeyBytes());
            dataOut.flush();

            aesKey.setupAESKeys(keyExchange.generateSecret());
        } catch (Exception e) {
            throw new IOException("Key exchange failed", e);
        }
    }
}