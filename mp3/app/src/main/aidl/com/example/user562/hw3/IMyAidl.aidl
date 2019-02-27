// IMyAidl.aidl
package com.example.user562.hw3;
import com.example.user562.hw3.IMyAidlBack;

// Declare any non-default types here with import statements

interface IMyAidl {
    void sendMessage(String mes);
    boolean getIsPlaying();
    oneway void registerAidlBack(IMyAidlBack aidl);
}
