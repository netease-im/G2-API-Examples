package com.netease.nertc.externalvideocapture.externalvideo;

import com.netease.lava.nertc.sdk.video.NERtcVideoFrame;

public abstract class ExternalVideoSource {

    public abstract boolean start();

    public abstract void stop();
}
