package com.itis._5a.frasson.busanello.common.Message;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OpponentTurn extends Message{
    private int[][] shipPlace;
    private int[][] moves;

    public OpponentTurn(int [][] shipPlace, int[][] moves){
        super("OP");
        this.shipPlace=shipPlace;
        this.moves=moves;
    }
}
