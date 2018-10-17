package com.baurasia.pranav.digimother;

import java.util.Vector;

/**
 * Created by Pranav Baurasia on 05-07-2018.
 */

class varQRCode {
    String name, id;
    vector2 startingCoordinates, endingCoordinates;

    varQRCode(String name, vector2 start, vector2 end){//, String id) {
        this.name = name;
        this.startingCoordinates = start;
        this.endingCoordinates = end;
//        this.id = id;
    }
}

//class Template {
//    NameWithWindow childIDname;
//    Vector<NameWithWindow> vaccines;
//
//    Template(NameWithWindow child, Vector<NameWithWindow> vaccines) {
//        this.childIDname = child;
//        for(NameWithWindow v : vaccines){
//            this.vaccines.add(v);
//        }
//    }
//
//}
