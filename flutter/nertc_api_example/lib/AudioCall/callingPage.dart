// Copyright (c) 2022 NetEase, Inc. All rights reserved.
// Use of this source code is governed by a MIT license that can be
// found in the LICENSE file.

import 'package:flutter/material.dart';
import 'package:nertc_api_example/config.dart';
import 'package:nertc_core/nertc_core.dart';

class CallPage extends StatefulWidget {
  final String cid;
  final int uid;

  CallPage({Key? key, required this.cid, required this.uid});

  @override
  _CallPageState createState() {
    return _CallPageState();
  }
}

class _CallPageState extends State<CallPage>
    with NERtcVideoRendererEventListener, NERtcChannelEventCallback {
  final NERtcEngine _engine = NERtcEngine.instance;
  final List<_UserSession> _remoteSessions = [];
  _UserSession? _localSession;
  _UserSession? _localSubStreamSession;

  bool isSpeakerEnabled = false;
  bool isAudioEnabled = false;
  bool isLeaveChannel = false;

  @override
  void initState() {
    super.initState();
    print('start call: uid=${widget.uid}, cid=${widget.cid}');
    _initRtcEngine();
  }

  void _initRtcEngine() async {
    // NERtcOptions options = NERtcOptions();
    _engine
        .create(appKey: Config.APP_KEY, channelEventCallback: this)
        .then((value) => _engine.setEventCallback(this))
        .then((value) => _engine.enableLocalAudio(true))
        .then((value) => _engine.joinChannel('', widget.cid, widget.uid));
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
    _localSession = null;
    _localSubStreamSession = null;
    _remoteSessions.clear();
    await _engine.enableLocalAudio(false);
    await _engine.leaveChannel();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
        appBar: AppBar(
          leading: buildLeaveButton(context),
          title: Text('房间号:${widget.cid}'),
        ),
        body: Container(
            margin: const EdgeInsets.all(30.0),
            child: Column(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: <Widget>[
                  buildAudioViews(context),
                  buildControlPanel(context)
                ])));
  }

  Widget buildLeaveButton(BuildContext context) {
    return TextButton(
        onPressed: (() {
          Navigator.pop(context);
        }),
        child: const Text(
          'Leave',
          style: TextStyle(fontSize: 14, color: Colors.white),
        ));
  }

  Widget buildAudioViews(BuildContext context) {
    final sessions = [
      if (_localSession != null) _localSession!,
      if (_localSubStreamSession != null) _localSubStreamSession!,
      ..._remoteSessions,
    ];

    return Expanded(child: OrientationBuilder(
      builder: (BuildContext context, Orientation orientation) {
        return GridView.builder(
          gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
            crossAxisCount: 3,
            childAspectRatio: 3 / 4,
            crossAxisSpacing: 2.0,
            mainAxisSpacing: 2.0,
          ),
          itemCount: sessions.length,
          itemBuilder: (BuildContext context, int index) {
            return Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const CircleAvatar(
                  backgroundImage:
                      AssetImage('assets/audiocall_user_portrait.png'),
                  radius: 30,
                ),
                const SizedBox(height: 8),
                Text('${sessions[index].uid}',
                    style: const TextStyle(fontSize: 16)),
              ],
            );
          },
        );
      },
    ));
  }

  Widget buildControlPanel(BuildContext context) {
    return Container(
        height: 40,
        child: Row(
          children: [
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
                isAudioEnabled ? '打开麦克风' : '关闭麦克风',
                style: const TextStyle(fontSize: 12),
              ),
            )),
            Expanded(
                child: buildControlButton(
              () {
                _engine
                    .muteLocalAudioStream(true)
                    .then((value) => _engine.leaveChannel())
                    .then((value) => _leaveChannel());
              },
              const Text(
                '挂断',
                style: TextStyle(fontSize: 12),
              ),
            ))
          ],
        ));
  }

  Widget buildControlButton(VoidCallback onPressed, Widget child) {
    return ElevatedButton(
      onPressed: onPressed,
      child: child,
    );
  }

  @override
  void onLeaveChannel(int result) {
    setState(() {});
  }

  @override
  void onDisconnect(int reason) {
    print('onDisconnect#$reason');
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
  void onUserAudioStart(int uid) {
    final session = _UserSession(uid);
    _remoteSessions.add(session);
    setState(() {});
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
