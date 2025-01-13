import React, { useState, useEffect } from "react";
import Uploader from "../lib/upload";
import sha1 from 'sha1';
import { AppKey, AppSecret } from "../config";

const VodUploader = () => {
    const [fileList, setFileList] = useState([]);
    const [successList, setSuccessList] = useState([]);
    const [uploader, setUploader] = useState(null);
    const [progress, setProgress] = useState(0);
    const [currentFile, setCurrentFile] = useState(null);

    useEffect(() => {
        const uploader = new Uploader({
            onAdd,
            onProgress,
            onUploaded,
            onAllUploaded,
            onError,
            getCheckSum,
        })
        uploader.init()
        setUploader(uploader)
    }, [])

    const onError = (errObj) => {
        console.error(errObj);
    }

    const onProgress = (curFile) => {
        console.log(`curFile: ${curFile.fileName} is uploading, ${curFile.progress}%, status :${curFile.status}`);
        setProgress(curFile.progress)
        setCurrentFile(curFile)
    }

    const onUploaded = (service, curFile) => {
        console.log('File: ' + curFile.fileName + ' is uploaded.');
        // 将文件信息存入上传成功列表
        service.successList.push(curFile);
        const { AppKey, Nonce, CurTime, CheckSum } = getCheckSum()
        fetch('https://vcloud.163.com/app/vod/video/query', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                AppKey,
                Nonce,
                CurTime,
                CheckSum
            },
            body: JSON.stringify({
                objectNames: [curFile.objectName]
            }),
        }).then(res => {
            return res.json()
        }).then(data => {
            if (data.code === 200) {
                setSuccessList(pre => [...pre, {
                    fileName: curFile.fileName,
                    vid: data.ret.list[0].vid,
                    objectName: data.ret.list[0].objectName
                }])
            } else {
                onError({
                    errCode: data.Code || data.code,
                    errMsg: data.msg
                });
            }
        }).catch(err => {
            onError(err);
        })
    }

    const onAllUploaded = () => {
        console.log('All done.');
    }

    const onAdd = (file) => {
        console.log('File: ' + file.fileName + ' is added.');
        setFileList(pre => [...pre, file])
    }

    const getCheckSum = () => {
        //建议将此处的AppKey、AppSecret、Nonce、CurTime等信息存储在服务端，通过接口获取
        const Nonce = Math.ceil(Math.random() * 1e9),
            CurTime = Math.ceil(Date.now() / 1000),
            CheckSum = sha1(`${AppSecret}${Nonce}${CurTime}`);

        return { AppKey, Nonce, CurTime, CheckSum }
    }

    const onFileInputChange = (e) => {
        uploader?.service?.onFileInputChange(e)
    }

    const onFileUpload = () => {
        uploader?.service?.onFileUploadClick()
    }

    return (
        <div>
            <div>
                <input type="file" multiple onChange={onFileInputChange} />
                <button onClick={onFileUpload}>Upload</button>
            </div>
            <br />
            <div>
                <label >File List:</label>
                <ul>
                    {fileList.map((file, index) => {
                        return <li key={index}>{file.fileName}</li>
                    })}
                </ul>
            </div>
            <br />
            <div>
                <label >Progress: </label>
                <progress max="100" value={progress} />
                <span> {progress}% </span>
                <span>{currentFile?.fileName}</span>
            </div>
            <br />
            <div>
                <label >Success List: </label>
                <br />
                <ul>
                    {successList.map((item, index) => {
                        return <li key={index}>{item.fileName + ' - ' + item.vid + ' - ' + item.objectName}</li>
                    })}
                </ul>
            </div>
        </div>
    )
}

export default VodUploader;