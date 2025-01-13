
/*jslint white: true*/
/**
 * 文件上传SDK
 *     1、兼容性说明：
 *         仅兼容IE10+、Firefox、Chrome等支持slice和localStorage的浏览器。
 *     2、SDK依赖md5插件，请提前引入。
 *     3、执行getInitInfo初始化前需要先调用鉴权接口（应用服务器自行实现）：http://vcloud.163.com/docs/api.html（API token校验）。
 *     4、调用方法：
 *      const uploader = new Uploader({
 *         //配置对象，将覆盖默认配置
 *         fileInputId: '', //上传输入框ID，如果不传，需要在页面中手动绑定onFileInputChange事件
 *         fileUploadId: '', //上传按钮ID，如果不传，需要在页面中手动绑定onFileUploadClick事件
 *         getCheckSum: function(){ //获取鉴权信息
 *             ...
 *             return { AppKey, Nonce, CurTime, CheckSum }
 *         }
 *
 *         ...
 *     })
 *     uploader.init()
 *     其中，配置对象的fileInputId、fileUploadId、getCheckSum，以及所有onXxx等回调函数需要自行修改实现（以配置对象参数方式传入init函数）。
 * @module uploader
 * @class Uploader
 * @static
 * @param {Object} options 配置项对象
 * @return {Object} 接口对象
 * @author luoweiping
 * @version 1.2.0
 */

//import md5 from 'md5'

class Service {
    constructor(opts) {
        this.fileList = []
        this.successList = []
        this.dnsList = null
        this.opts = opts
    }

