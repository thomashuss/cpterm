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
    validateCondition(
        !((document.getElementById("useDir") as HTMLInputElement | null)?.checked)
        || (document.getElementById("dirPath") as HTMLInputElement | null)?.value.length !== 0,
        "Set the problem directory."
    );

    if (errors.length > 0) {
        alert("Correct the following errors:\n" + errors.join("\n"));
        return false;
    }
    return true;
}

document.addEventListener("DOMContentLoaded", () => {
    /**
     * Elements whose ID is the preference key, and whose value is not transformed.
     */
    const prefElems = <NodeListOf<HTMLInputElement | HTMLSelectElement>>document.querySelectorAll("input.pref, select.pref");
    const saveBtn = document.getElementById("save") as HTMLButtonElement | null;
    /**
     * Checkbox for whether to create a directory for each problem.
     */
    const useDir = document.getElementById("useDir") as HTMLInputElement | null;
    /**
     * Checkbox for whether to use temporary files.
     */
    const useTempFiles = document.getElementById("useTempFiles") as HTMLInputElement | null;
    /**
     * Field for path to parent directory for problem directories.
     */
    const dirPath = document.getElementById("dirPath") as HTMLInputElement | null;
    /**
     * Problem converter drop-down.
     */
    const converter = document.getElementById("problem_converter") as HTMLSelectElement | null;

    if (prefElems.length > 0 && saveBtn != null && dirPath != null && useTempFiles != null && useDir != null && converter != null) {
        document.querySelectorAll("input, select").forEach((e) => e.addEventListener("change", () => saveBtn.disabled = false));

        useTempFiles.addEventListener("change", () => {
            if (useTempFiles.checked) {
                dirPath.disabled = true;
            }
        });

        useDir.addEventListener("change", () => {
            if (useDir.checked) {
                dirPath.disabled = false;
            }
        });

        const converterParamsShow = () => {
            (<NodeListOf<HTMLElement>>document.querySelectorAll(".converterParam")).forEach((e) => e.style.display = "none");
            (<NodeListOf<HTMLElement>>document.querySelectorAll(`.converterParam.${converter.value}`)).forEach((e) => e.style.display = "list-item");
        };
        converter.addEventListener("change", converterParamsShow);

        browser.storage.local.get(null).then((prefs) => {
            const codeFilePath = prefs["code_file_path"];
            if (typeof codeFilePath === "string") {
                dirPath.value = codeFilePath;
            }

            if (prefs["write_code_to_temp_file"] === "true" && prefs["write_problem_to_temp_file"] === "true") {
                useTempFiles.checked = true;
                dirPath.disabled = true;
            } else if (prefs["write_code_to_temp_file"] === "false" && prefs["write_problem_to_temp_file"] === "false"
                && prefs["create_dir_for_problem"] === "true" && prefs["problem_file_path"] === codeFilePath) {
                useDir.checked = true;
                dirPath.disabled = false;
            }

            for (const e of prefElems) {
                const p = prefs[e.id];
                if (typeof p === "string") {
                    if (e.type === "checkbox") {
                        e.checked = p === "true";
                    } else {
                        e.value = p;
                    }
                }
            }
            converterParamsShow();
        });

        saveBtn.addEventListener("click", () => {
            if (validate()) {
                const p: Record<string, string> = {};

                if (useTempFiles.checked) {
                    p["write_code_to_temp_file"] = p["write_problem_to_temp_file"] = "true";
                } else if (useDir.checked) {
                    p["write_code_to_temp_file"] = p["write_problem_to_temp_file"] = "false";
                    p["create_dir_for_problem"] = "true";
                    p["problem_file_path"] = p["code_file_path"] = dirPath.value;
                }

                for (const e of prefElems) {
                    if (e.type === "checkbox") {
                        p[e.id] = e.checked ? "true" : "false";
                    } else {
                        p[e.id] = e.value;
                    }
                }

                browser.storage.local.set(p)
                    .then(() => saveBtn.disabled = true)
                    .catch((e) => alert("Error while saving:\n" + e));
            }
        });
    }
});
