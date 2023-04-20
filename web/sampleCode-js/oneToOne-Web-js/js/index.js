let appkey= ''// 请输入自己的appkey
let appSecret= '' // 请输入自己的appSecret

let channelName // '您指定的房间号'
let uid // '您指定的用户ID'
window.rtc = {
    client: null,
    joined: false,
    published: false,
    localStream: null,
    remoteStreams: {},
    params: {},
}
const localStoragePrefix = 'NERTC-'
initClient()

function loadEnv() {
    const channelName = window.localStorage
        ? window.localStorage.getItem(`${localStoragePrefix}channelName`) : ''
    const uid = window.localStorage
        ? window.localStorage.getItem(`${localStoragePrefix}uid`) : ''
    $('#channelName').val(channelName)
    $('#uid').val(uid)
}

function initClient() {
    loadEnv()
    rtc.client = NERTC.createClient({ appkey, debug: true })
}

async function initLocalStream() {
    const audio = true
    const video = true
    const createStreamOptions = {
        uid: $('#uid').val() || rtc.client.getChannelInfo().uid,
        audio,
        microphoneId: $('#micro').val(),
        video,
        client: rtc.client
    }
    if ($('#camera').val()) {
        createStreamOptions.cameraId = $('#camera').val()
    }
    if ($('#micro').val()) {
        createStreamOptions.microphoneId = $('#micro').val()
    }
    rtc.localStream = NERTC.createStream(createStreamOptions)
    await rtc.localStream.init()
    // 设置本地视频画布
    rtc.localStream.setLocalRenderMode({
        width: 320,
        height: 240
    })
}

async function join() {
    channelName = parseInt($('#channelName').val())
    if (window.localStorage) {
        window.localStorage.setItem(`${localStoragePrefix}channelName`, channelName)
    }
    uid = $('#uid').val()
     // 监听事件
    rtc.client.on('stream-added', async(event) => {
        const remoteStream = event.stream
        console.warn('收到别人的发布消息: ', remoteStream.streamID, 'mediaType: ', event.mediaType)
        rtc.remoteStreams[remoteStream.streamID] = remoteStream
        // 已经有两人在房间中，剔除（不能排除观众）
        if (Object.keys(rtc.remoteStreams).length === 2) {
            await this.handleOver()
        }
        //订阅远端流
        rtc.client.subscribe(remoteStream).then(() => {
            console.warn(`subscribe 成功 ${remoteStream.streamID}`)
        })
    })

    rtc.client.on('stream-subscribed', async (event) => {
        // 远端流订阅成功
        const remoteStream = event.stream
        console.warn('订阅别人的流成功的通知: ', remoteStream.streamID, 'mediaType: ',event.mediaType
        )
        // 设置远端视频画布
        remoteStream.setRemoteRenderMode({
            width: 320,
            height: 240
        })
        // 播放远端流
        await remoteStream.play('remoteVideoContent')
    })

    rtc.client.on('stream-removed', (evt) => {
        let remoteStream = evt.stream
        console.warn('收到别人停止发布的消息: ', remoteStream.streamID, 'mediaType: ', evt.mediaType)
        remoteStream.stop(evt.mediaType)
    })

    rtc.client.on('uid-duplicate', () => {
        console.warn('==== uid重复，你被踢出');
    });

    rtc.client.on('error', (type) => {
        console.error('===== 发生错误事件：', type);
        if (type === 'SOCKET_ERROR') {
            console.warn('==== 网络异常，已经退出房间');
        }
    });

    rtc.client.on('accessDenied', (type) => {
        console.warn(`==== ${type}设备开启的权限被禁止`);
    });

    rtc.client.on('connection-state-change', (evt) => {
        console.warn(
            `网络状态变更: ${evt.prevState} => ${evt.curState}, 当前是否在重连：${evt.reconnect}`
        );
    });

    try {
        await rtc.client.join({
            channelName,
            uid,
            wssArr: $('#isGetwayAddrConf').prop('checked') ? $('#getwayAddr').val().split(',') : null
        })
        await initLocalStream()
        // 播放本地流
        const playOptions = {
            audio: true,
            audioType: true,
            video: true,
            screen: true
        }
        await rtc.localStream.play('localVideoContent', playOptions)
        await rtc.client.publish(rtc.localStream)
    } catch (error) {
        console.error(error)
    }
}

async function handleOver() {
    await rtc.client.leave()
    rtc.localStream.destroy()
    rtc.client.destroy()
}

async function leave() {
    await rtc.client.leave()
}

// 进房
$('#join').on('click', async() => {
    console.log('进房')
    join()
})
// 离开
$('#leave').on('click', async() => {
    console.log('离开')
    leave()
})
