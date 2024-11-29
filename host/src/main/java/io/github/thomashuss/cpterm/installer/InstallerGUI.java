package io.github.thomashuss.cpterm.installer;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.Dimension;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class InstallerGUI
{
    private static final Dimension SPACER = new Dimension(0, 5);
    private static final String[] OPTIONS = {"Cancel", "Install"};

    private static void promptInstall()
    {
        JPanel rootPanel = new JPanel();
        rootPanel.setLayout(new BoxLayout(rootPanel, BoxLayout.PAGE_AXIS));

        rootPanel.add(new JLabel("Select the browsers to target:"));
        rootPanel.add(Box.createRigidArea(SPACER));

        JCheckBox firefoxCheck = new JCheckBox("Firefox");
        rootPanel.add(firefoxCheck);
        JCheckBox chromeCheck = new JCheckBox("Chrome");
        rootPanel.add(chromeCheck);
        JCheckBox chromiumCheck = new JCheckBox("Chromium");
        rootPanel.add(chromiumCheck);
        rootPanel.add(Box.createRigidArea(SPACER));

        rootPanel.add(new JLabel("Select the install location:"));
        JTextField locationField = new JTextField(Installer.getDefaultDir().toString());
        rootPanel.add(locationField);

        boolean ff, chrome, chromium;
        String location;
        while (JOptionPane.showOptionDialog(null, rootPanel, "CPTerm Installer",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE,
                null, OPTIONS, null) == 1) {
            location = locationField.getText();
            ff = firefoxCheck.isSelected();
            chrome = chromeCheck.isSelected();
            chromium = chromiumCheck.isSelected();
            if (!ff && !chrome && !chromium) {
                JOptionPane.showMessageDialog(null, "Select at least one browser.",
                        null, JOptionPane.ERROR_MESSAGE);
            } else if (location.isEmpty()) {
                JOptionPane.showMessageDialog(null, "Choose an installation location.",
                        null, JOptionPane.ERROR_MESSAGE);
            } else {
                List<Browser> b = new ArrayList<>((ff ? 1 : 0) + (chrome ? 1 : 0) + (chromium ? 1 : 0));
                if (ff) {
                    b.add(Browser.FIREFOX);
                }
                if (chrome) {
                    b.add(Browser.CHROME);
                }
                if (chromium) {
                    b.add(Browser.CHROMIUM);
                }
                try {
                    Installer.install(Paths.get(locationField.getText()), b);
                    JOptionPane.showMessageDialog(null,
                            "Installation succeeded.  You may delete the installer.",
                            null, JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(null,
                            "Installation failed.\n\n" + e,
                            null, JOptionPane.ERROR_MESSAGE);
                }
                break;
            }
        }
    }

    private static boolean promptUninstall()
    {
        Path installationFile = Installer.getInstallationFilePath();
        if (installationFile != null) {
            if (JOptionPane.showOptionDialog(null,
                    "An existing installation was found.  Would you like to remove it?",
                    null, JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE,
                    null, null, null) == JOptionPane.YES_OPTION) {
                try {
                    if (Installer.uninstall(installationFile)) {
                        JOptionPane.showMessageDialog(null,
                                "Uninstallation succeeded.  You may delete the folder containing the installation.",
                                null, JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        JOptionPane.showMessageDialog(null,
                                "Some files could not be deleted.",
                                null, JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(null,
                            "Uninstallation failed.\n\n" + e,
                            null, JOptionPane.ERROR_MESSAGE);
                }
                return true;
            }
        }
        return false;
    }

    public static void prompt()
    {
        if (!promptUninstall()) {
            promptInstall();
        }
    }
}
