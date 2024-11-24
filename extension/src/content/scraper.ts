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

function firstAttrByClassName(className: string, attr: (e: Element) => string | null): string {
    const l = document.getElementsByClassName(className);
    if (l.length > 0) {
        return attr(l[0]) || "";
    } else {
        return "";
    }
}

interface Scraper {
    isProblem(): boolean;
    getCode(): string;
    setCode(code: string): void;
    getProblem(): string;
    getLanguage(): string;
}

interface Monaco {
    getValue(): string;
    setValue(value: string): void;
}

interface MonacoWindow {
    monaco: {
        editor: {
            getModels(): Array<Monaco>;
        };
    } | undefined;
}

abstract class HasMonaco implements Scraper {
    private monaco: Monaco | null;

    constructor() {
        this.monaco = null;
    }

    private ensureMonaco(): Monaco | null {
        if (this.monaco == null) {
            this.monaco = (window as unknown as MonacoWindow).monaco?.editor.getModels()[0] || null;
        }
        return this.monaco;
    }

    public getCode(): string {
        return this.ensureMonaco()?.getValue() || "";
    }

    public setCode(code: string): void {
        this.ensureMonaco()!.setValue(code);
    }

    public disposeMonaco() {
        this.monaco = null;
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

function getScraper(): Scraper | null {
    if (location.hostname.indexOf("hackerrank.com") != -1) {
        return new HackerRank();
    }
    return null;
}

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

function sendProblem(scraper: Scraper): boolean {
    const p = scraper.getProblem(), c = scraper.getCode(), l = scraper.getLanguage();
    if (p.length > 0 && c.length > 0) {
        document.dispatchEvent(new CustomEvent(FROM_CPTERM_SCRAPER, {
            detail: JSON.stringify(new NewProblem(p, c, l, location.href))
        }));
        return true;
    }
    return false;
}

function init() {
    const scraper = getScraper();
    if (scraper != null) {
        document.dispatchEvent(new CustomEvent(FROM_CPTERM_SCRAPER, { detail: M_KEEP_ALIVE }));
        registerBackgroundListener(scraper);

        let oldPathname = location.pathname;
        let sentProblem = false;
        new MutationObserver(() => {
            if (location.pathname !== oldPathname) {
                oldPathname = location.pathname;
                sentProblem = false;
                if (scraper instanceof HasMonaco) {
                    scraper.disposeMonaco();
                }
            }

            if (!sentProblem && scraper.isProblem() && sendProblem(scraper)) {
                sentProblem = true;
            }
        }).observe(document.body, { childList: true, subtree: true });
    }
}

init();
