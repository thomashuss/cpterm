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
        (document.getElementById("write_test_case_to_temp_file") as HTMLInputElement | null)?.checked
        || (document.getElementById("test_case_file_path") as HTMLInputElement | null)?.value.trim() !== "",
        "Test case file path must be specified if not writing test cases to a temporary file."
    );
    validateCondition(
        (document.getElementById("write_code_to_temp_file") as HTMLInputElement | null)?.checked
        || (document.getElementById("code_file_path") as HTMLInputElement | null)?.value.trim() !== "",
        "Code file path must be specified if not writing code to a temporary file."
    );
    validateCondition(
        (document.getElementById("write_problem_to_temp_file") as HTMLInputElement | null)?.checked
        || (document.getElementById("problem_file_path") as HTMLInputElement | null)?.value.trim() !== "",
        "Problem file path must be specified if not writing problems to a temporary file."
    );
    validateCondition(
        !((document.getElementById("create_dir_for_problem") as HTMLInputElement | null)?.checked)
        || ((document.getElementById("code_file_path") as HTMLInputElement | null)?.value.trim() !== ""
            && (document.getElementById("problem_file_path") as HTMLInputElement | null)?.value.trim() !== ""),
        "Code and problem file paths must be specified if creating a directory for each problem."
    );

    if (errors.length > 0) {
        alert("Correct the following errors:\n" + errors.join("\n"));
        return false;
    }
    return true;
}

document.addEventListener("DOMContentLoaded", () => {
    const elems = document.getElementsByClassName("pref");
    const saveBtn = document.getElementById("save") as HTMLButtonElement | null;
    if (elems.length > 0 && saveBtn != null) {
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
                e.addEventListener("change", () => saveBtn.disabled = false);
            }
        });

        saveBtn.addEventListener("click", () => {
            saveBtn.disabled = true;
            if (validate()) {
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
            }
        });
    }
});
