package com.baurasia.pranav.digimother;

/**
 * Created by Pranav Baurasia on 29-06-2018.
 */

class child {
    private String childID;
    private String[] workerID;
    private boolean[] vaccines;

    public child(String cID) {
        this.childID = cID;
        this.workerID = new String[20];
        this.vaccines = new boolean[20];
        for(int i=0; i<20; i++){
            vaccines[i]=false;
            workerID[i] = "";
        }
    }

}
