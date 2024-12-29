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

public final class NewProblem
        extends Message
{
    @JsonProperty()
    private String problem;
    @JsonProperty()
    private String code;
    @JsonProperty()
    private String language;
    @JsonProperty()
    private String url;
    @JsonProperty()
    private String name;

    public String getProblem()
    {
        return problem;
    }

    public String getCode()
    {
        return code;
    }

    public String getLanguage()
    {
        return language;
    }

    public String getUrl()
    {
        return url;
    }

    public String getName()
    {
        return name;
    }

}
