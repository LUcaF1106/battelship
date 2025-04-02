package com.itis._5a.frasson.busanello;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

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
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)
        ) {


            String message;
            while ((message = in.readLine()) != null) {
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

    private void Login(String loginMessage, PrintWriter out) {
        String[] parts = loginMessage.split(":");
        if (parts.length != 3) {
            out.println("Formato non valido. Usa 'login:username:password'");
            return;
        }

        String username = parts[1];
        String password = parts[2];

        if (auth.authenticate(username, password)) {
            this.username = username;
            this.isAuthenticated = true;
            out.println("Accesso");
            System.out.println("Utente autenticato: " + username);
        } else {
            out.println("Autenticazione fallita: username o password non validi");
        }
    }

    private void Logout(PrintWriter out) {

        System.out.println("Utente disconnesso: " + username);
        out.println("Logout effettuato!");
        isAuthenticated = false;
        username = null;

    }
}
