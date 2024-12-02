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
import { Message } from "../common/message";
import { HostInterface } from "./host-interface";

const host = new HostInterface();

browser.storage.local.onChanged.addListener((c) => host.updatePrefs(c));
browser.runtime.onConnect.addListener((contentPort) =>
    contentPort.onMessage.addListener((u) =>
        host.postMessage(contentPort, u as Message)));
