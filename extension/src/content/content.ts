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

import browser from "webextension-polyfill";
import { FROM_CPTERM_SCRAPER, TO_CPTERM_SCRAPER } from ".//const";
import { Message } from "../message/message";
import { ERROR, LOG_ENTRY, LogEntry } from "../message/log-entry";

/**
 * A closure which guarantees a background script connection.
 */
const ensureBackground = (function() {
    let background: browser.Runtime.Port | null = null;
    return () => {
        if (background == null) {
            background = browser.runtime.connect({ name: "cpterm-cs-bg" });
            background.onMessage.addListener((u) => {
                if ((u as Message).type === LOG_ENTRY) {
                    // log entries could be from the nm host or bg script
                    const le = u as LogEntry;
                    if (le.messageType === ERROR) {
                        alert(le.message);
                    } else {
                        console.log(le.message);
                    }
                } else {
                    document.dispatchEvent(new CustomEvent(TO_CPTERM_SCRAPER, { detail: JSON.stringify(u) }));
                }
            });
            background.onDisconnect.addListener(() => background = null);
        }
        return background;
    };
})();

const script = document.createElement("script");
script.src = browser.runtime.getURL("scraper-inject.js");
(document.head ?? document.documentElement).appendChild(script);
script.onload = () => {
    document.addEventListener(FROM_CPTERM_SCRAPER, (e) => {
        if (e instanceof CustomEvent) {
            // everything from the scraper can go to the bg script
            ensureBackground().postMessage(JSON.parse(e.detail));
        }
    });
};
