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

document.addEventListener("DOMContentLoaded", () => {
    const elems = document.getElementsByClassName("pref");
    browser.storage.local.get(null).then((prefs) => {
        for (const e of elems) {
            const p = prefs[e.id] as string;
            if (p && (e instanceof HTMLInputElement || e instanceof HTMLSelectElement)) {
                if (e.type === "checkbox") {
                    e.checked = p === "true";
                } else {
                    e.value = p;
                }
            }
        }
    });

    document.getElementById("save")?.addEventListener("click", () => {
        const p: Record<string, string> = {};
        for (const e of elems) {
            if (e instanceof HTMLInputElement || e instanceof HTMLSelectElement) {
                if (e.type === "checkbox") {
                    p[e.id] = e.checked ? "true" : "false";
                } else {
                    p[e.id] = e.value;
                }
            }
        }
        browser.storage.local.set(p);
    });
});
