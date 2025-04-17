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

    public ClientHandler(Socket socket, Auth auth) {
        this.clientSocket = socket;
        this.auth = auth;
        this.id=getId();
        this.server=Server.getInstance();
    }

    @Synchronized("lock")
    public int getState() {
        return state;
    }

    @Synchronized("lock")
    public void setState(int state) {
        this.state = state;
    }



    @Override
    public void run() {
        try (
                InputStream in = clientSocket.getInputStream();
                OutputStream out = clientSocket.getOutputStream();
        ) {
            setupSecureCom(new DataOutputStream(out), new DataInputStream(in));

            // Create ONE ObjectInputStream at the start
            ObjectInputStream objectIn = new ObjectInputStream(in);

            while (!clientSocket.isClosed()) {
                try {
                    byte[] messageEncrypted = (byte[]) objectIn.readObject();

                    if (messageEncrypted != null && messageEncrypted.length > 0) {
                        byte[] messageDecrypted = aes.decrypt(messageEncrypted);
                        Message m = Json.deserialiazedMessage(messageDecrypted);

                        switch (m.getType()) {
                            case "LOGIN":
                                LoginM mes = Json.deserializedSpecificMessage(messageDecrypted, LoginM.class);
                                Login(mes.getUser(), mes.getPassword(), out);
                                break;
                            case "LOGOUT":
                                Logout(new DataOutputStream(out));
                                break;
                            case "FMATCH":
                                server.enqueue(this);
                                break;
                            default:
                                break;
                        }
                    }
                } catch (EOFException e) {
                    // Client disconnected
                    break;
                }
            }
        } catch (IOException e) {
            System.err.println("Errore con il client: " + e.getMessage());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            try {
                System.out.println("Client disconnesso: " + (username != null ? username : "non autenticato"));
                clientSocket.close();
            } catch (IOException e) {
                System.err.println("Errore nella chiusura della connessione: " + e.getMessage());
            }
        }
    }

    private void Login(String username, String password, OutputStream out) throws Exception {
        ObjectOutputStream objectOut = new ObjectOutputStream(out);

        if (auth.authenticate(username, password)) {
            this.username = username;
            this.isAuthenticated = true;
            Message message = new Message("ACC");
            byte[] encmsg = aes.encrypt(Json.serializedMessage(message));
            objectOut.writeObject(encmsg);
        } else {
            Message message = new Message("AUTHERR");
            objectOut.writeObject(aes.encrypt(Json.serializedMessage(message)));
        }
        objectOut.flush();
    }

    private void Logout(DataOutputStream out) {
        System.out.println("Utente disconnesso: " + username);
        try {
            Message message = new Message("LOGOUT");
            out.write(aes.encrypt(Json.serializedMessage(message)));
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

            int clientKeyLength = in.readInt();

            byte[] clientPublicKey = new byte[clientKeyLength];
            in.readFully(clientPublicKey);
            keyExchange.setOtherPublicKey(clientPublicKey);

            aes.setupAESKeys(keyExchange.generateSecret());
            System.out.println("Setup aes key");
        } catch (RuntimeException | IOException e) {
            System.err.println("Comunicazione sicura non impostata");
            try {
                clientSocket.close();
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }

    }
    public static synchronized String generateId() {
        return System.currentTimeMillis() + "-" + (counter++);
    }


}