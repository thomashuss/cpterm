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
import { Scraper } from "./scraper";

/**
 * A single monaco editor.
 */
export interface Monaco {
    getValue(): string;
    setValue(value: string): void;
}

declare global {
    interface Window {
        monaco?: {
            editor: {
                getModels(): Monaco[];
            };
        };
    }
}

/**
 * A scraper for a website which uses the monaco editor.
 */
export abstract class HasMonaco implements Scraper {
    /**
     * Get the monaco reference from the page.
     * @returns monaco reference or null if global monaco object not present
     */
    private getMonaco(): Monaco | undefined {
        return window.monaco?.editor.getModels()[0];
    }

    public getCode(): string {
        return this.getMonaco()?.getValue() ?? "";
    }

    public setCode(code: string): void {
        this.getMonaco()?.setValue(code);
    }

    abstract isProblem(): boolean;
    abstract getName(): string;
    abstract getProblem(): string;
    abstract getLanguage(): string;
    abstract runTestCases(): Promise<Record<string, TestCase>>;
    abstract runSubmitTestCases(): Promise<Record<string, TestCase>>;
}
