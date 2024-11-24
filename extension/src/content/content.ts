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
import { LOG_ENTRY, LogEntry, Message, SET_CODE } from "../common/message";
import { FROM_CPTERM_SCRAPER, TO_CPTERM_SCRAPER } from "./const";

const ensureBackground = (function() {
    let background: browser.Runtime.Port | null = null;
    return () => {
        if (background == null) {
            background = browser.runtime.connect({ name: "cpterm-cs-bg" });
            background.onMessage.addListener((u) => {
                if ((u as Message).type === LOG_ENTRY) {
                    let le = u as LogEntry;
                    if (le.messageType === "error") {
                        alert(le.message);
                    } else {
                        console.log(le.message);
                    }
                }
            });
            background.onMessage.addListener((u) => {
                if ((u as Message).type === SET_CODE) {
                    document.dispatchEvent(new CustomEvent(TO_CPTERM_SCRAPER, { detail: JSON.stringify(u) }));
                }
            });
            background.onDisconnect.addListener(() => {
                background = null;
            });
        }
        return background;
    };
})();

function inject() {
    let script = document.createElement("script");
    script.src = browser.runtime.getURL("scraper.js");
    (document.head || document.documentElement).appendChild(script);
    script.onload = () => {
        document.addEventListener(FROM_CPTERM_SCRAPER, (e) => {
            if (e instanceof CustomEvent) {
                ensureBackground().postMessage(JSON.parse(e.detail));
            }
        });
    };
}

inject();
