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

const NEW_PROBLEM = "newProblem";

export class NewProblem implements Message {
    readonly type = NEW_PROBLEM;
    readonly problem: string;
    readonly code: string;
    readonly language: string;
    readonly url: string;
    readonly name: string;

    constructor(problem: string, code: string, language: string, name: string) {
        this.problem = problem;
        this.code = code.trim();
        this.language = language.trimStart();
        this.url = location.href;
        this.name = name.trim();
    }
}
