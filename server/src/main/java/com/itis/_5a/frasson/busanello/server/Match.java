package com.itis._5a.frasson.busanello.server;

import com.itis._5a.frasson.busanello.common.Message.Message;



public class Match implements Runnable{
    private ClientHandler client1;
    private char[][] mapShip1 =new char[10][10];
    private char[][] moveMap1=new char[10][10];

    private ClientHandler client2;
    private char[][] mapShip2 =new char[10][10];
    private char[][] moveMap2=new char[10][10];


    public Match(ClientHandler c1, ClientHandler c2){
        this.client1=c1;
        this.client2=c2;

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
