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
import { HasMonaco } from "./monaco";
import { firstAttrByClassName, watchElement, waitForElement, OptionalHElement, firstVisibleSibling, assertDomStructure } from "./util";

export class HackerRank extends HasMonaco {
    isProblem(): boolean {
        return location.pathname.endsWith("problem");
    }

    getName(): string {
        return location.pathname.match(/challenges\/([^/]+)/)?.at(1) ?? "";
    }

    getProblem(): string {
        return firstAttrByClassName("challenge-body-html", e => e.outerHTML);
    }

    getLanguage(): string {
        return firstAttrByClassName("select-language", e => (e as HTMLElement).innerText);
    }

    private static async getTestCases(btnQuery: string): Promise<Record<string, TestCase>> {
        await watchElement(document, () => document.getElementsByClassName("testcases-result-wrapper").length === 0,
            { childList: true, subtree: true }, () => (document.querySelector(btnQuery) as OptionalHElement)?.click());
        let compileError = false;
        const wrapper = await waitForElement(document,
            () => {
                const wrapper = document.querySelector(".testcases-result-wrapper");
                if (wrapper == null) {
                    const err = document.querySelector(".compile-error-wrapper") as OptionalHElement;
                    if (err != null) {
                        compileError = true;
                        return firstVisibleSibling(err);
                    }
                }
                return wrapper;
            }, { childList: true, subtree: true });
        if (compileError) {
            return {
                "0": new TestCase("", "", "",
                    (wrapper.querySelector("pre.compile-message") as OptionalHElement)?.innerText ?? "")
            };
        } else {
            const ret: Record<string, TestCase> = {};
            const content = wrapper.querySelector(".tab-content");
            assertDomStructure(content);
            const tabs = wrapper.getElementsByClassName("tab-item");
            for (const tab of tabs) {
                await watchElement(content, () => content.getAttribute("aria-labelledby") === tab.id
                    && (content.getElementsByClassName("unlock-wrapper").length > 0
                        || content.getElementsByClassName("lines-container").length > 0),
                    { childList: true, subtree: true, attributes: true, attributeFilter: ["aria-labelledby"] },
                    () => (tab as HTMLElement).click());
                const stdin = (content.querySelector(".stdin .lines-container") as OptionalHElement)?.innerText ?? "";
                const expected = (content.querySelector(".expected-output .lines-container") as OptionalHElement)?.innerText;
                const stdout = ((content.querySelector(".stdout .lines-container") as OptionalHElement)?.innerText
                    ?? tab.querySelector("svg[aria-label='Failed']") != null) ? "" : expected;
                const stderr = (content.querySelector(".stderr .lines-container") as OptionalHElement)?.innerText;
                ret[(tab as HTMLElement).innerText] = new TestCase(stdin, stdout, expected, stderr);
            }
            return ret;
        }
    }

    async runTestCases(): Promise<Record<string, TestCase>> {
        return HackerRank.getTestCases(".hr-monaco-compile");
    }

    async runSubmitTestCases(): Promise<Record<string, TestCase>> {
        return HackerRank.getTestCases(".hr-monaco-submit");
    }
}
