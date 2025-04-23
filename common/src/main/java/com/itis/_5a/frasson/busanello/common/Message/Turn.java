package com.itis._5a.frasson.busanello.common.Message;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Turn extends Message{
    private int[][] shipPlace;
    private int[][] moves;

    public Turn(String t, int [][] shipPlace, int[][] moves){
        super(t);
        this.shipPlace=shipPlace;
        this.moves=moves;
    }
}
