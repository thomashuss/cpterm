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

package io.github.thomashuss.cpterm.core.message;

import java.util.Map;

public class TestResults
        extends Message
{
    private Map<String, TestCase> cases;
    private String error;

    public String getError()
    {
        return error;
    }

    public void setError(String error)
    {
        this.error = error;
    }

    public Map<String, TestCase> getCases()
    {
        return cases;
    }

    public void setCases(Map<String, TestCase> cases)
    {
        this.cases = cases;
    }

    public static class TestCase
    {
        private String input;
        private String output;
        private String expected;

        public String getExpected()
        {
            return expected;
        }

        public void setExpected(String expected)
        {
            this.expected = expected;
        }

        public String getOutput()
        {
            return output;
        }

        public void setOutput(String output)
        {
            this.output = output;
        }

        public String getInput()
        {
            return input;
        }

        public void setInput(String input)
        {
            this.input = input;
        }

        @Override
        public String toString()
        {
            return "TestCase{" +
                    "input='" + input + '\'' +
                    ", output='" + output + '\'' +
                    ", expected='" + expected + '\'' +
                    '}';
        }
    }
}
