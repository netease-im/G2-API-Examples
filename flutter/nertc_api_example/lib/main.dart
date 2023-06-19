// Copyright (c) 2022 NetEase, Inc. All rights reserved.
// Use of this source code is governed by a MIT license that can be
// found in the LICENSE file.
import 'package:flutter/material.dart';
import 'package:nertc_api_example/AudioCall/audioCallingPage.dart';
import 'package:nertc_api_example/VideoCall/videoCallingPage.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: '网易云信 NERtc API Example',
      theme: ThemeData(
        primarySwatch: Colors.blue,
      ),
      home: const MyHomePage(title: '网易云信 NERtc API Example'),
    );
  }
}

class MyHomePage extends StatefulWidget {
  const MyHomePage({Key? key, required this.title}) : super(key: key);
  final String title;

  @override
  State<MyHomePage> createState() => _MyHomePageState();
}

class _MyHomePageState extends State<MyHomePage> {
  int _counter = 0;

  void _incrementCounter() {
    setState(() {
      _counter++;
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text(widget.title),
      ),
      body: Container(
        margin: const EdgeInsets.all(15.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text("基础功能",
                textAlign: TextAlign.left,
                style: TextStyle(color: Colors.black, fontSize: 18)),
            const SizedBox(
              height: 10,
            ),
            Row(crossAxisAlignment: CrossAxisAlignment.start, children: [
              Expanded(
                child: ElevatedButton(
                    onPressed: () {
                      Navigator.push(
                        context,
                        MaterialPageRoute(
                            builder: (context) => AudioCallingPage()),
                      );
                    },
                    style: ElevatedButton.styleFrom(
                        primary: Colors.blue,
                        shape: RoundedRectangleBorder(
                          borderRadius: BorderRadius.circular(10.0),
                        ),
                        padding: const EdgeInsets.symmetric(
                            horizontal: 10, vertical: 5)),
                    child: Align(
                      alignment: Alignment.centerLeft,
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        mainAxisAlignment: MainAxisAlignment.spaceBetween,
                        children: const [
                          Text('语音通话', style: TextStyle(fontSize: 18)),
                          Text('一对一或一对多语音通话，包含免提/静音等功能',
                              style: TextStyle(fontSize: 12))
                        ],
                      ),
                    )),
              ),
            ]),
            const SizedBox(
              height: 10,
            ),
            Row(crossAxisAlignment: CrossAxisAlignment.start, children: [
              Expanded(
                  child: ElevatedButton(
                      onPressed: () {
                        Navigator.push(
                          context,
                          MaterialPageRoute(
                              builder: (context) => VideoCallingPage()),
                        );
                      },
                      style: ElevatedButton.styleFrom(
                          primary: Colors.blue,
                          shape: RoundedRectangleBorder(
                            borderRadius: BorderRadius.circular(10.0),
                          ),
                          padding: const EdgeInsets.symmetric(
                              horizontal: 10, vertical: 5)),
                      child: Align(
                        alignment: Alignment.centerLeft,
                        child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            mainAxisAlignment: MainAxisAlignment.spaceBetween,
                            children: const [
                              Text('视频通话', style: TextStyle(fontSize: 18)),
                              Text('一对一或一对多视频通话，包含免提/静音等功能',
                                  style: TextStyle(fontSize: 12)),
                            ]),
                      )))
            ]),
            const SizedBox(
              height: 10,
            ),
          ],
        ),
      ),
    );
  }
}
