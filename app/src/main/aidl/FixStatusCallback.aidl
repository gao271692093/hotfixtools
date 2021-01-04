package aidl;
interface FixStatusCallback {
    void onLoad(int mode, int code, String info, int handlePatchVersion);
}
