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

package io.github.thomashuss.cpterm.host.message;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public class TestResults
        extends Message
{
    @JsonProperty()
    private Map<String, TestCase> cases;
    @JsonProperty()
    private String error;

    public String getError()
    {
        return error;
    }

    public Map<String, TestCase> getCases()
    {
        return cases;
    }

    public static class TestCase
    {
        @JsonProperty()
        private String input;
        @JsonProperty()
        private String output;
        @JsonProperty()
        private String expected;
        @JsonProperty()
        private String error;

        public String getExpected()
        {
            return expected;
        }

        public String getOutput()
        {
            return output;
        }

        public String getInput()
        {
            return input;
        }

        public String getError()
        {
            return error;
        }
    }
}
