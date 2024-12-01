package io.github.thomashuss.cpterm.core.message;

public final class Version
        extends Message
{
    private final String hostVersion;

    public Version(String hostVersion)
    {
        this.hostVersion = hostVersion;
    }

    public String getHostVersion()
    {
        return hostVersion;
    }
}
