package com.itis._5a.frasson.busanello.server;

import java.io.*;
import java.net.Socket;


import com.itis._5a.frasson.busanello.common.Json;
import com.itis._5a.frasson.busanello.common.KeyExchange;
import com.itis._5a.frasson.busanello.common.AES;
import com.itis._5a.frasson.busanello.common.Message.LoginM;
import com.itis._5a.frasson.busanello.common.Message.Message;
import lombok.Getter;
import lombok.Synchronized;

public class ClientHandler implements Runnable {
    private static long counter = 0;

    private final Socket clientSocket;
    private final Auth auth;

    @Getter
    private String id;
    private Server server;
    @Getter
    private boolean isAuthenticated;
    private String username = null;
    private AES aes;
    private int state;
    private Object lock = new Object();
    private InputStream in;
    private OutputStream out;
    private ObjectInputStream objectIn;
    private ObjectOutputStream objectOut;


    public ClientHandler(Socket socket, Auth auth) {
        this.clientSocket = socket;
        this.auth = auth;
        this.id=generateId();
        this.server=Server.getInstance();

        try {
            in = clientSocket.getInputStream();
            out = clientSocket.getOutputStream();

            objectIn = new ObjectInputStream(in);
            objectOut = new ObjectOutputStream(out);
        }catch (IOException e){


        }
    }

    @Synchronized("lock")
    public int getState() {
        return state;
    }

    @Synchronized("lock")
    public void setState(int state) {
        this.state = state;
    }
    public void sendMessage(Object msg) throws Exception {
        objectOut.writeObject(aes.encrypt(Json.serializedMessage(msg)));
        objectOut.flush();
    }


    @Override
    public void run() {
        try  {
            setupSecureCom(new DataOutputStream(out), new DataInputStream(in));

            while (!clientSocket.isClosed()) {
                try {
                    byte[] messageEncrypted = (byte[]) objectIn.readObject();

                    if (messageEncrypted != null && messageEncrypted.length > 0) {
                        byte[] messageDecrypted = aes.decrypt(messageEncrypted);
                        Message m = Json.deserialiazedMessage(messageDecrypted);

                        switch (m.getType()) {
                            case "LOGIN":
                                LoginM mes = Json.deserializedSpecificMessage(messageDecrypted, LoginM.class);
                                Login(mes.getUser(), mes.getPassword());
                                break;
                            case "LOGOUT":
                                Logout();
                                break;
                            case "FMATCH":
                                System.out.println("Find a match");
                                server.enqueue(this);
                                break;
                            default:
                                break;
                        }
                    }
                } catch (EOFException e) {
                    disconnect();
                    break;
                }
            }
        } catch (IOException e) {
            System.err.println("Errore con il client: " + e.getMessage());
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            System.out.println("Client disconnesso: " + (username != null ? username : "non autenticato"));
            disconnect();
        }
    }

    private void Login(String username, String password) throws Exception {

        if (auth.authenticate(username, password)) {
            this.username = username;
            this.isAuthenticated = true;
            Message message = new Message("ACC");

            objectOut.writeObject(aes.encrypt(Json.serializedMessage(message)));

        } else {
            Message message = new Message("AUTHERR");
            objectOut.writeObject(aes.encrypt(Json.serializedMessage(message)));

        }
        objectOut.flush();
    }

    private void Logout() {
        System.out.println("Utente disconnesso: " + username);
        try {
            Message message = new Message("LOGOUT");
            objectOut.writeObject(aes.encrypt(Json.serializedMessage(message)));
            objectOut.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        isAuthenticated = false;
        username = null;
    }

    private void setupSecureCom(DataOutputStream out, DataInputStream in) {
        KeyExchange keyExchange = new KeyExchange();
        aes = new AES();

        try {
            keyExchange.generateDHKeys();

            out.writeInt(keyExchange.getPublicKeyBytes().length);
            out.write(keyExchange.getPublicKeyBytes());
            out.flush();

            int clientKeyLength = in.readInt();

            byte[] clientPublicKey = new byte[clientKeyLength];
            in.readFully(clientPublicKey);
            keyExchange.setOtherPublicKey(clientPublicKey);

            aes.setupAESKeys(keyExchange.generateSecret());
            System.out.println("Setup aes key");

        } catch (RuntimeException | IOException e) {
            System.err.println("Comunicazione sicura non impostata: " + e.getMessage());
            e.printStackTrace();
            try {
                clientSocket.close();
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }
    }
    public void disconnect() {
       // isconnected = false;
        try {
            if (out != null) out.close();
            if (in != null) in.close();
            if(objectIn !=null) objectIn.close();
            if(objectOut!=null) objectOut.close();
            if (clientSocket != null) clientSocket.close();
        } catch (IOException e) {
            System.err.println("Error during disconnect: " + e.getMessage());
        }
    }
    public static synchronized String generateId() {
        return System.currentTimeMillis() + "-" + (counter++);
    }



}