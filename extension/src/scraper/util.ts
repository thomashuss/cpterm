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

export type OptionalUndefHElement = HTMLElement | undefined | null;
export type OptionalHElement = HTMLElement | null;

/**
 * Complain about the DOM structure if t is null.
 * @param t checked
 */
export function assertDomStructure<T>(t: T | null | undefined): asserts t is T {
    if (t == null) throw new Error("Unexpected DOM structure");
}

/**
 * Return the attr function evaluated with the first element matching the class name,
 * or an empty string if no match.
 * @param className class name to match
 * @param attr gets the desired attribute
 * @returns value of attr
 */
export function firstAttrByClassName(className: string, attr: (e: Element) => string | null): string {
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
export function firstVisibleSibling(elem: HTMLElement): OptionalHElement {
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
export async function waitForElement<T>(elem: Node, cb: () => T | null | undefined, options?: MutationObserverInit,
    initCb?: (() => void) | null, timeout?: number): Promise<NonNullable<T>> {
    return new Promise<NonNullable<T>>((resolve, reject) => {
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
export async function watchElement(elem: Node, condition: () => boolean, options?: MutationObserverInit,
    initCb?: (() => void) | null, timeout?: number): Promise<void> {
    await waitForElement(elem, () => condition() ? true : null, options, initCb, timeout);
}
