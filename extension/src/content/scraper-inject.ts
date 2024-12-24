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
import { COMMAND, Command, KEEP_ALIVE } from "../message/command";
import { Message } from "../message/message";
import { SET_CODE, SetCode } from "../message/set-code";
import { TestCase, TestResults } from "../message/test-results";
import { NewProblem } from "../message/new-problem";
import { watchElement } from "../scraper/util";
import { HackerRank } from "../scraper/hackerrank";
import { LeetCode } from "../scraper/leetcode";
import { Scraper } from "../scraper/scraper";

const M_KEEP_ALIVE = JSON.stringify(new Command(KEEP_ALIVE));
const RUN_TEST = "run";
const SUBMIT_CODE = "submit";

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
function handleTestCase(p: Promise<Record<string, TestCase>>) {
    p.then((c) => sendMessage(new TestResults(c, null)))
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
