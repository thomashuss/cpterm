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

import { TestCase } from "../message/test-results";

/**
 * Get and set contents of the problem.
 */

export interface Scraper {
    /**
     * Whether the current page is a problem page.
     */
    isProblem(): boolean;
    /**
     * Get the problem name.
     */
    getName(): string;
    /**
     * Get the problem code.
     */
    getCode(): string;
    /**
     * Set the problem code.
     * @param code new code
     */
    setCode(code: string): void;
    /**
     * Get the problem statement.
     */
    getProblem(): string;
    /**
     * Get the name of the problem's language.
     */
    getLanguage(): string;
    /**
     * Get the results of the test cases.
     */
    runTestCases(): Promise<Record<string, TestCase>>;
    /**
     * Submit the problem and get the results of the submission test cases.
     */
    runSubmitTestCases(): Promise<Record<string, TestCase>>;
}
