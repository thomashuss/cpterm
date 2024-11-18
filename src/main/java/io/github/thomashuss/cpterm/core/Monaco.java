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

package io.github.thomashuss.cpterm.core;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.ScriptKey;

/**
 * Interface between a {@link Site} and a Monaco-based editor.
 */
class Monaco
{
    private JavascriptExecutor executor;
    private ScriptKey getEditorContents;
    private ScriptKey setEditorContents;

    Monaco()
    {
    }

    void setDriver(JavascriptExecutor executor)
    {
        this.executor = executor;
        getEditorContents = executor.pin("return monaco.editor.getModels()[0].getValue()");
        setEditorContents = executor.pin("monaco.editor.getModels()[0].setValue(arguments[0])");
    }

    String getCode()
    {
        return String.valueOf(executor.executeScript(getEditorContents));
    }

    void setCode(String code)
    {
        executor.executeScript(setEditorContents, code);
    }
}
