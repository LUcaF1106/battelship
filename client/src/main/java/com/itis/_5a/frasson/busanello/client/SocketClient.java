package com.itis._5a.frasson.busanello.client;

import com.itis._5a.frasson.busanello.common.KeyExchange;
import java.io.*;
import java.net.Socket;

public class SocketClient{
    private static SocketClient instance;

    private Socket socket;
    private DataOutputStream out;
    private DataInputStream in;
    private boolean connected = false;

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
            connected = true;

            setupKey();
            return true;
        } catch (IOException e) {
            System.err.println("Connection error: " + e.getMessage());
            return false;
        }
    }

    public void sendMessage(String message) throws IOException {

        if (connected && out != null) {
            out.writeUTF(message);


        }
    }

    public String receiveMessage() {
        if (connected && in != null) {
            try {
                return in.readUTF();
            } catch (IOException e) {
                System.err.println("Error receiving message: " + e.getMessage());
            }
        }
        return "";
    }

    public boolean isConnected() {
        return connected;
    }

    public void disconnect() {
        connected = false;
        try {
            if (out != null) out.close();
            if (in != null) in.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            System.err.println("Error during disconnect: " + e.getMessage());
        }
    }

    public String sendAndReceive(String message) throws IOException {
        sendMessage(message);
        return receiveMessage();
    }

    public void setupKey() {
        try{
            //genera chiavi
            KeyExchange keyExchange= new KeyExchange();
            keyExchange.generateDHKeys();

            //scambio chiavi con server
            int serverKeyLength = in.readInt();
            System.out.println(serverKeyLength);
            byte[] serverPublicKey = new byte[serverKeyLength];
            System.out.println(serverPublicKey);
            in.readFully(serverPublicKey);
            keyExchange.setOtherPublicKey(serverPublicKey);

            out.writeInt(keyExchange.getPublicKeyBytes().length);
            out.write(keyExchange.getPublicKeyBytes());

            keyExchange.generateSecret();

            //TODO: implementare sul server e fare in modo che si calcoli il "segreto comune"

        }catch (Exception e){
            System.err.println("Errore creazione comunicazione sicura");
            disconnect();

        }


    }
}

