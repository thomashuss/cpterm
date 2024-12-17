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

import { FROM_CPTERM_SCRAPER, TO_CPTERM_SCRAPER } from "./const";
import { COMMAND, Command, KEEP_ALIVE } from "../common/command";
import { Message } from "../common/message";
import { SET_CODE, SetCode } from "../common/set-code";
import { TestCase, TestResults } from "../common/test-results";
import { NewProblem } from "../common/new-problem";

const M_KEEP_ALIVE = JSON.stringify(new Command(KEEP_ALIVE));
const RUN_TEST = "run";
const SUBMIT_CODE = "submit";

type OptionalUndefHElement = HTMLElement | undefined | null;
type OptionalHElement = HTMLElement | null;

/**
 * Complain about the DOM structure if t is null.
 * @param t checked
 */
function assertDomStructure<T>(t: T | null | undefined): asserts t is T {
    if (t == null) throw new Error("Unexpected DOM structure");
}

/**
 * Return the attr function evaluated with the first element matching the class name,
 * or an empty string if no match.
 * @param className class name to match
 * @param attr gets the desired attribute
 * @returns value of attr
 */
function firstAttrByClassName(className: string, attr: (e: Element) => string | null): string {
    const l = document.getElementsByClassName(className);
    if (l.length > 0) {
        return attr(l[0]) ?? "";
    } else {
        return "";
    }
}

/**
 * Find the first visible sibling of an element.
 * @param elem starting element
 * @returns first visible sibling of elem, or null if one couldn't be found
 */
function firstVisibleSibling(elem: HTMLElement): OptionalHElement {
    let ret = elem.nextElementSibling as OptionalHElement;
    while (ret != null && ret.offsetParent == null) {
        ret = ret.nextElementSibling as OptionalHElement;
    }
    return ret;
}

/**
 * Watch the specified element for changes until a callback function returns non-null.  If the callback returns non-null
 * before starting to watch the element, the promise resolves immediately.
 * @param elem element to watch
 * @param cb resolves promise with the non-null return value of this function
 * @param options passed to MutationObserver
 * @param initCb called immediately after listening starts
 * @param timeout wait no longer than this time in milliseconds to resolve promise; rejects on timeout (default 60000)
 * @returns promise
 */
async function waitForElement<T>(elem: Node, cb: () => T | null, options?: MutationObserverInit,
    initCb?: (() => void) | null, timeout?: number): Promise<T> {
    return new Promise<T>((resolve, reject) => {
        const c = cb();
        if (c == null) {
            const t = setTimeout(() => { observer.disconnect(); reject("Timed out"); }, timeout ?? 60000);
            const observer = new MutationObserver(() => {
                const c = cb();
                if (c != null) {
                    clearTimeout(t);
                    observer.disconnect();
                    resolve(c);
                }
            });
            observer.observe(elem, options);
        } else {
            resolve(c);
        }
        if (initCb != null) initCb();
    });
}

/**
 * Watch the specified element for changes until a condition is met.  If the condition returns true
 * before starting to watch the element, the promise resolves immediately.
 * @param elem element to watch
 * @param condition promise is not resolved until this returns true
 * @param options passed to MutationObserver
 * @param initCb called immediately after listening starts
 * @param timeout wait no longer than this time in milliseconds to resolve promise; rejects on timeout (default 60000)
 * @returns promise
 */
async function watchElement(elem: Node, condition: () => boolean, options?: MutationObserverInit,
    initCb?: (() => void) | null, timeout?: number): Promise<void> {
    await waitForElement(elem, () => condition() ? true : null, options, initCb, timeout);
}

/**
 * Get and set contents of the problem.
 */
interface Scraper {
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

/**
 * A single monaco editor.
 */
interface Monaco {
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
abstract class HasMonaco implements Scraper {
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

class HackerRank extends HasMonaco {
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

    private static async getTestCases(btnClass: string): Promise<Record<string, TestCase>> {
        await watchElement(document, () => document.getElementsByClassName("testcases-result-wrapper").length === 0,
            { childList: true, subtree: true }, () => (document.getElementsByClassName(btnClass)[0] as HTMLElement).click());
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
                const stdout = (content.querySelector(".stdout .lines-container") as OptionalHElement)?.innerText
                    ?? tab.querySelector("svg[aria-label='Failed']") != null ? "" : expected;
                const stderr = (content.querySelector(".stderr .lines-container") as OptionalHElement)?.innerText;
                ret[(tab as HTMLElement).innerText] = new TestCase(stdin, stdout, expected, stderr);
            }
            return ret;
        }
    }

    async runTestCases(): Promise<Record<string, TestCase>> {
        return HackerRank.getTestCases("hr-monaco-compile");
    }

