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

/**
 * Output of submitting code.
 */
export class TestResults implements Message {
    readonly type = "testResults";
    readonly cases: Record<string, TestCase> | null;
    readonly error: string | null;

    constructor(cases: Record<string, TestCase> | null, error: string | null) {
        this.cases = cases;
        this.error = error;
    }
}

/**
 * A single test case and its result.
 */
export class TestCase {
    readonly input: string;
    readonly output: string | null;
    readonly expected: string | null;
    readonly error: string | null;

    constructor(input: string, output?: string, expected?: string, error?: string) {
        this.input = input.trim();
        this.output = output?.trim() ?? null;
        this.expected = expected?.trim() ?? null;
        this.error = error?.trim() ?? null;
    }
}
