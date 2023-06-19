import 'dart:io';
import 'dart:math';
import 'callingPage.dart';
import 'package:permission_handler/permission_handler.dart';
import 'package:flutter/material.dart';

enum ConfirmAction { CANCEL, ACCEPT }

class AudioCallingPage extends StatefulWidget {
  const AudioCallingPage({Key? key}) : super(key: key);

  @override
  State<StatefulWidget> createState() {
    return _AudioCallingPageState();
  }
}

class _AudioCallingPageState extends State<AudioCallingPage> {
  FocusNode _channelFocusNode = FocusNode();
  FocusNode _uidFocusNode = FocusNode();
  final _channelController =
      TextEditingController(text: getRandomChannelNumber());
  bool _channelValidateError = false;
  final _uidController = TextEditingController(text: getRandomUserNameNumber());
  bool _uidValidateError = false;

  @override
  void dispose() {
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
        appBar: AppBar(
          title: const Text('网易云信 NERtc API Example',
              style: TextStyle(fontSize: 18)),
        ),
        body: Container(
          margin: const EdgeInsets.all(30.0),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Column(
                mainAxisSize: MainAxisSize.max,
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Text("请输入房间号（必填项）",
                      textAlign: TextAlign.left,
                      style: TextStyle(color: Colors.black, fontSize: 16)),
                  TextField(
                    controller: _channelController,
                    onChanged: (value) {
                      if (_channelValidateError) {
                        setState(() {
                          _channelValidateError = value.isEmpty;
                        });
                      }
                    },
                    autofocus: true,
                    focusNode: _channelFocusNode,
                    decoration: InputDecoration(
                        hintText: _channelController.text,
                        errorText: _channelValidateError ? 'Required' : null),
                  ),
                  const SizedBox(
                    height: 20,
                  ),
                  const Text("请输入用户名（必填项）",
                      textAlign: TextAlign.left,
                      style: TextStyle(color: Colors.black, fontSize: 16)),
                  TextField(
                    focusNode: _uidFocusNode,
                    controller: _uidController,
                    onChanged: (value) {
                      if (_uidValidateError) {
                        setState(() {
                          _uidValidateError = value.isEmpty;
                        });
                      }
                    },
                    decoration: InputDecoration(
                        hintText: _uidController.text,
                        errorText: _uidValidateError ? 'Required' : null),
                  ),
                ],
              ),
              SizedBox(
                width: 200,
                child: ElevatedButton(
                  onPressed: () {
                    _startAudioCalling(context);
                  },
                  child: const Text('进入房间', style: TextStyle(fontSize: 18)),
                ),
              )
            ],
          ),
        ));
  }

  Future<void> _startAudioCalling(BuildContext context) async {
    _channelValidateError = _channelController.text.isEmpty;
    _uidValidateError = _uidController.text.isEmpty;

    setState(() {});

    if (_channelValidateError || _uidValidateError) return;

    //检查权限
    final permissions = [Permission.camera, Permission.microphone];
    if (Platform.isAndroid) {
      permissions.add(Permission.storage);
    }
    List<Permission> missed = [];
    for (var permission in permissions) {
      PermissionStatus status = await permission.status;
      if (status != PermissionStatus.granted) {
        missed.add(permission);
      }
    }

    bool allGranted = missed.isEmpty;
    if (!allGranted) {
      List<Permission> showRationale = [];
      for (var permission in missed) {
        bool isShown = await permission.shouldShowRequestRationale;
        if (isShown) {
          showRationale.add(permission);
        }
      }

      if (showRationale.isNotEmpty) {
        ConfirmAction? action = await showDialog<ConfirmAction>(
            context: context,
            builder: (BuildContext context) {
              return AlertDialog(
                content: const Text('You need to allow some permissions'),
                actions: <Widget>[
                  TextButton(
                    child: const Text('Cancel'),
                    onPressed: () {
                      Navigator.of(context).pop(ConfirmAction.CANCEL);
                    },
                  ),
                  TextButton(
                    child: const Text('Accept'),
                    onPressed: () {
                      Navigator.of(context).pop(ConfirmAction.ACCEPT);
                    },
                  )
                ],
              );
            });
        if (action == ConfirmAction.ACCEPT) {
          Map<Permission, PermissionStatus> allStatus = await missed.request();
          allGranted = true;
          for (var status in allStatus.values) {
            if (status != PermissionStatus.granted) {
              allGranted = false;
            }
          }
        }
      } else {
        Map<Permission, PermissionStatus> allStatus = await missed.request();
        allGranted = true;
        for (var status in allStatus.values) {
          if (status != PermissionStatus.granted) {
            allGranted = false;
          }
        }
      }
    }
    Navigator.push(
        context,
        MaterialPageRoute(
            builder: (context) => CallPage(
                  cid: _channelController.text,
                  uid: int.parse(_uidController.text),
                )));
    // }
  }

  static String getRandomChannelNumber() {
    var random = Random();
    int randomNumber = random.nextInt(99999999); // 生成一个 0-99999999 之间的随机整数
    return randomNumber.toString().padLeft(8, '0'); // 将随机数转换为字符串，并在左边补0，使其长度为8
  }

  static String getRandomUserNameNumber() {
    var random = Random();
    int randomNumber = random.nextInt(999999); // 生成一个 0-99999999 之间的随机整数
    return randomNumber.toString().padLeft(6, '0'); // 将随机数转换为字符串，并在左边补0，使其长度为8
  }
}