    async runSubmitTestCases(): Promise<Record<string, TestCase>> {
        return HackerRank.getTestCases("hr-monaco-submit");
    }
}

class LeetCode extends HasMonaco {
    isProblem(): boolean {
        return location.pathname.match(/\/problems\/.+\//) != null
            && (document.querySelector("div[data-track-load='description_content']") as HTMLElement | undefined)?.offsetParent != null;
    }

    getName(): string {
        return location.pathname.match(/problems\/([^/]+)/)?.at(1) ?? "";
    }

    getProblem(): string {
        return document.querySelector("div[data-track-load='description_content']")?.outerHTML ?? "";
    }

    getLanguage(): string {
        return (document.querySelector("#editor button:has(div svg[data-icon*='down'])") as HTMLElement | undefined)?.innerText ?? "";
    }

    async runTestCases(): Promise<Record<string, TestCase>> {
        const results = document.querySelector("div[data-layout-path='/c1/ts1/t1']") as OptionalHElement;  // "Test Result" tab content
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
        if (consoleResult.classList.contains("text-red-s")) {  // compile/runtime error
            return {
                "0": new TestCase(input?.innerText ?? "", "", "",
                    (results.querySelector(".whitespace-pre-wrap") as OptionalHElement)?.innerText ?? "")
            };
        } else {
            const ret: Record<string, TestCase> = {};
            if (input != null && output != null && expected != null) {
                for (const tab of <NodeListOf<HTMLDivElement>>results.querySelectorAll("div.cursor-pointer")) {
                    if (tab.innerText !== "") {  // button is a test case tab
                        if (!tab.classList.contains("bg-fill-3")) {  // tab is not presently selected
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
        await watchElement(container, () => (subDetailBtn = document.getElementById("submission-detail_tab")) != null,
            { childList: true, subtree: true }, () => submitBtn.click());
        // don't reuse old value in case path changed
        const resultsPath = LeetCode.findResultsPath(subDetailBtn!);

        let input: OptionalUndefHElement, output: OptionalUndefHElement, expected: OptionalUndefHElement;
        const results = await waitForElement(container, () =>
            container.querySelector(`div[data-layout-path='${resultsPath}']`) as OptionalHElement,
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
            return {
                "0": new TestCase(input?.innerText ?? "", "", "",
                    (results.querySelector(".whitespace-pre-wrap") as OptionalHElement)?.innerText ?? "")
            };
        } else {
            return {};
        }
    }
}

/**
 * Determine the best scraper to use on this page.
 * @returns scraper or null if no scraper will work
 */
function getScraper(): Scraper | null {
    if (location.hostname.indexOf("hackerrank.com") != -1) {
        return new HackerRank();
    } else if (location.hostname.indexOf("leetcode.com") != -1) {
        return new LeetCode();
    }
    return null;
}

/**
 * Send a message to content->background.
 * @param m 
 */
function sendMessage(m: Message) {
    document.dispatchEvent(new CustomEvent(FROM_CPTERM_SCRAPER, {
        detail: JSON.stringify(m)
    }));
}

/**
 * Add promise handlers for the test case submission.
 * @param p submission promise
 */
function handleTestCase(p: Promise<Record<string, TestCase> | string>) {
    p.then((c) => sendMessage(typeof c === "string" ? new TestResults(null, c) : new TestResults(c, null)))
        .catch((err) => sendMessage(new TestResults(null, JSON.stringify(err))));
}

/**
 * Register the listener for background script messages.
 * @param scraper used to set code on change
 */
function registerBackgroundListener(scraper: Scraper) {
    document.addEventListener(TO_CPTERM_SCRAPER, (e) => {
        if (e instanceof CustomEvent) {
            const message = JSON.parse(e.detail) as Message;
            if (message.type === SET_CODE) {
                scraper.setCode((message as SetCode).code);
            } else if (message.type === COMMAND) {
                if ((message as Command).command == RUN_TEST) {
                    handleTestCase(scraper.runTestCases());
                } else if ((message as Command).command === SUBMIT_CODE) {
                    handleTestCase(scraper.runSubmitTestCases());
                }
            }
        }
    });
}

/**
 * Send the current problem to the content script.
 * @param scraper used to get the problem
 * @returns true if the problem was sent
 */
function sendProblem(scraper: Scraper): boolean {
    const p = scraper.getProblem(), c = scraper.getCode(), l = scraper.getLanguage();
    // check if the page is ready
    if (p.length > 0 && c.length > 0) {
        sendMessage(new NewProblem(p, c, l, location.href, scraper.getName()));
        return true;
    }
    return false;
}

/**
 * Waiting for problem?
 */
let waiting = false;
/**
 * Call {@code sendProblem} when appropriate.
 * @param scraper used to get the problem
 */
async function sendProblemWhenReady(scraper: Scraper) {
    if (!waiting && (!scraper.isProblem() || !sendProblem(scraper))) {
        waiting = true;
        await watchElement(document.body, () => scraper.isProblem() && sendProblem(scraper),
            { childList: true, subtree: true });
        waiting = false;
    }
}

const scraper = getScraper();
if (scraper != null) {
    // keep nm host alive in case the kill timer is set
    document.dispatchEvent(new CustomEvent(FROM_CPTERM_SCRAPER, { detail: M_KEEP_ALIVE }));
    registerBackgroundListener(scraper);

    const button = document.createElement("button");
    button.style.position = "fixed";
    button.style.left = button.style.top = "0px";
    button.style.zIndex = "1000";
    button.innerText = "Open problem";
    button.addEventListener("click", () => sendProblemWhenReady(scraper));
    const shadowElem = document.createElement("div");
    document.body.appendChild(shadowElem);
    shadowElem.attachShadow({ mode: "open" }).appendChild(button);

    document.addEventListener("keyup", (e) => {
        if (e.altKey && e.shiftKey && e.key === "C") {
            sendProblemWhenReady(scraper);
        }
    });
}
