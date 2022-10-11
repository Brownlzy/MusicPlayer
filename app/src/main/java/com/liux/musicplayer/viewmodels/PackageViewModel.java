package com.liux.musicplayer.viewmodels;


import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class PackageViewModel extends ViewModel {
    private MutableLiveData<String> status = new MutableLiveData<>();

    private static volatile PackageViewModel sInstance;
    private PackageViewModel(){

    }

    public static PackageViewModel getsInstance() {
        if(sInstance == null){
            synchronized (PackageViewModel.class){
                sInstance = new PackageViewModel();
            }
        }
        return sInstance;
    }

    public MutableLiveData<String> getStatus() {
        return status;
    }
}
