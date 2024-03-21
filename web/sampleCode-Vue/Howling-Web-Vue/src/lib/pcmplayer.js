class PcmPlayer {
    constructor(options) {
        console.warn('PcmPlayer options: ', options);
        this.audioCtx = null;
        this.gainNode = null;
        this.startTime = 0;
        this.channels = options.channel;
        this.sampleRate = options.sampleRate;
        this.samples = null;
        this.createContext();
    }


    createContext() {
        this.audioCtx = new (window.AudioContext || window.webkitAudioContext)();

        // context needs to be resumed on iOS and Safari (or it will stay in "suspended" state)
       // this.audioCtx.resume();
        this.audioCtx.onstatechange = () => {
             console.warn('audioContext state: ', this.audioCtx.state);
            if (this.audioCtx.state == 'closed') {
                this.audioCtx = null;
            }
        };

        this.gainNode = this.audioCtx.createGain();
        this.gainNode.gain.value = 1;
        this.gainNode.connect(this.audioCtx.destination);
        this.startTime = this.audioCtx.currentTime;

    }

    feed(frame) {
        this.samples = frame
        let bufferSource = this.audioCtx.createBufferSource(),
            length = this.samples.length / this.channels,
            audioBuffer = this.audioCtx.createBuffer(this.channels, length, this.sampleRate),
            audioData,
            channel,
            offset,
            i,
            decrement;

        for (channel = 0; channel < this.channels; channel++) {
            audioData = audioBuffer.getChannelData(channel);
            offset = channel;
            decrement = 50;
            for (i = 0; i < length; i++) {
                audioData[i] = this.samples[offset];
                /* fadein */
                if (i < 50) {
                    audioData[i] = (audioData[i] * i) / 50;
                }
                /* fadeout*/
                if (i >= (length - 51)) {
                    audioData[i] = (audioData[i] * decrement--) / 50;
                }
                offset += this.channels;
            }
        }

        bufferSource.buffer = audioBuffer;
        bufferSource.playbackRate.value = 1;
        bufferSource.connect(this.gainNode);
        bufferSource.start(this.startTime);
        console.warn('duration: ', audioBuffer.duration)
        this.startTime += audioBuffer.duration;
        this.samples = new Float32Array();
    }

    play() {
        return this.audioCtx.resume();
    }

    pause() {
        return this.audioCtx.suspend();
    }

    destroy() {
        return this.audioCtx.close();
    }

}

export default PcmPlayer;