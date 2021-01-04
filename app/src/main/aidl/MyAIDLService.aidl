package aidl;
interface MyAIDLService {
    boolean fix(String path, FixStatusCallback fixStatusCallback);
}