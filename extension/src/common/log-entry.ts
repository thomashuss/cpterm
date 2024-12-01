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

import { Message } from "./message";

export const LOG_ENTRY = "logEntry";
export const ERROR = "error";

export class LogEntry implements Message {
    readonly type = LOG_ENTRY;
    readonly messageType: string = "";
    readonly message: string = "";

    constructor(messageType: string, message: string) {
        this.messageType = messageType;
        this.message = message;
    }
}
