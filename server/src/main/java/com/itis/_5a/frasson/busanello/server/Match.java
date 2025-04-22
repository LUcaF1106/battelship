package com.itis._5a.frasson.busanello.server;

import com.itis._5a.frasson.busanello.common.Message.Message;
import com.itis._5a.frasson.busanello.common.Message.OpponentTurn;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;



public class Match implements Runnable{
    private ClientHandler client1;

    private int[][] mapShip1 =new int[10][10];
    private int[][] moveMap1=new int[10][10];
    private ClientHandler client2;

    private int[][] mapShip2 =new int[10][10];
    private int[][] moveMap2=new int[10][10];

    private  boolean p1Ready = false;
    private  boolean p2Ready = false;


    public Match(ClientHandler c1, ClientHandler c2){
        this.client1=c1;
        this.client2=c2;
    }
    public void setMapShip(int[][] map, ClientHandler c)  {
        if(c==client1){
            mapShip1=map;
        }else if(c==client2){
            mapShip2=map;
        }

        playerReady(c);
    }
    public void setInitialTurn(ClientHandler c) throws Exception {
        if (c == client1) {
            c.sendMessage(new Message("YT"));
            client2.sendMessage(new Message("OT"));
        }
        else if (c == client2) {
            c.sendMessage(new Message("YT"));
            client1.sendMessage(new Message("OT"));
        }
    }
    public synchronized void playerReady(ClientHandler c) {
        if (c == client1) p1Ready = true;
        else if (c == client2) p2Ready = true;

        if (p1Ready && p2Ready) {
            try {
                startGame();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void startGame() throws Exception {
        client1.sendMessage(new Message("PLAY"));
        client2.sendMessage(new Message("PLAY"));
    }
    @Override
    public void run() {
        client1.setState(2);
        client2.setState(2);

        try {
            client1.sendMessage(new Message("MFIND"));
            client2.sendMessage(new Message("MFIND"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
