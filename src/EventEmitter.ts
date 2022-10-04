import { NativeModules, NativeEventEmitter, EmitterSubscription } from 'react-native';

const { WebRTCModule } = NativeModules;

const EventEmitter = new NativeEventEmitter(WebRTCModule);

export default EventEmitter;

type EventHandler = (event: unknown) => void;
type Listener = unknown;

const _subscriptions: Map<Listener, EmitterSubscription[]> = new Map();

export function addListener(listener: Listener, eventName: string, eventHandler: EventHandler): void {
    if (!_subscriptions.has(listener)) {
        _subscriptions.set(listener, []);
    }

    _subscriptions.get(listener)?.push(EventEmitter.addListener(eventName, eventHandler));
}

export function removeListener(listener: Listener): void {
    _subscriptions.get(listener)?.forEach(sub => {
        sub.remove();
    });

    _subscriptions.delete(listener);
}
