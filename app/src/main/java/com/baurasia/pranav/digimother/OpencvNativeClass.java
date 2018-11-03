package com.baurasia.pranav.digimother;

public class OpencvNativeClass {
    public native static int convertGray(long matAddrRgba, long matAddrGray);
    public native static int findSquares(long matAddrRgba, long matAddrGray);
    public native static int cropTable(long matAddrImage, long matAddrTable);
}