package io.github.thomashuss.cpterm.core.message;

import com.fasterxml.jackson.annotation.JsonProperty;

public final class Version
        extends Message
{
    @JsonProperty()
    private final String hostVersion;

    public Version(String hostVersion)
    {
        this.hostVersion = hostVersion;
    }
}
