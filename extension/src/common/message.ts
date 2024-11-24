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
export const QUIT = "quit";
export const KEEP_ALIVE = "keepAlive";
export const LOG_ENTRY = "logEntry";
export const NEW_PROBLEM = "newProblem";
export const SET_CODE = "setCode";
export const SET_PREFS = "setPrefs";

export abstract class Message {
    readonly type: string;

    constructor(type: string) {
        this.type = type;
    }
}

export class Command extends Message {
    readonly command: string;

    constructor(command: string) {
        super(COMMAND);
        this.command = command;
    }
}

export class LogEntry extends Message {
    readonly messageType: string = "";
    readonly message: string = "";

    constructor(messageType: string, message: string) {
        super(LOG_ENTRY);
        this.messageType = messageType;
        this.message = message;
    }
}

export class NewProblem extends Message {
    readonly problem: string;
    readonly code: string;
    readonly language: string;
    readonly url: string;

    constructor(problem: string, code: string, language: string, url: string) {
        super(NEW_PROBLEM);
        this.problem = problem;
        this.code = code;
        this.language = language;
        this.url = url;
    }
}

export class SetCode extends Message {
    readonly code: string = "";
}

export class SetPrefs extends Message {
    readonly prefs: object;

    constructor(prefs: object) {
        super(SET_PREFS);
        this.prefs = prefs;
    }
}