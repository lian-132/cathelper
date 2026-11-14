package com.cathelperawa.owo.app.api;

import com.cathelperawa.owo.app.proto.Pb;

public final class PbApi {

    private PbApi() {}

    public static byte[] elemJsonToPb(String json) {
        return Pb.elemBytes(json);
    }

    public static byte[] ld(int field, byte[] data) {
        return Pb.ld(field, data);
    }

    public static byte[] vf(int field, long v) {
        return Pb.vf(field, v);
    }

    public static byte[] strField(int field, String s) {
        return Pb.strField(field, s);
    }

    public static byte[] cat(byte[]... arrs) {
        return Pb.cat(arrs);
    }
}
