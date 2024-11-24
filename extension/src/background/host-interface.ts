/*
 *  Copyright (C) 2024 Thomas Huss
 *
 *  CPTerm is free software: you can redistribute it and/or modify it under
 *  the terms of the GNU General Public License as published by the Free Software
 *  Foundation, either version 3 of the License, or (at your option) any later
 *  version.
 *
 *  CPTerm is distributed in the hope that it will be useful, but WITHOUT ANY
 *  WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 *  PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with
 *  this program. If not, see https://www.gnu.org/licenses/.
 */

import { Command, LogEntry, Message, QUIT as M_QUIT, SetPrefs, COMMAND, KEEP_ALIVE } from "../common/message";
import browser from "webextension-polyfill";

const NATIVE_NAME = "cpterm_host";
const QUIT = new Command(M_QUIT);
const PREFS = new SetPrefs({});

/**
 * Abstracts away the maintenance of a native messaging connection.
 */
export class HostInterface {
    private nativePort: browser.Runtime.Port | null;
    private quitTimeout: any | null;
    private origins: Set<browser.Runtime.Port>;

    constructor() {
        this.nativePort = null;
        this.quitTimeout = null;
        this.origins = new Set<browser.Runtime.Port>();
    }

    private static getPrefs(): SetPrefs {
        return PREFS;
    }

    private postToOrigin(message: any) {
        this.origins.forEach((o) => o.postMessage(message));
    }

    private onNativePortDisconnect(nativePort: browser.Runtime.Port) {
        if (this.nativePort == nativePort) {
            this.nativePort = null;
        }
        if (nativePort.error) {
            this.postToOrigin(new LogEntry("error", nativePort.error.message));
        }
    }

    private onOriginDisconnect(origin: browser.Runtime.Port) {
        this.origins.delete(origin);
        if (this.origins.size == 0) {
            this.end();
        }
    }

    /**
     * If the port is not open, open it.
     * 
     * @param origin browser port from which the request originated; used for registering callbacks
     * @returns non-null port
     */
    private ensurePort(origin: browser.Runtime.Port): browser.Runtime.Port {
        if (!this.origins.has(origin)) {
            this.origins.add(origin);
            origin.onDisconnect.addListener(this.onOriginDisconnect.bind(this));
        }

        if (this.nativePort == null) {
            this.nativePort = browser.runtime.connectNative(NATIVE_NAME);
            this.nativePort.onDisconnect.addListener(this.onNativePortDisconnect.bind(this));
            this.nativePort.onMessage.addListener(this.postToOrigin.bind(this));
            this.nativePort.postMessage(HostInterface.getPrefs());
        }

        return this.nativePort;
    }

    /**
     * Post a message to the native messaging host, starting the host if it's not running.
     * 
     * @param origin browser port from which the request originated; used for registering callbacks
     * @param message message
     */
    public postMessage(origin: browser.Runtime.Port, message: Message) {
        if (this.quitTimeout != null) {
            clearTimeout(this.quitTimeout);
            if (message.type === COMMAND && (message as Command).command === KEEP_ALIVE) {
                this.setQuitTimeout();
                return;
            }
        }
        this.ensurePort(origin).postMessage(message);
    }

    private setQuitTimeout() {
        this.quitTimeout = setTimeout(() => {
            this.nativePort?.postMessage(QUIT);
            this.nativePort = null;
        }, 60000);
    }

    /**
     * Instruct the native messaging process to stop soon, if it's running.
     */
    public end() {
        if (this.nativePort != null) {
            this.setQuitTimeout();
        }
    }
}