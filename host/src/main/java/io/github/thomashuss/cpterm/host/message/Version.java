package io.github.thomashuss.cpterm.host.message;

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
