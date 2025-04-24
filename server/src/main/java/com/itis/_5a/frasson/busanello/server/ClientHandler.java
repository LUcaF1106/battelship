package com.itis._5a.frasson.busanello.server;

import java.io.*;
import java.net.Socket;


import com.itis._5a.frasson.busanello.common.Json;
import com.itis._5a.frasson.busanello.common.KeyExchange;
import com.itis._5a.frasson.busanello.common.AES;
import com.itis._5a.frasson.busanello.common.Message.*;
import com.itis._5a.frasson.busanello.common.Message.Message;
import com.itis._5a.frasson.busanello.common.Message.ShipPlacement;
import lombok.Getter;
import lombok.Setter;
import lombok.Synchronized;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;

public class ClientHandler implements Runnable {
    private static final Logger LOGGER = LogManager.getLogger(ClientHandler.class);
    private static long counter = 0;

    @Getter
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

    @Getter
    @Setter
    private Match currentMatch;

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
            LOGGER.info("I/O streams initialized for client: " + id);
        }catch (IOException e){
            LOGGER.error("Failed to initialize streams for client: " + id, e);

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
        try{
            objectOut.writeObject(aes.encrypt(Json.serializedMessage(msg)));
            objectOut.flush();
            LOGGER.info("Message sent to client: " + id + ", type: " + msg.getClass().getSimpleName());
        } catch (IOException e){
            LOGGER.error("Failed to send message to client: " + id, e);
            disconnect();
        }

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


                        if (getState() == 3 && currentMatch != null && m.getType() != "LOGOUT") {
                            currentMatch.processMessage(this, m, messageDecrypted);
                        } else {
                        switch (m.getType()) {
                            case "LOGIN":
                                LoginM mes = Json.deserializedSpecificMessage(messageDecrypted, LoginM.class);
                                Login(mes.getUser(), mes.getPassword());
                                break;
                            case "SIGNUP":
                                SignUpM msg = Json.deserializedSpecificMessage(messageDecrypted, SignUpM.class);
                                SignUp(msg.getUser(), msg.getPassword());
                                break;
                            case "LOGOUT":
                                Logout();
                                break;
                            case "FMATCH":
                                LOGGER.info("Client " + id + " requested match finding");
                                server.enqueue(this);
                                break;
                            case "SP":

                                if(currentMatch!=null){
                                    ShipPlacement sp=Json.deserializedSpecificMessage(messageDecrypted, ShipPlacement.class);
                                    currentMatch.setMapShip(sp.getShip(), this);
                                } else LOGGER.warn("Received ship placement from client without active match: " + id);
                                break;
                            case "EXIT": if(getState()==0){
                                LOGGER.info("Client disconencted");
                                disconnect();
                            }else if(getState()==1){
                                LOGGER.info("Client " + id + " canceled matchmaking");
                                server.removeFromQueue(this);
                                disconnect();
                            }else{
                                currentMatch.handleDisconnect(this);
                                disconnect();
                            }
                                break;
                            default:
                                LOGGER.warn("Unknown message type from client " + id + ": " + m.getType());
                                break;
                        }}
                    }
                } catch (EOFException e) {
                    LOGGER.error("Error handling message." ,e);
                    disconnect();
                    break;
                }
            }
        } catch (IOException e) {
            LOGGER.error("Errore con il client: " + e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            LOGGER.info("Client disconnesso: " + (username != null ? username : "non autenticato"));
            disconnect();
        }
    }

    private void Login(String username, String password) throws Exception {
        LOGGER.info("Login attempt: " + username);

        if (auth.authenticate(username, password)) {
            this.username = username;
            this.isAuthenticated = true;
            Message message = new Message("ACC");

            objectOut.writeObject(aes.encrypt(Json.serializedMessage(message)));
            LOGGER.info("Authentication successful: " + username);

        } else {
            Message message = new Message("AUTHERR");
            objectOut.writeObject(aes.encrypt(Json.serializedMessage(message)));
            LOGGER.info("Authentication failed: " + username);
        }
        objectOut.flush();
    }

    private void SignUp(String username, String password) throws Exception {
        LOGGER.info("Sign in attempt: " + username);

        if (auth.signIn(username, password)) {
            this.username = username;
            this.isAuthenticated = true;
            Message message = new Message("ACC");

            objectOut.writeObject(aes.encrypt(Json.serializedMessage(message)));
            LOGGER.info("User added: " + username);

        } else {
            Message message = new Message("AUTHERR");
            objectOut.writeObject(aes.encrypt(Json.serializedMessage(message)));
            LOGGER.info("Error while adding user: " + username);
        }
        objectOut.flush();
    }


    private void Logout() {
        LOGGER.info("User logging out: " + username);
        try {
            Message message = new Message("LOGOUT");
            objectOut.writeObject(aes.encrypt(Json.serializedMessage(message)));
            objectOut.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            LOGGER.warn("Error during logout for " + username, e);
            throw new RuntimeException(e);
        } finally{
            isAuthenticated = false;
            username = null;
        }
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
            LOGGER.info("Secure communication established with client: " + id);

        } catch (RuntimeException | IOException e) {
            LOGGER.error("Failed to setup secure communication with client: " + id, e);
            e.printStackTrace();
            try {
                clientSocket.close();
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }
    }
    public void disconnect() {

        try {
            server.clientDisconnected(this);

            if (objectOut != null) objectOut.close();
            if (objectIn != null) objectIn.close();
            if (out != null) out.close();
            if (in != null) in.close();
            if (clientSocket != null && !clientSocket.isClosed()) clientSocket.close();

            LOGGER.info("Client resources cleaned up: " + id);
        } catch (IOException e) {
            LOGGER.warn("Error during disconnect for client " + id + ": " + e.getMessage());
        }
    }

    public static synchronized String generateId() {
        return System.currentTimeMillis() + "-" + (counter++);
    }

    public boolean isConnected() {
        return !clientSocket.isClosed();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        ClientHandler that = (ClientHandler) obj;
        return id.equals(that.id);
    }



}