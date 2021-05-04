
import { useCallback, useEffect, useState } from 'react';
import { Platform } from 'react-native';
import {NativeModules} from 'react-native';

const { WebRTCFilterModule } = NativeModules;

/**
 * useWebRTCFilter hook to toggle webrtc filter status applied to camera 
 * while streaming. Connects to native ios module to enable and disable the 
 * filters.
 */
export default function useWebRTCFilter() {
   
    const [enabled, setFilterEnabled] = useState(false);

    useEffect(() => {
        if(Platform.OS === "ios") {
            WebRTCFilterModule.setFilterEnabled(false)
        }
    }, [])

    const toggleEnabled = useCallback(() => {
        setFilterEnabled(e => {
            if(Platform.OS === "ios") {
                WebRTCFilterModule.setFilterEnabled(!e)
            }
            return !e;
        })
    }, [])


    return {
        enabled,
        toggleEnabled 
    }
}