    getDNS(param, callback) {
        if (this.dnsList) {//已缓存则直接取缓存数据
            callback(this.dnsList);
        } else {
            let centerUrl = ['https://wanproxy-web.127.net']
            return callback(centerUrl)
            // fetch(this.opts.urlDns, {
            //     method: 'GET',
            //     headers: {
            //         'Content-Type': 'application/json',
            //         'version': '1.0',
            //         'bucketname': param.bucketName
            //     },
            // }).then(res => {
            //     return res.json()
            // }).then(data => {
            //     if (data.code) {
            //         this.opts.onError({
            //             errCode: data.Code || data.code,
            //             errMsg: data.Message
            //         });
            //     } else {
            //         this.dnsList = data.upload;
            //         callback(data.upload);
            //     }
            // }).catch(err => {
            //     this.opts.onError(err);
            // })
        }
    }
    /**
     * 删除文件，终止上传并从列表中移除（进度保持不变）
     * @method removeFile
     * @static
     * @param  {Object} file 文件对象
     * @return {void}
     * @version 1.0.0
     */
    removeFile(file) {
        this.fileList.forEach((v, i) => {
            if (v.fileKey === file.fileKey) {
                if (v.xhr) {
                    v.xhr.upload.onprogress = () => { };
                    v.xhr.onreadystatechange = () => { };
                    v.xhr.abort();

                    v.xhr = null;
                }
                this.fileList.splice(i, 1);

                if (v.status === 1) {
                    this.upload(i);
                }
                return false;
            }
        })
    }
    /**
     * 根据fileKey获取指定文件对象
     * @method getFile
     * @static
     * @param  {String} fileKey 文件名和文件大小md5值
     * @return {Obejct}         文件对象
     * @version 1.0.0
     */
    getFile(fileKey) {
        let curFile;
        this.fileList.forEach((v, i) => {
            if (v.fileKey === fileKey) {
                curFile = v;
                return false;
            }
        })

        return curFile;
    }
    /**
     * 上传分片
     * @method uploadTrunk
     * @static
     * @param  {Object}   param     AJAX参数
     * @param  {Object}   trunkData 分片数据
     * @param  {Function} callback  文件（非分片）上传成功回调函数
     * @return {void}
     * @version 1.0.0
     */
    uploadTrunk(param, trunkData, callback) {
        let xhr,
            xhrParam = '',
            curFile,
            context;
        curFile = this.getFile(trunkData.fileKey);
        context = localStorage.getItem(trunkData.fileKey + '_context');

        if (curFile.xhr) {
            xhr = curFile.xhr;
        } else {
            xhr = new XMLHttpRequest();
            curFile.xhr = xhr;
        }

        xhr.upload.onprogress = (e) => {
            let progress = 0;

            if (e.lengthComputable) {
                progress = (trunkData.offset + e.loaded) / trunkData.file.size;
                curFile.progress = (progress * 100).toFixed(2);

                if (progress > 0 && progress < 1) {
                    curFile.status = 1;
                } else if (progress === 1) {
                    curFile.status = 2;
                }
                localStorage.setItem(trunkData.fileKey + '_progress', curFile.progress);
                this.opts.onProgress(curFile);
            } else {
                this.opts.onError({
                    errCode: 501,
                    errMsg: '浏览器不支持进度事件'
                });
            }
        };

        xhr.onreadystatechange = () => {
            if (xhr.readyState !== 4) {
                return;
            }
            let result;
            try {
                result = JSON.parse(xhr.responseText);
            } catch (e) {
                result = {
                    errCode: 500,
                    errMsg: '未知错误'
                };
            }
            if (xhr.status === 200) {
                if (!result.errCode) {
                    localStorage.setItem(trunkData.fileKey + '_context', result.context);
                    if (result.offset < trunkData.file.size) {//上传下一片
                        this.uploadTrunk(param, Object.assign({}, trunkData, {
                            offset: result.offset,
                            trunkEnd: result.offset + trunkData.trunkSize,
                            context: context || result.context
                        }), callback);
                    } else {//单文件上传结束
                        callback(this, trunkData);
                    }
                } else {
                    this.clearStorage(trunkData.fileKey);
                    this.opts.onError({
                        errCode: result.errCode,
                        errMsg: result.errMsg
                    });
                }
            } else {
                if (xhr.status) {//nos error
                    this.clearStorage(trunkData.fileKey);
                }
                //取消、关闭情况
                this.opts.onError(xhr.responseText);
            }
        };
        xhrParam = '?offset=' + trunkData.offset + '&complete=' + (trunkData.trunkEnd >= trunkData.file.size) + '&context=' + (context || trunkData.context) + '&version=1.0';

        xhr.open('post', param.serveIp + '/' + param.bucketName + '/' + param.objectName + xhrParam);
        xhr.setRequestHeader('x-nos-token', param.nosToken);
        xhr.send(trunkData.file.slice(trunkData.offset, trunkData.trunkEnd));
    }
    /**
     * 获取上传断点位置
     * @method getOffset
     * @static
     * @param  {Object}   param    AJAX参数
     * @param  {Function} callback 获取成功回调
     * @return {void}
     * @version 1.0.0
     */
    getOffset(param, callback) {
        let context;
        context = localStorage.getItem(param.fileKey + '_context');
        if (!context) {
            return callback(0);
        }
        fetch(param.serveIp + '/' + param.bucketName + '/' + param.objectName + '?uploadContext', {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json',
                'version': '1.0',
                'context': context,
                'x-nos-token': param.nosToken
            },
        }).then(res => {
            return res.json()
        }).then(data => {
            if (data.errCode) {
                this.opts.onError({
                    errCode: data.errCode,
                    errMsg: data.errMsg
                });
            } else {
                this.callback(data.offset);
            }
        }).catch(err => {
            this.opts.onError(err);
        })
    }
    clearStorage(fileKey) {
        localStorage.removeItem(fileKey + '_progress');
        localStorage.removeItem(fileKey + '_context');
        localStorage.removeItem(fileKey + '_created');
        localStorage.removeItem(fileKey + '_bucket');
        localStorage.removeItem(fileKey + '_object');
        localStorage.removeItem(fileKey + '_xNosToken');
    }
    /**
     * 上传文件操作
     * @method upload
     * @static
     * @param  {Number} fileIdx 文件索引
     * @return {void}
     * @version 1.0.0
     */
    upload(fileIdx) {
        if (fileIdx < this.fileList.length) {
            if (this.fileList[fileIdx].status === 2 || !this.fileList[fileIdx].checked) {//上传完成或未勾选
                return this.upload(fileIdx + 1);
            }
            console.log('upload', fileIdx, this.fileList[fileIdx])
            this.opts.getInitInfo(this.fileList[fileIdx], (data) => {
                let curFile = this.fileList[fileIdx];
                curFile.objectName = data.objectName;
                curFile.bucketName = data.bucketName;

                this.getDNS(data, (dnsList) => {
                    let curFile = this.fileList[fileIdx];
                    this.getOffset({
                        serveIp: dnsList[0],
                        bucketName: data.bucketName,
                        objectName: data.objectName,
                        nosToken: data.nosToken,
                        fileKey: this.fileList[fileIdx].fileKey
                    }, (offset) => {
                        this.uploadTrunk({
                            serveIp: dnsList[0],
                            bucketName: data.bucketName,
                            objectName: data.objectName,
                            nosToken: data.nosToken
                        }, {
                            file: this.fileList[fileIdx].file,
                            fileKey: this.fileList[fileIdx].fileKey,
                            fileIdx: fileIdx,
                            offset: offset || 0,
                            trunkSize: this.opts.trunkSize,
                            trunkEnd: (offset || 0) + this.opts.trunkSize,
                            context: ''
                        }, (trunkData) => {
                            this.clearStorage(trunkData.fileKey);
                            this.opts.onUploaded(this, curFile);
                            this.upload(fileIdx + 1);
                        });
                    });
                });
            });
        } else {
            this.opts.onAllUploaded();
        }
    }
    /**
     * 添加文件
     * @method addFile
     * @static
     * @param {Element}   fileInput 上传输入框元素
     * @param {Function} callback  添加成功回调
     * @return {void}
     * @version 1.0.0
     */
    addFile(fileInput, callback) {
        let file = fileInput,
            fileKey = md5(file.name + ':' + file.size),
            fileObj;

        fileObj = {
            fileKey: fileKey,
            file: file,
            fileName: file.name,
            fileSizeMb: (file.size / 1024 / 1024).toFixed(2),
            format: file.name.split('.').pop(),
            status: 0,
            checked: true,
            progress: localStorage.getItem(fileKey + '_progress') || 0
        };
        this.fileList.push(fileObj);
        localStorage.setItem(fileKey + '_created', +new Date());
        callback(fileInput, fileObj);
    }
    /**
     * 判断文件是否已存在列表中
     * @method checkExist
     * @static
     * @param  {File} file File对象
     * @return {Boolean}      存在：true，不存在：false
     * @version 1.0.0
     */
    checkExist(file) {
        let exist = false,
            curKey = md5(file.name + ':' + file.size);
        this.fileList.forEach((v, i) => {
            if (curKey === v.fileKey) {
                exist = true;
                return false;
            }
        })
        return exist;
    }
    /**
     * 判断是否有待上传（已选中且上传未完成）的文件
     * @method checkedPending
     * @static
     * @return {Boolean} 有：true，无：false
     * @version 1.0.0
     */
    checkedPending() {
        let checked = false;
        this.fileList.forEach((v, i) => {
            if (v.checked && v.status === 0) {
                checked = true;
                return false;
            }
        })
        return checked;
    }
    /**
     * 事件绑定
     * @method initEvent
     * @static
     * @return {void}
     * @version 1.0.0
     */
    initEvent() {
        if (!this.opts.fileInputId || !this.opts.fileUploadId) {
            return false
        }
        const fileInput = document.getElementById(this.opts.fileInputId)
        const fileUpload = document.getElementById(this.opts.fileUploadId)
        if (!fileInput || !fileUpload) {
            console.error('请检查fileInputId和fileUploadId是否正确');
            return false;
        }
        fileInput.addEventListener('change', (e) => {
            let fileExt = '';
            if (e.target.files) {
                const files = e.target.files
                for (let i = 0; i < files.length; i++) {
                    if (!this.checkExist(files[i])) {
                        fileExt = files[i].name.split('.').pop();
                        fileExt = fileExt.toUpperCase();
                        if (!this.opts.fileExts.includes(fileExt)) {
                            this.opts.mismatchFn();
                            continue;
                        }
                        this.addFile(files[i], (fileInput, fileObj) => {
                            this.opts.onAdd(fileObj);
                            if (i == files.length - 1) {
                                e.target.value = '';
                            }
                        });
                    } else {
                        this.value = '';
                        this.opts.existFn();
                    }

                }
            }
        });

        fileUpload.addEventListener('click', () => {
            if (!this.checkedPending()) {
                this.opts.noUploadFn();
                return false;
            }
            localStorage.clear()
            this.upload(0);
            return false;
        })
    }
    /**
     * 初始化
     * @method init
     * @static
     * @return {void}
     * @version 1.0.0
     */
    init() {
        this.initEvent();
    }
    /**
     * 文件输入框change事件
     * @method onFileInputChange
     * @static
     * @param  {Event} e 事件对象
     * @return {void}
     * @version 1.2.0
     */
    onFileInputChange(e) {
        let fileExt = '';
        if (e.target.files) {
            const files = e.target.files
            for (let i = 0; i < files.length; i++) {
                if (!this.checkExist(files[i])) {
                    fileExt = files[i].name.split('.').pop();
                    fileExt = fileExt.toUpperCase();
                    if (!this.opts.fileExts.includes(fileExt)) {
                        this.opts.mismatchFn();
                        continue;
                    }
                    this.addFile(files[i], (fileInput, fileObj) => {
                        this.opts.onAdd(fileObj);
                        if (i == files.length - 1) {
                            e.target.value = '';
                        }
                    });
                } else {
                    this.value = '';
                    this.opts.existFn();
                }

            }
        }
    }
    /**
     * 上传按钮点击事件
     * @method onFileUploadClick
     * @static
     * @return {Boolean} false
     * @version 1.2.0
     */
    onFileUploadClick() {
        if (!this.checkedPending()) {
            this.opts.noUploadFn();
            return false;
        }
        localStorage.clear()
        this.upload(0);
        return false;
    }
}

