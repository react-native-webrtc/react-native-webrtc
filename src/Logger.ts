import debug from 'debug';


export default class Logger {
    static ROOT_PREFIX = 'rn-webrtc';

    private _debug: debug.Debugger;
    private _info: debug.Debugger;
    private _warn: debug.Debugger;
    private _error: debug.Debugger;

    static enable(ns: string): void {
        debug.enable(ns);
    }

    constructor(prefix: string) {
        const _prefix = `${Logger.ROOT_PREFIX}:${prefix}`;

        this._debug = debug(`${_prefix}:DEBUG`);
        this._info = debug(`${_prefix}:INFO`);
        this._warn = debug(`${_prefix}:WARN`);
        this._error = debug(`${_prefix}:ERROR`);

        const log = console.log.bind(console);

        this._debug.log = log;
        this._info.log = log;
        this._warn.log = log;
        this._error.log = log;
    }

    debug(msg: string): void {
        this._debug(msg);
    }

    info(msg: string): void {
        this._info(msg);
    }

    warn(msg: string): void {
        this._warn(msg);
    }

    error(msg: string, err?: Error): void {
        const trace = err?.stack ?? 'N/A';

        this._error(`${msg} Trace: ${trace}`);
    }
}
