
import { NativeModules } from 'react-native';
import MediaStreamTrack from './MediaStreamTrack';

export default class RtpSender {
    _id: String;
    _track: MediaStreamTrack;

    constructor(id, track) {
        this._id = id;
        this._track = track;
    }
    id = () => {
      return this._id;
    }
    track = () => {
      return this._track;
    }
}