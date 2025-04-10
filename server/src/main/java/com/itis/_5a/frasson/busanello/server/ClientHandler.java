package com.itis._5a.frasson.busanello.server;


import java.io.*;
import java.net.Socket;
import com.itis._5a.frasson.busanello.common.KeyExchange;
import com.itis._5a.frasson.busanello.common.AES;


public class ClientHandler extends Thread {
    private Socket clientSocket;
    private Auth auth;
    private boolean isAuthenticated;
    private String username = null;
    private AES aes;

    public ClientHandler(Socket socket, Auth auth) {
        this.clientSocket = socket;
        this.auth= auth;
    }

    @Override
    public void run() {
        try (
                DataInputStream in = new DataInputStream(clientSocket.getInputStream());
                DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream())
        ) {
            KeyExchange keyExchange = new KeyExchange();
            try {
                keyExchange.generateDHKeys();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            out.writeInt(keyExchange.getPublicKeyBytes().length);
            out.write(keyExchange.getPublicKeyBytes());

            int clientKeyLength = in.readInt();
            System.out.println(clientKeyLength);
            byte[] clientPublicKey = new byte[clientKeyLength];
            in.readFully(clientPublicKey);
            try {
                keyExchange.setOtherPublicKey(clientPublicKey);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            aes=new AES();
            try {
                aes.setupAESKeys(keyExchange.generateSecret());
                System.out.println("Setup aes key");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }


            try {
                while (true) {
                    int length = in.readInt(); // leggiamo la lunghezza del messaggio
                    if (length > 0) {
                        byte[] messageEncrypted = new byte[length];
                        in.readFully(messageEncrypted); // leggiamo i byte cifrati

                        byte[] messageDecrypted = aes.decrypt(messageEncrypted);
                        String msg = new String(messageDecrypted);
                        System.out.println("Messaggio ricevuto: " + msg);

                        if (msg.startsWith("login:")) {
                            Login(msg, out);
                        } else if (msg.equals("logout")) {
                            Logout(out);
                            break; // usciamo dal ciclo se è logout
                        }
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

    private void Login(String loginMessage, DataOutputStream out) throws Exception {
        String[] parts = loginMessage.split(":");
        if (parts.length != 3) {
            out.writeUTF("Formato non valido. Usa 'login:username:password'");
            return;
        }

        String username = parts[1];
        String password = parts[2];

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


}
