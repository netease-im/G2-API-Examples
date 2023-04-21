package com.netease.nertc.externalaudioshare.externalvideo;


public abstract class ExternalAudioSource {

    public abstract boolean start();

    public abstract void stop();

    public abstract void mute(boolean mute, boolean keepPushMuteData);
}
