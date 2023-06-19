// Copyright (c) 2022 NetEase, Inc. All rights reserved.
// Use of this source code is governed by a MIT license that can be
// found in the LICENSE file.

import 'package:flutter/material.dart';
import 'package:nertc_api_example/config.dart';
import 'package:nertc_core/nertc_core.dart';

class VideoPage extends StatefulWidget {
  final String cid;
  final int uid;

  VideoPage({Key? key, required this.cid, required this.uid});

  @override
  _VideoPageState createState() {
    return _VideoPageState();
  }
}

class _VideoPageState extends State<VideoPage>
    with NERtcVideoRendererEventListener, NERtcChannelEventCallback {
  final NERtcEngine _engine = NERtcEngine.instance;
  List<_UserSession> _remoteSessions = [];
  _UserSession? _localSession;
  _UserSession? _localSubStreamSession;

  bool isSpeakerEnabled = false;
  bool isAudioEnabled = false;
  bool isLeaveChannel = false;
  bool isVideoEnabled = false;
  bool isFrontCameraMirror = true;
  bool isFrontCamera = true;

  @override
  void initState() {
    super.initState();
    print('start call: uid=${widget.uid}, cid=${widget.cid}');
    _initRtcEngine();
  }

  void _releaseRtcEngine() async {
    await _engine.release();
  }

  @override
  void dispose() {
    _leaveChannel();
    _releaseRtcEngine();
    super.dispose();
  }

  void _leaveChannel() async {
    await _engine.enableLocalVideo(false);
    await _engine.enableLocalAudio(false);
    await _engine.stopVideoPreview();
    _localSession = null;
    _localSubStreamSession = null;
    _remoteSessions.clear();
    await _engine.leaveChannel();
  }

  void _initRtcEngine() async {
    // NERtcOptions options = NERtcOptions();
    _engine
        .create(appKey: Config.APP_KEY, channelEventCallback: this)
        .then((value) => _engine.setEventCallback(this))
        .then((value) => _engine.enableLocalAudio(true))
        .then((value) => _engine.enableLocalVideo(true))
        .then((value) => _initRenderer())
        .then((value) => _engine.joinChannel('', widget.cid, widget.uid));

    setState(() {
      isAudioEnabled = true;
      isVideoEnabled = true;
    });
  }

  Future<void> _initRenderer() async {
    setState(() {
      _localSession = _UserSession(widget.uid);
      updateLocalMirror();
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
        appBar: AppBar(
          title: Text('房间号:${widget.cid}'),
        ),
        body: Container(
            margin: const EdgeInsets.all(30.0),
            child: Column(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                buildVideoViews(context),
                const SizedBox(
                  height: 15,
                ),
                buildControlPanel(context)
              ],
            )));
  }

  Widget buildControlPanel(BuildContext context) {
    return Column(
        mainAxisAlignment: MainAxisAlignment.end,
        crossAxisAlignment: CrossAxisAlignment.start,
        children: <Widget>[
          const Text('视频设置', style: TextStyle(fontSize: 18, color: Colors.red)),
          const SizedBox(
            height: 10,
          ),
          buildControlPanel1(context),
          const Text('音频设置', style: TextStyle(fontSize: 18, color: Colors.red)),
          const SizedBox(
            height: 10,
          ),
          buildControlPanel2(context),
        ]);
  }

  Widget buildVideoViews(BuildContext context) {
    final sessions = [
      if (_localSession != null) _localSession!,
      if (_localSubStreamSession != null) _localSubStreamSession!,
      ..._remoteSessions,
    ];

    return Expanded(child: OrientationBuilder(
      builder: (BuildContext context, Orientation orientation) {
        return GridView.builder(
          gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
            crossAxisCount: 2,
            childAspectRatio: 3 / 4,
            crossAxisSpacing: 2.0,
            mainAxisSpacing: 2.0,
          ),
          itemCount: sessions.length,
          itemBuilder: (BuildContext context, int index) {
            return buildVideoView(context, sessions[index]);
          },
        );
      },
    ));
  }

  Widget buildVideoView(BuildContext context, _UserSession session) {
    return Stack(
      children: [
        NERtcVideoView(
          uid: session.uid == widget.uid ? null : session.uid,
          subStream: session.subStream,
          mirrorListenable: session.mirror,
          rendererEventLister: this,
          fitType: NERtcVideoViewFitType.cover,
        ),
        Row(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Text(
              session.subStream ? '${session.uid} @ Sub' : '${session.uid}',
              style: const TextStyle(color: Colors.red, fontSize: 10),
            )
          ],
        )
      ],
    );
  }

  Widget buildControlPanel1(BuildContext context) {
    return Container(
        height: 40,
        child: Row(
          children: [
            Expanded(
                child: buildControlButton(
              () {
                _engine.deviceManager.switchCamera().then((value) {
                  if (value == 0) {
                    setState(() {
                      isFrontCamera = !isFrontCamera;
                    });
                    updateLocalMirror();
                  }
                });
              },
              Text(
                isFrontCamera ? '使用后置摄像头' : '使用前置摄像头',
                style: const TextStyle(fontSize: 12),
              ),
            )),
            Expanded(
                child: buildControlButton(
              () {
                bool videoEnabled = !isVideoEnabled;
                _engine.enableLocalVideo(videoEnabled).then((value) {
                  if (value == 0) {
                    setState(() {
                      isVideoEnabled = videoEnabled;
                    });
                  }
                });
              },
              Text(
                isVideoEnabled ? '关闭摄像头' : '打开摄像头',
                style: const TextStyle(fontSize: 12),
              ),
            )),
          ],
        ));
  }

  Widget buildControlPanel2(BuildContext context) {
    return Container(
        height: 40,
        child: Row(
          children: [
            Expanded(
                child: buildControlButton(
              () {
                bool audioEnabled = !isAudioEnabled;
                _engine.muteLocalAudioStream(audioEnabled).then((value) {
                  if (value == 0) {
                    setState(() {
                      isAudioEnabled = audioEnabled;
                    });
                  }
                });
              },
              Text(
                isAudioEnabled ? '关闭麦克风' : '打开麦克风',
                style: const TextStyle(fontSize: 12),
              ),
            )),
            Expanded(
                child: buildControlButton(
              () {
                bool speakerEnabled = !isSpeakerEnabled;
                _engine.deviceManager
                    .setSpeakerphoneOn(speakerEnabled)
                    .then((value) {
                  if (value == 0) {
                    setState(() {
                      isSpeakerEnabled = speakerEnabled;
                    });
                  }
                });
              },
              Text(
                isSpeakerEnabled ? '使用听筒' : '使用扬声器',
                style: const TextStyle(fontSize: 12),
              ),
            )),
          ],
        ));
  }

  void updateLocalMirror() {
    _localSession?.mirror.value = isFrontCamera && isFrontCameraMirror;
  }

  Widget buildControlButton(VoidCallback onPressed, Widget child) {
    return ElevatedButton(
      onPressed: onPressed,
      child: child,
    );
  }

  @override
  void onUserJoined(int uid, NERtcUserJoinExtraInfo? joinExtraInfo) {
    setState(() {});
  }

  @override
  void onUserLeave(
      int uid, int reason, NERtcUserLeaveExtraInfo? leaveExtraInfo) {
    for (_UserSession session in _remoteSessions.toList()) {
      if (session.uid == uid) {
        _remoteSessions.remove(session);
      }
    }
    setState(() {});
  }

  @override
  void onUserVideoStart(int uid, int maxProfile) {
    setupVideoView(uid, maxProfile, false);
  }

  Future<void> setupVideoView(int uid, int maxProfile, bool subStream) async {
    final session = _UserSession(uid, subStream);
    _remoteSessions.add(session);
    _engine.subscribeRemoteVideoStream(
        uid, NERtcRemoteVideoStreamType.high, true);
    setState(() {});
  }

  Future<void> releaseVideoView(int uid, bool subStream) async {
    for (_UserSession session in _remoteSessions.toList()) {
      if (session.uid == uid && subStream == session.subStream) {
        _remoteSessions.remove(session);
        _engine.subscribeRemoteSubStreamVideo(uid, false);
        setState(() {});
        break;
      }
    }
  }

  @override
  void onUserVideoStop(int uid) {
    releaseVideoView(uid, false);
  }

  @override
  void onFirstFrameRendered(int uid) {
    // TODO: implement onFirstFrameRendered
  }

  @override
  void onFrameResolutionChanged(int uid, int width, int height, int rotation) {
    // TODO: implement onFrameResolutionChanged
  }
}

class _UserSession {
  final int uid;
  final bool subStream;

  _UserSession(this.uid, [this.subStream = false]);

  ValueNotifier<bool>? _mirror;
  ValueNotifier<bool> get mirror {
    _mirror ??= ValueNotifier<bool>(false);
    return _mirror!;
  }
}
