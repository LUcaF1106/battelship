package com.itis._5a.frasson.busanello.common.Message;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ShipPlacement extends Message{

    private int[][] ship;

    public ShipPlacement(){
        super("SP");
    }
    public ShipPlacement(int[][] ship){
        super("SP");
        this.ship=ship;
    }
}
