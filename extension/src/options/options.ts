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

import browser from "webextension-polyfill";

function validate() {
    const errors: string[] = [];

    function validateCondition(condition: boolean, errorMessage: string) {
        if (!condition) {
            errors.push(errorMessage);
        }
    }

    const problemFileSuffix = (document.getElementById("problem_file_suffix") as HTMLInputElement | null)?.value;
    const selectedConverter = (document.getElementById("problem_converter") as HTMLSelectElement | null)?.value;
    validateCondition(
        selectedConverter !== "open_html_to_pdf" || problemFileSuffix === ".pdf",
        "Problem file extension must be `.pdf' when using Open HTML to PDF."
    );
    validateCondition(
        selectedConverter !== "raw_html" || problemFileSuffix === ".html",
        "Problem file extension must be `.html' when using raw HTML."
    );
    validateCondition(
        selectedConverter !== "pandoc"
        || (document.getElementById("pandoc_path") as HTMLInputElement | null)?.value.trim() !== "",
        "Pandoc path must be specified when using Pandoc."
    );
    validateCondition(
        selectedConverter !== "libreoffice"
        || (document.getElementById("libreoffice_path") as HTMLInputElement | null)?.value.trim() !== "",
        "LibreOffice path must be specified when using LibreOffice."
    );

    if (errors.length > 0) {
        alert("Correct the following errors:\n" + errors.join("\n"));
        return false;
    }
    return true;
}

document.addEventListener("DOMContentLoaded", () => {
    const prefElems = document.getElementsByClassName("pref");
    const saveBtn = document.getElementById("save") as HTMLButtonElement | null;
    const useDir = document.getElementById("useDir") as HTMLInputElement | null;
    const useTempFiles = document.getElementById("useTempFiles") as HTMLInputElement | null;
    const dirPath = document.getElementById("dirPath") as HTMLInputElement | null;

    if (prefElems.length > 0 && saveBtn != null && dirPath != null && useTempFiles != null && useDir != null) {
        useTempFiles.addEventListener("change", () => {
            if (useTempFiles.checked) {
                saveBtn.disabled = false;
                dirPath.disabled = true;
            }
        });

        useDir.addEventListener("change", () => {
            if (useDir.checked) {
                saveBtn.disabled = false;
                dirPath.disabled = false;
            }
        });

        dirPath.addEventListener("change", () => saveBtn.disabled = false);

        browser.storage.local.get(null).then((prefs) => {
            dirPath.value = prefs["code_file_path"] as string | undefined ?? "";
            if (prefs["write_code_to_temp_file"] === "true" && prefs["write_problem_to_temp_file"] === "true") {
                useTempFiles.checked = true;
                dirPath.disabled = true;
            } else if (prefs["write_code_to_temp_file"] === "false" && prefs["write_problem_to_temp_file"] === "false"
                && prefs["create_dir_for_problem"] === "true" && prefs["problem_file_path"] === prefs["code_file_path"]
                && typeof prefs["code_file_path"] === "string") {
                useDir.checked = true;
                dirPath.disabled = false;
            }
            for (const e of prefElems) {
                const p = prefs[e.id];
                if (typeof p === "string" && (e instanceof HTMLInputElement || e instanceof HTMLSelectElement)) {
                    if (e.type === "checkbox") {
                        e.checked = p === "true";
                    } else {
                        e.value = p;
                    }
                }
                e.addEventListener("change", () => saveBtn.disabled = false);
            }
        });

        saveBtn.addEventListener("click", () => {
            if (validate()) {
                const p: Record<string, string> = {};

                if (useTempFiles.checked) {
                    p["write_code_to_temp_file"] = p["write_problem_to_temp_file"] = "true";
                } else if (useDir.checked) {
                    const dirPathValue = dirPath.value;
                    if (dirPathValue == null || dirPathValue.length === 0) {
                        alert("Set the problem directory.");
                        return;
                    } else {
                        p["write_code_to_temp_file"] = p["write_problem_to_temp_file"] = "false";
                        p["create_dir_for_problem"] = "true";
                        p["problem_file_path"] = p["code_file_path"] = dirPathValue;
                    }
                }

                for (const e of prefElems) {
                    if (e instanceof HTMLInputElement || e instanceof HTMLSelectElement) {
                        if (e.type === "checkbox") {
                            p[e.id] = e.checked ? "true" : "false";
                        } else {
                            p[e.id] = e.value;
                        }
                    }
                }
                browser.storage.local.set(p);
                saveBtn.disabled = true;
            }
        });
    }
});
