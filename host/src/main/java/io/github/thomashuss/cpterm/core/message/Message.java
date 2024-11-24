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

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = Command.class, name = "command"),
        @JsonSubTypes.Type(value = LogEntry.class, name = "logEntry"),
        @JsonSubTypes.Type(value = NewProblem.class, name = "newProblem"),
        @JsonSubTypes.Type(value = SetCode.class, name = "setCode"),
        @JsonSubTypes.Type(value = SetPrefs.class, name = "setPrefs")
})
public abstract sealed class Message
        permits Command, LogEntry, NewProblem, SetCode, SetPrefs
{
}
