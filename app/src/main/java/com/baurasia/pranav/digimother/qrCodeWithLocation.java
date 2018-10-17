package com.baurasia.pranav.digimother;

import com.google.android.gms.vision.barcode.Barcode;

/**
 * Created by Pranav Baurasia on 29-06-2018.
 */

class qrCodeWithLocation {
    Barcode barcode = null;
    vector2 coordinates = new vector2();

    public qrCodeWithLocation (Barcode barcode, vector2 coordinates) {
        this.barcode = barcode;
        this.coordinates.x = coordinates.x;
        this.coordinates.y = coordinates.y;
    }
}
