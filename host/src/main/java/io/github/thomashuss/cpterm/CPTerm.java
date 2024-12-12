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

package io.github.thomashuss.cpterm;

import io.github.thomashuss.cpterm.core.CPTermHost;
import io.github.thomashuss.cpterm.installer.Installer;
import io.github.thomashuss.cpterm.installer.InstallerGUI;

import java.io.IOException;
import java.util.Arrays;

public class CPTerm
{
    public static void main(String[] args)
    throws IOException
    {
        if (args.length == 0) {
            InstallerGUI.prompt();
        } else if ("--install".equals(args[0])) {
            Installer.installCLI(Arrays.asList(args).subList(1, args.length));
        } else if ("--uninstall".equals(args[0])) {
            Installer.uninstallCLI();
        } else {
            CPTermHost.run();
        }
    }
}
