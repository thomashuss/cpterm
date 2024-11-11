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

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.ScriptKey;
import org.openqa.selenium.support.ui.ExpectedConditions;

public class HackerRank
        extends Site
{
    private final String editorName;
    private ScriptKey createEditorRef;
    private ScriptKey getEditorContents;
    private ScriptKey setEditorContents;

    public HackerRank()
    {
        int editorNameLen = RANDOM.nextInt(4, 16);
        StringBuilder buf = new StringBuilder(editorNameLen);
        for (int i = 0; i < editorNameLen; i++) {
            buf.append((char) ((RANDOM.nextBoolean() ? 'a' : 'A') + RANDOM.nextInt(26)));
        }
        editorName = buf.toString();
    }

    @Override
    public String getUrl()
    {
        return "https://www.hackerrank.com/";
    }

    @Override
    public String getHome()
    {
        return "https://www.hackerrank.com/dashboard";
    }

    @Override
    void onReady()
    {
        JavascriptExecutor js = (JavascriptExecutor) driver.driver;
        createEditorRef = js.pin("window." + editorName + "=monaco.editor.getModels()[0]");
        getEditorContents = js.pin("return " + editorName + ".getValue()");
        setEditorContents = js.pin(editorName + ".setValue(arguments[0])");
    }

    @Override
    public String getChallengeStatement()
    {
        return driver.wait.until(ExpectedConditions.elementToBeClickable(By.className("challenge-body-html")))
                .getAttribute("outerHTML");
    }

    @Override
    public String getCode()
    {
        ((JavascriptExecutor) driver.driver).executeScript(createEditorRef);
        return String.valueOf(((JavascriptExecutor) driver.driver).executeScript(getEditorContents));
    }

    @Override
    public void setCode(String code)
    {
        ((JavascriptExecutor) driver.driver).executeScript(setEditorContents, code);
    }

    @Override
    public String getLanguage()
    {
        return driver.wait.until(ExpectedConditions.elementToBeClickable(By.className("select-language")))
                .getText();
    }
}
