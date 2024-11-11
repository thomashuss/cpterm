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

package io.github.thomashuss.cpterm.tui;

import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.gui2.DefaultWindowManager;
import com.googlecode.lanterna.gui2.EmptySpace;
import com.googlecode.lanterna.gui2.MultiWindowTextGUI;
import com.googlecode.lanterna.gui2.dialogs.ActionListDialogBuilder;
import com.googlecode.lanterna.gui2.dialogs.MessageDialogBuilder;
import com.googlecode.lanterna.gui2.dialogs.MessageDialogButton;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import com.googlecode.lanterna.terminal.swing.TerminalEmulatorAutoCloseTrigger;
import io.github.thomashuss.cpterm.core.CPTerm;
import io.github.thomashuss.cpterm.core.HackerRank;
import io.github.thomashuss.cpterm.core.InvalidPrefsException;
import io.github.thomashuss.cpterm.core.MissingPrefsException;

import java.io.IOException;

public class CPTermUI
{
    private final Screen screen;
    private final MultiWindowTextGUI gui;
    private final CPTerm cpterm;

    public CPTermUI()
    throws IOException
    {
        cpterm = new CPTerm();
        DefaultTerminalFactory factory = new DefaultTerminalFactory();
        factory.setTerminalEmulatorFrameAutoCloseTrigger(TerminalEmulatorAutoCloseTrigger.CloseOnExitPrivateMode);
        screen = new TerminalScreen(factory.createTerminal());
        screen.startScreen();
        gui = new MultiWindowTextGUI(screen, new DefaultWindowManager(), new EmptySpace(TextColor.ANSI.DEFAULT));
    }

    public static void main(String[] args)
    throws IOException
    {
        new CPTermUI().mainLoop();
    }

    public void mainLoop()
    throws IOException
    {
        try {
            cpterm.init();
            enterSite();
        } catch (MissingPrefsException e) {
            new MessageDialogBuilder()
                    .setTitle("Error")
                    .setText("Missing a value for the property " + e.getMessage() + ".")
                    .addButton(MessageDialogButton.Close)
                    .build()
                    .showDialog(gui);
        } catch (InvalidPrefsException e) {
            new MessageDialogBuilder()
                    .setTitle("Error")
                    .setText("The current value of " + e.getMessage() + " is invalid.")
                    .addButton(MessageDialogButton.Close)
                    .build()
                    .showDialog(gui);
        } finally {
            try {
                screen.stopScreen();
            } finally {
                cpterm.quit();
            }
        }
    }

    private void enterSite()
    {
        new ActionListDialogBuilder()
                .setTitle("Challenge Websites")
                .setDescription("Select a website to use.")
                .addAction("HackerRank", () -> cpterm.startSite(HackerRank::new))
                .build()
                .showDialog(gui);
        waitForProblem();
    }

    private void waitForProblem()
    {
        while (new MessageDialogBuilder()
                .setTitle("Waiting for problem")
                .setText("Select OK when a problem is open.")
                .addButton(MessageDialogButton.OK)
                .addButton(MessageDialogButton.Abort)
                .build()
                .showDialog(gui) != MessageDialogButton.Abort) {
            if (cpterm.isReady()) {
                waitForProblemEnd();
            }
        }
    }

    private void waitForProblemEnd()
    {
        try {
            cpterm.startProblem();
            new MessageDialogBuilder()
                    .setTitle("Problem")
                    .setText("Watching for problem work.")
                    .addButton(MessageDialogButton.Abort)
                    .build()
                    .showDialog(gui);
        } catch (Exception e) {
            new MessageDialogBuilder()
                    .setTitle("Error")
                    .setText(e.toString())
                    .addButton(MessageDialogButton.OK)
                    .build()
                    .showDialog(gui);
        }
        cpterm.endProblem();
    }
}
