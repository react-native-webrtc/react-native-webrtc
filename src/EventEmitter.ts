import { NativeModules, NativeEventEmitter, EmitterSubscription } from 'react-native';

const { WebRTCModule } = NativeModules;

const EventEmitter = new NativeEventEmitter(WebRTCModule);

export default EventEmitter;

const _subscriptions: Record<any, EmitterSubscription[]> = {}; 

type EventHandler = (event: any) => void;

export function addListener(listener: any, eventName: string, eventHandler: EventHandler): void {
  if (!_subscriptions[listener]) {
    _subscriptions[listener] = [];
  }
  _subscriptions[listener].push(EventEmitter.addListener(eventName, eventHandler));
}

export function removeListener(listener: any): void {
  if (!_subscriptions[listener]) {
    return;
  }
  _subscriptions[listener].forEach((eventHandler) => {
    eventHandler.remove();
  })
  delete _subscriptions[listener];
}