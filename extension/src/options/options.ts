import browser from "webextension-polyfill";

document.addEventListener("DOMContentLoaded", () => {
    const elems = document.getElementsByClassName("pref");
    browser.storage.local.get(null).then((prefs) => {
        for (const e of elems) {
            const p = prefs[e.id] as string;
            if (p && (e instanceof HTMLInputElement || e instanceof HTMLSelectElement)) {
                if (e.type === "checkbox") {
                    e.checked = p === "true";
                } else {
                    e.value = p;
                }
            }
        }
    });

    document.getElementById("save")?.addEventListener("click", () => {
        const p: Record<string, string> = {};
        for (const e of elems) {
            if (e instanceof HTMLInputElement || e instanceof HTMLSelectElement) {
                if (e.type === "checkbox") {
                    p[e.id] = e.checked ? "true" : "false";
                } else {
                    p[e.id] = e.value;
                }
            }
        }
        browser.storage.local.set(p);
    });
});
