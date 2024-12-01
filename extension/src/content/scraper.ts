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

import { Command, KEEP_ALIVE, Message, NewProblem, SET_CODE, SetCode } from "../common/message";
import { FROM_CPTERM_SCRAPER, TO_CPTERM_SCRAPER } from "./const";

const M_KEEP_ALIVE = JSON.stringify(new Command(KEEP_ALIVE));

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
        return attr(l[0]) || "";
    } else {
        return "";
    }
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
}

/**
 * A single monaco editor.
 */
interface Monaco {
    getValue(): string;
    setValue(value: string): void;
}

/**
 * Extension of the window object which has the global monaco.
 */
interface MonacoWindow {
    monaco: {
        editor: {
            getModels(): Array<Monaco>;
        };
    } | undefined;
}

/**
 * A scraper for a website which uses the monaco editor.
 */
abstract class HasMonaco implements Scraper {
    /**
     * Get the monaco reference from the page.
     * @returns monaco reference or null if global monaco object not present
     */
    private ensureMonaco(): Monaco | undefined {
        return (window as unknown as MonacoWindow).monaco?.editor.getModels()[0];
    }

    public getCode(): string {
        return this.ensureMonaco()?.getValue() || "";
    }

    public setCode(code: string): void {
        this.ensureMonaco()!.setValue(code);
    }

    abstract isProblem(): boolean;
    abstract getProblem(): string;
    abstract getLanguage(): string;
}

class HackerRank extends HasMonaco {
    isProblem(): boolean {
        return location.pathname.endsWith("problem");
    }

    getProblem(): string {
        return firstAttrByClassName("challenge-body-html", e => e.outerHTML);
    }

    getLanguage(): string {
        return firstAttrByClassName("select-language", e => (e as HTMLElement).innerText);
    }
}

class LeetCode extends HasMonaco {
    isProblem(): boolean {
        return location.pathname.match(/\/problems\/.+\/description/) != null;
    }

    getProblem(): string {
        return document.querySelector("div[data-track-load='description_content']")?.outerHTML || "";
    }

    getLanguage(): string {
        return (document.querySelector("#editor button:has(div svg[data-icon*='down'])") as HTMLElement)?.innerText || "";
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
 * Register the listener for background script messages.
 * @param scraper used to set code on change
 */
function registerBackgroundListener(scraper: Scraper) {
    document.addEventListener(TO_CPTERM_SCRAPER, (e) => {
        if (e instanceof CustomEvent) {
            const message = JSON.parse(e.detail) as Message;
            if (message.type === SET_CODE) {
                scraper.setCode((message as SetCode).code);
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
        document.dispatchEvent(new CustomEvent(FROM_CPTERM_SCRAPER, {
            detail: JSON.stringify(new NewProblem(p, c, l, location.href))
        }));
        return true;
    }
    return false;
}

let waiting = false;
/**
 * Call {@code sendProblem} when appropriate.
 * @param scraper used to get the problem
 */
function sendProblemWhenReady(scraper: Scraper) {
    if (!waiting && (!scraper.isProblem() || !sendProblem(scraper))) {
        waiting = true;
        // watch for changes that may indicate a new problem was opened
        const observer = new MutationObserver(() => {
            if (scraper.isProblem() && sendProblem(scraper)) {
                waiting = false;
                observer.disconnect();
            }
        });
        observer.observe(document.body, { childList: true, subtree: true });
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
