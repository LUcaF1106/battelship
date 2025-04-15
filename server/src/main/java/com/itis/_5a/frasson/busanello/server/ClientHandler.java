package com.itis._5a.frasson.busanello.server;


import java.io.*;
import java.net.Socket;

import com.itis._5a.frasson.busanello.common.Json;
import com.itis._5a.frasson.busanello.common.KeyExchange;
import com.itis._5a.frasson.busanello.common.AES;
import com.itis._5a.frasson.busanello.common.Message.LoginM;
import com.itis._5a.frasson.busanello.common.Message.Message;
import lombok.Synchronized;

public class ClientHandler implements Runnable{
    private final Socket clientSocket;
    private final Auth auth;

    private boolean isAuthenticated;
    private String username =null;
    private AES aes;
    private int state;
    private Object lock= new Object();

    public ClientHandler(Socket socket, Auth auth) {
        this.clientSocket = socket;
        this.auth= auth;
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
                DataInputStream in = new DataInputStream(clientSocket.getInputStream());
                DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream())
        ) {
            setupSecureCom(out, in);

            try {
                while (true) {
                    int length = in.readInt(); // leggiamo la lunghezza del messaggio
                    if (length > 0) {
                        byte[] messageEncrypted = new byte[length];
                        in.readFully(messageEncrypted); // leggiamo i byte cifrati

                        byte[] messageDecrypted = aes.decrypt(messageEncrypted);

                        Message m= Json.deserialiazedMessage(messageDecrypted);


                        switch (m.getType()){
                            case "LOGIN":
                                LoginM mes=Json.deserializedSpecificMessage(messageDecrypted, LoginM.class);
                                Login(mes.getUser(), mes.getPassword(), out);
                                break;
                            case "LOGOUT":
                                break;
                            default:
                                break;
                        }
//                        if (msg.startsWith("login:")) {
//                            Login(msg, out);
//                        } else if (msg.equals("logout")) {
//                            Logout(out);
//                            break; // usciamo dal ciclo se è logout
//                        }
                    }
                }
            } catch (IOException e) {
                System.err.println("Errore nella lettura dello stream: " + e.getMessage());
                e.printStackTrace();
            }

        } catch (IOException e) {
            System.err.println("Errore con il client: " + e.getMessage());
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

    private void Login(String username,String password, DataOutputStream out) throws Exception {


        if (auth.authenticate(username, password)) {
            this.username = username;
            this.isAuthenticated = true;
            String msg="Accesso";
            byte[] encmsg=aes.encrypt(msg.getBytes());

            System.out.println(encmsg.length);
            out.writeInt(encmsg.length);

            out.write(encmsg);

            System.out.println("Utente autenticato: " + username);
        } else{
            String msg="Autenticazione fallita: username o password non validi";

            out.writeUTF(aes.encrypt(msg.getBytes()).toString());
        }
    }

    private void Logout(DataOutputStream out) {

        System.out.println("Utente disconnesso: " + username);
        try {
            String msg="Logout effettuato!";

            out.writeUTF(aes.encrypt(msg.getBytes()).toString());

        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        isAuthenticated = false;
        username = null;

    }

    private void setupSecureCom(DataOutputStream out, DataInputStream in)  {
        KeyExchange keyExchange = new KeyExchange();
        aes=new AES();

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

}
