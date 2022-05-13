import { NativeModules, NativeEventEmitter, EmitterSubscription } from 'react-native';

const { WebRTCModule } = NativeModules;

const EventEmitter = new NativeEventEmitter(WebRTCModule);

export default EventEmitter;

const _subscriptions: Map<any, EmitterSubscription[]> = new Map(); 

type EventHandler = (event: any) => void;

export function addListener(listener: any, eventName: string, eventHandler: EventHandler): void {
    if (!_subscriptions.has(listener)) {
        _subscriptions.set(listener, []);
    }

    _subscriptions.get(listener)?.push(EventEmitter.addListener(eventName, eventHandler));
}

export function removeListener(listener: any): void {
    _subscriptions.get(listener)?.forEach(sub => {
        sub.remove();
    });

    _subscriptions.delete(listener);
}
