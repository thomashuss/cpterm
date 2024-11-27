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

export const COMMAND = "command";
export const KEEP_ALIVE = "keepAlive";
export const LOG_ENTRY = "logEntry";
export const NEW_PROBLEM = "newProblem";
export const SET_CODE = "setCode";

export interface Message {
    readonly type: string;
}

export class Command implements Message {
    readonly type = COMMAND;
    readonly command: string;

    constructor(command: string) {
        this.command = command;
    }
}

export class LogEntry implements Message {
    readonly type = LOG_ENTRY;
    readonly messageType: string = "";
    readonly message: string = "";

    constructor(messageType: string, message: string) {
        this.messageType = messageType;
        this.message = message;
    }
}

export class NewProblem implements Message {
    readonly type = NEW_PROBLEM;
    readonly problem: string;
    readonly code: string;
    readonly language: string;
    readonly url: string;

    constructor(problem: string, code: string, language: string, url: string) {
        this.problem = problem;
        this.code = code;
        this.language = language;
        this.url = url;
    }
}

export interface SetCode extends Message {
    readonly code: string;
}
