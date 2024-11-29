package io.github.thomashuss.cpterm;

import io.github.thomashuss.cpterm.core.CPTermHost;
import io.github.thomashuss.cpterm.installer.InstallerGUI;

import java.io.IOException;

public class CPTerm
{
    public static void main(String[] args)
    throws IOException
    {
        if (args.length == 0) {
            InstallerGUI.prompt();
        } else {
            CPTermHost.run();
        }
    }
}
