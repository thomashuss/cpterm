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
import { OptionalHElement, assertDomStructure, watchElement, waitForElement, OptionalUndefHElement, firstVisibleSibling } from "./util";


export class LeetCode extends HasMonaco {
    isProblem(): boolean {
        return location.pathname.match(/\/problems\/.+\//) != null
            && (document.querySelector("div[data-track-load='description_content']") as OptionalHElement)?.offsetParent != null;
    }

    getName(): string {
        return location.pathname.match(/problems\/([^/]+)/)?.at(1) ?? "";
    }

    getProblem(): string {
        return document.querySelector("div[data-track-load='description_content']")?.outerHTML ?? "";
    }

    getLanguage(): string {
        return (document.querySelector("#editor button:has(div svg[data-icon*='down'])") as OptionalHElement)?.innerText ?? "";
    }

    async runTestCases(): Promise<Record<string, TestCase>> {
        const results = document.querySelector("div[data-layout-path='/c1/ts1/t1']") as OptionalHElement; // "Test Result" tab content
        assertDomStructure(results);
        const btn = document.querySelector("button[data-e2e-locator='console-run-button']");
        // wait for prior result to disappear if it's visible
        await watchElement(results, () =>
            // not actually a double negative; will return true if the null check fails
            // (which is desired, since this means the old result was never visible)
            !((results.querySelector("[data-e2e-locator='console-result']") as OptionalHElement)?.offsetParent != null),
            { childList: true, subtree: true, attributes: true, attributeFilter: ["data-e2e-locator"] },
            () => (btn as OptionalHElement)?.click());
        // wait for new result to appear
        const consoleResult = await waitForElement(results, () => {
            const cr = results.querySelector("[data-e2e-locator='console-result']") as OptionalHElement;
            if (cr?.offsetParent != null) {
                return cr;
            }
            return null;
        },
            { childList: true, subtree: true, attributes: true, attributeFilter: ["data-e2e-locator"] });

        let input: OptionalUndefHElement, output: OptionalUndefHElement, expected: OptionalUndefHElement;
        for (const label of <NodeListOf<HTMLDivElement>>results.querySelectorAll("div.text-label-3")) {
            // TODO: maybe make this less locale dependent
            if (label.innerText === "Input") input = firstVisibleSibling(label);
            else if (label.innerText.startsWith("Last Executed Input")) input = firstVisibleSibling(label);
            else if (label.innerText === "Output") output = firstVisibleSibling(label);
            else if (label.innerText === "Expected") expected = firstVisibleSibling(label);
        }
        if (consoleResult.classList.contains("text-red-s") && consoleResult.innerText !== "Wrong Answer") { // compile/runtime error
            const errorBox = results.querySelector(".whitespace-pre-wrap") as OptionalHElement;
            return {
                "0": new TestCase(input?.innerText ?? "", "", "",
                    input?.contains(errorBox) ? consoleResult.innerText : errorBox?.innerText ?? "")
            };
        } else {
            const ret: Record<string, TestCase> = {};
            if (input != null && output != null && expected != null) {
                for (const tab of <NodeListOf<HTMLDivElement>>results.querySelectorAll("div.cursor-pointer")) {
                    if (tab.innerText !== "") { // button is a test case tab
                        if (!tab.classList.contains("bg-fill-3")) { // tab is not presently selected
                            const oldInput = input.innerText;
                            await watchElement(results, () => input.innerText !== oldInput,
                                { childList: true, subtree: true }, () => tab.click());
                        }
                        ret[tab.innerText] = new TestCase(input.innerText, output.innerText, expected.innerText);
                    }
                }
            }
            return ret;
        }
    }

    private static findHasResultsPath(hasResultsPath: HTMLElement): HTMLElement {
        while (!hasResultsPath.hasAttribute("data-layout-path")) {
            assertDomStructure(hasResultsPath.parentElement);
            hasResultsPath = hasResultsPath.parentElement;
        }
        return hasResultsPath;
    }

    private static findResultsPath(hasResultsPath: HTMLElement): string {
        return LeetCode.findHasResultsPath(hasResultsPath).getAttribute("data-layout-path")!.replace(/tb(?=[0-9]+$)/, "t");
    }

    async runSubmitTestCases(): Promise<Record<string, TestCase>> {
        // tab button for submission details
        let subDetailBtn: OptionalHElement = document.getElementById("submission-detail_tab");
        if (subDetailBtn != null) {
            // if tab is already open, close it to evict old results
            const hasResultsPath = LeetCode.findHasResultsPath(subDetailBtn);
            await watchElement(document, () => hasResultsPath.offsetParent == null, { childList: true, subtree: true },
                () => (document.querySelector(
                    `div[data-layout-path='${hasResultsPath.getAttribute("data-layout-path") + "/button/close"}']`
                ) as OptionalHElement)?.click());
        }

        // excludes button header
        const container = document.getElementById("qd-content");
        assertDomStructure(container);
        const submitBtn = <HTMLElement>document.querySelector("button[data-e2e-locator='console-submit-button']");
        assertDomStructure(submitBtn);
        // click submit button and wait for subDetailBtn to become visible
        subDetailBtn = await waitForElement(container, () => document.getElementById("submission-detail_tab"),
            { childList: true, subtree: true }, () => submitBtn.click());
        // don't reuse old value in case path changed
        const resultsPath = LeetCode.findResultsPath(subDetailBtn);

        let input: OptionalUndefHElement, output: OptionalUndefHElement, expected: OptionalUndefHElement;
        const results = await waitForElement(container, () => container.querySelector(`div[data-layout-path='${resultsPath}']`) as OptionalHElement,
            { childList: true, subtree: true });
        let consoleResult: OptionalUndefHElement;

        await watchElement(results, () => {
            for (const label of <NodeListOf<HTMLDivElement>>results.querySelectorAll("div.text-label-3, div.text-text-tertiary")) {
                // TODO: maybe make this less locale dependent
                if (label.innerText === "Input") input = firstVisibleSibling(label);
                else if (label.innerText.startsWith("Last Executed Input") && label.parentElement != null) input = firstVisibleSibling(label.parentElement);
                else if (label.innerText === "Output") output = firstVisibleSibling(label);
                else if (label.innerText === "Expected") expected = firstVisibleSibling(label);
            }
            return (input != null && output != null && expected != null)
                || results.querySelector("span[data-e2e-locator='submission-result']")?.parentElement?.classList.contains("text-green-s")
                || (consoleResult = (results.querySelector("span[data-e2e-locator='console-result']") as OptionalHElement)) != null;
        }, { childList: true, subtree: true });
        if (input != null && output != null && expected != null) {
            return { "0": new TestCase(input.innerText, output.innerText, expected.innerText) };
        } else if (consoleResult?.classList.contains("text-red-s")) {
            const errorBox = results.querySelector(".whitespace-pre-wrap") as OptionalHElement;
            return {
                "0": new TestCase(input?.innerText ?? "", "", "",
                    input?.contains(errorBox) ? consoleResult.innerText : errorBox?.innerText ?? "")
            };
        } else {
            return {};
        }
    }
}
