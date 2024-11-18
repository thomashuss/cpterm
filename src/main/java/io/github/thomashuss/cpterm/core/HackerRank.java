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
import org.openqa.selenium.support.ui.ExpectedConditions;

public class HackerRank
        extends Site
{
    private final Monaco monaco = new Monaco();

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
        monaco.setDriver((JavascriptExecutor) driver.driver);
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
        return monaco.getCode();
    }

    @Override
    public void setCode(String code)
    {
        monaco.setCode(code);
    }

    @Override
    public String getLanguage()
    {
        return driver.wait.until(ExpectedConditions.elementToBeClickable(By.className("select-language")))
                .getText();
    }
}
