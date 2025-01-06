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

import { Message } from "../message/message";
import { Command, COMMAND, KEEP_ALIVE } from "../message/command";
import { ERROR, LogEntry } from "../message/log-entry";
import browser from "webextension-polyfill";

const NATIVE_NAME = "io.github.thomashuss.cpterm";
const VERSION = "version";
const HOST_VERSION = "1.0-SNAPSHOT";

interface Version extends Message {
    readonly hostVersion: string;
}

/**
 * Abstracts away the maintenance of a native messaging connection.
 */
export class HostInterface {
    private nativePort: browser.Runtime.Port | null;
    private quitTimeout: ReturnType<typeof setTimeout> | null;
    private csPorts: Set<browser.Runtime.Port>;

    constructor() {
        this.nativePort = null;
        this.quitTimeout = null;
        this.csPorts = new Set<browser.Runtime.Port>();
    }

    /**
     * Post the object as a message to all open CS ports.
     * @param message to post
     */
    private postToCS(message: unknown) {
        this.csPorts.forEach((o) => o.postMessage(message));
    }

    /**
     * Event handler for native port disconnect.
     * @param nativePort disconnected native port
     */
    private onNativePortDisconnect(nativePort: browser.Runtime.Port) {
        if (this.nativePort == nativePort) {
            this.nativePort = null;
        }
        if (nativePort.error) {
            this.postToCS(new LogEntry(ERROR, nativePort.error.message));
        }
    }

    /**
     * Event handler for a browser CS port disconnect.
     * @param cs disconnected CS port
     */
    private onCSDisconnect(cs: browser.Runtime.Port) {
        this.csPorts.delete(cs);
        if (this.csPorts.size == 0) {
            this.end();
        }
    }

    /**
     * If the port is not open, open it.
     * @param cs browser port from which the request originated; used for registering callbacks
     * @returns non-null port
     */
    private async ensurePort(cs: browser.Runtime.Port): Promise<browser.Runtime.Port> {
        if (!this.csPorts.has(cs)) {
            this.csPorts.add(cs);
            cs.onDisconnect.addListener(this.onCSDisconnect.bind(this));
        }

        if (this.nativePort == null) {
            const nativePort = browser.runtime.connectNative(NATIVE_NAME);
            await new Promise<void>((resolve, reject) => {
                const versionCheck = (m: unknown) => {
                    if ((m as Message).type === VERSION) {
                        if ((m as Version).hostVersion === HOST_VERSION) {
                            nativePort.onMessage.removeListener(versionCheck);
                            resolve();
                        } else {
                            cs.postMessage(new LogEntry(ERROR, "Host version and extension version are incompatible.  Update both the host and extension."));
                            nativePort.disconnect();
                            reject();
                        }
                    }
                };
                nativePort.onMessage.addListener(versionCheck);
            });
            nativePort.onDisconnect.addListener(this.onNativePortDisconnect.bind(this));
            nativePort.onMessage.addListener(this.postToCS.bind(this));
            const prefs = await browser.storage.local.get(null);
            if (Object.keys(prefs).length != 0) {
                nativePort.postMessage({ type: "setPrefs", prefs: prefs });
            }
            this.nativePort = nativePort;
        }

        return this.nativePort;
    }

    public updatePrefs(changes: browser.Storage.StorageAreaOnChangedChangesType) {
        if (this.nativePort != null) {
            const p: Record<string, string> = {};
            for (const change of Object.keys(changes)) {
                const newValue = changes[change].newValue;
                if (newValue != null) {
                    p[change] = <string>newValue;
                }
            }
            this.nativePort.postMessage({ type: "setPrefs", prefs: p });
        }
    }

    /**
     * Post a message to the native messaging host, starting the host if it's not running.
     * @param cs browser port from which the request originated; used for registering callbacks
     * @param message message
     */
    public postMessage(cs: browser.Runtime.Port, message: Message) {
        if (!(this.unsetQuitTimeout() && message.type === COMMAND && (message as Command).command === KEEP_ALIVE)) {
            this.ensurePort(cs).then((p) => p.postMessage(message));
        }
    }

    /**
     * Stop the timer to end the host process, if the timer is set.
     * @returns true if the timer was running and stopped
     */
    private unsetQuitTimeout(): boolean {
        if (this.quitTimeout != null) {
            clearTimeout(this.quitTimeout);
            return true;
        }
        return false;
    }

    /**
     * Start a timer to end the host process.
     */
    private setQuitTimeout() {
        this.quitTimeout = setTimeout(() => {
            try {
                this.nativePort?.disconnect();
            } finally {
                this.nativePort = null;
            }
        }, 10000);
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