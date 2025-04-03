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
            System.out.println(clientPublicKey);
            in.readFully(clientPublicKey);
            try {
                keyExchange.setOtherPublicKey(clientPublicKey);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            AES aes=new AES();
            try {
                aes.setupAESKeys(keyExchange.generateSecret());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }


            String message;
            while (!((message = in.readUTF()) == null)) {
                System.out.println("Messaggio ricevuto: " + message);

                if (message.startsWith("login:")) {
                    Login(message, out);
                } else if (message.equals("logout")) {
                    Logout(out);
                }
            }
        } catch (IOException e) {
            System.err.println("Errore con il client: " + e.getMessage());
        } finally {
            try {
                System.out.println("Client disconnesso: " + (username != null ? username : "non autenticato"));
                clientSocket.close();
            } catch (IOException e) {
                System.err.println("Errore nella chiusura della connessione: " + e.getMessage());
            }
        }
    }

    private void Login(String loginMessage, DataOutputStream out) throws IOException {
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
            out.writeUTF("Accesso");
            System.out.println("Utente autenticato: " + username);
        } else {
            out.writeUTF("Autenticazione fallita: username o password non validi");
        }
    }

    private void Logout(DataOutputStream out) {

        System.out.println("Utente disconnesso: " + username);
        try {
            out.writeUTF("Logout effettuato!");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        isAuthenticated = false;
        username = null;

    }


}