class Uploader {
    constructor(opts) {
        this.defaults = {
            /**
             * 分片大小
             * @attribute trunkSize
             * @writeOnce
             * @type {Number}
             */
            trunkSize: 4 * 1024 * 1024,
            /**
             * 获取dns列表的URL
             * @attribute urlDns
             * @writeOnce
             * @type {String}
             */
            urlDns: 'http://wanproxy.127.net/lbs',

            urlHttps: 'https://wanproxy-web.127.net',
            /**
             * 上传输入框元素ID，如果不传，需要在页面中手动绑定onFileInputChange事件
             * @attribute fileInputId
             * @writeOnce
             * @type {String}
             */
            fileInputId: '',
            /**
             * 上传按钮ID，如果不传，需要在页面中手动绑定onFileUploadClick事件
             * @attribute fileUploadId
             * @writeOnce
             * @type {String}
             */
            fileUploadId: '',
            fileExts: ['JPG', 'PNG', 'WMV', 'ASF', 'AVI', '3GP', 'MKV', 'MP4', 'DVD', 'OGM', 'MOV', 'MPG', 'MPEG', 'MPE', 'FLV', 'F4V', 'SWF', 'M4V', 'QT', 'DAT', 'VOB', 'RMVB', 'RM', 'OGM', 'M2TS', 'MTS', 'TS', 'TP', 'WEBM', 'MP3', 'AAC'],
            /**
             * 获取初始化信息
             *     发送请求到视频云服务端或应用服务器，参数见代码注释；
             *     其中，typeId和presetId需自行获取(接口文档暂未发布，请联系客服)，headers参数为API token校验返回的结果(必填)
             * @method fileUploadId
             * @static
             * @param  {Object}   file     文件对象
             *      fileKey: 对文件名和文件大小进行md5后的结果
             *      file: File对象
             *      fileName: 文件名（作为file.name的备份）
             *      fileSizeMb: 文件大小（MB）
             *      format: 文件后缀
             *      status: 上传状态（0：待上传，1：上传中；2：上传完成）
             *      checked: 是否选中（用于列表）
             *      progress: 上传进度
             * @param  {Function} callback 回调函数
             *      回调函数的参数包括：
             *      bucketName: 桶名
             *      objectName: 对象名
             *      nosToken: x-nos-token
             * @return {void}
             * @version 1.0.0
             */
            getInitInfo: function (file, callback) {
                let context;
                context = localStorage.getItem(file.fileKey + '_context');
                if (!context) {
                    const { AppKey, Nonce, CurTime, CheckSum } = this.getCheckSum()
                    fetch('https://vcloud.163.com/app/vod/upload/init', {
                        method: 'POST',
                        headers: {
                            'Content-Type': 'application/json',
                            AppKey,
                            Nonce,
                            CurTime,
                            CheckSum
                        },
                        body: JSON.stringify({
                            originFileName: file.file.name,
                            userFileName: file.file.name,
                            typeId: null,
                            presetId: null,
                            callbackUrl: null,
                            description: null,
                            //视频预览截图
                            previewSnapshot: false,
                            previewSnapshotPreset: null,
                        })
                    }).then(res => {
                        return res.json()
                    }).then(data => {
                        /*
                          data格式：
                          "Content-Type": "application/json; charset=utf-8"
                          {
                              "code": 200,
                              "msg": "",
                              "ret": {
                                  "xNosToken": "xxsfsgdsgetret",
                                  "bucket": "origv10000",
                                  "object": "qrwr-eete-dsft-vdfg.mp4"
                              }
                          }
                      */
                        if (data.code === 200) {
                            localStorage.setItem(file.fileKey + '_bucket', data.ret.bucket);
                            localStorage.setItem(file.fileKey + '_object', data.ret.object);
                            localStorage.setItem(file.fileKey + '_xNosToken', data.ret.xNosToken);
                            callback({
                                'bucketName': data.ret.bucket,
                                'objectName': data.ret.object,
                                'nosToken': data.ret.xNosToken
                            });
                        } else {
                            this.onError({
                                errCode: data.Code || data.code,
                                errMsg: data.msg
                            });
                        }
                    }).catch(err => {
                        this.onError(err);
                    })
                } else {
                    callback({
                        'bucketName': localStorage.getItem(file.fileKey + '_bucket'),
                        'objectName': localStorage.getItem(file.fileKey + '_object'),
                        'nosToken': localStorage.getItem(file.fileKey + '_xNosToken')
                    });
                }
            },
            /**
             * 错误处理回调
             * @method onError
             * @static
             * @param  {Object} errObj 带errCode和errMsg的Object或XHR错误对象
             * @return {void}
             * @version 1.0.0
             */
            onError: function (errObj) {
                console.log(errObj);
            },
            /**
             * 上传进度回调
             * @method onProgress
             * @static
             * @param  {Object} curFile 文件对象
             * @return {void}
             * @version 1.0.0
             */
            onProgress: function (curFile) {
                console.log(`curFile: ${curFile.fileName} is uploading, ${curFile.progress}%, status :${curFile.status}`);
            },
            /**
             * 单文件上传成功回调
             * @method onUploaded
             * @static
             * @param  {Object} curFile 文件对象
             * @return {void}
             * @version 1.0.0
             */
            onUploaded: function (service, curFile) {
                console.log('File: ' + curFile.fileName + ' is uploaded.');
                // 将文件信息存入上传成功列表
                service.successList.push(curFile);
                /**
                 * 用于获取vid等信息，暂只支持在单个文件上传成功后的回调中进行
                 * 在全部上传成功的回调中发起请求会导致在上传失败时无法执行请求（接口的URL、参数格式、响应格式等均相同）
                 */
                const { AppKey, Nonce, CurTime, CheckSum } = this.getCheckSum()
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
                        /**
                         * 根据需要进行处理，返回的data格式：
                         * "Content-Type": "application/json; charset=utf-8"
                         * {
                         *     "code" : 200,
                         *     "msg": "",
                         *     "ret" : {
                         *         "count": 1,
                         *         "list" : [{
                         *             "objectName" : "33cf71b1-86ac-4555-a071-d70db07b9685.mp4",
                         *             "vid" : 1008
                         *         },
                         *         ...
                         *         ]
                         *     }
                         * }
                         */
                    } else {
                        this.opts.onError({
                            errCode: data.Code || data.code,
                            errMsg: data.msg
                        });
                    }
                }).catch(err => {
                    this.opts.onError(err);
                })
            },
            /**
             * 全部文件上传成功回调
             * @method onAllUploaded
             * @static
             * @return {void}
             * @version 1.0.0
             */
            onAllUploaded: function () {
                console.log('All done.');
            },
            /**
             * 文件添加成功回调
             * @method onAdd
             * @static
             * @param  {File} fileObj 文件对象
             * @return {void}
             * @version 1.0.0
             */
            onAdd: function (curFile) {
                console.log(curFile.file.name + ': ' + curFile.fileSizeMb + ' MB');
            },
            /**
             * 无文件上传时的处理函数
             * @method noUploadFn
             * @static
             * @return {void}
             * @version 1.0.0
             */
            noUploadFn: function () {
                console.log('请选择待上传的文件');
            },
            /**
             * 文件已存在列表中的处理函数
             * @method existFn
             * @static
             * @return {void}
             * @version 1.0.0
             */
            existFn: function () {
                console.warn('文件已存在列表中');
            },
            /**
             * 文件格式不匹配的处理函数
             * @method existFn
             * @static
             * @return {void}
             * @version 1.0.0
             */
            mismatchFn: function () {
                console.error('不是有效的视频或图片格式');
            },
            /**
             * 获取checkSum
             * @method getCheckSum
             * @version 1.2.0
             */
            getCheckSum: function () {
                console.log('getCheckSum， 这里建议改为从服务端获取')
                const AppKey = '',  //开发者平台分配的appkey
                    AppSecret = '',  // 开发者平台分配的appSecret，注意不要写在前端代码中，以防泄露
                    Nonce = '',  //随机数（随机数，最大长度128个字符）
                    CurTime = '',  //当前UTC时间戳，从1970年1月1日0点0分0秒开始到现在的秒数
                    CheckSum = ''  //服务器认证需要,SHA1(AppSecret+Nonce+CurTime),16进制字符小写
                return { AppKey, Nonce, CurTime, CheckSum }
            }
        }
        this.opts = Object.assign(this.defaults, opts)
        this.service = new Service(this.opts)
    }

    init() {
        this.service.init()
    }
}

//export default Uploader