package io.github.thomashuss.cpterm.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class X11BrowserWindow
        extends BrowserWindow
{
    private static final String WID_PID_SCRIPT = "xprop -root -notype _NET_CLIENT_LIST"
            + " | awk -F' # ' '{gsub(/, /,\"\\n\",$2);print $2}'"
            + " | while read -r wid; do echo -n \"$wid \"; xprop _NET_WM_PID -id \"$wid\"; done";
    private static boolean tried;
    private static boolean supported;

    private final String wid;

    private X11BrowserWindow(String wid)
    {
        this.wid = wid;
    }

    static X11BrowserWindow of(String pid)
    {
        try {
            String wid = getWid(pid);
            return new X11BrowserWindow(wid);
        } catch (IOException e) {
            return null;
        }
    }

    static boolean isSupported()
    {
        if (!tried) {
            tried = true;
            if (System.getenv("DISPLAY") != null) {
                try {
                    supported = new ProcessBuilder("which", "xdotool").start().waitFor() == 0
                            && new ProcessBuilder("which", "xprop").start().waitFor() == 0;
                } catch (IOException | InterruptedException ignored) {
                }
            }
        }
        return supported;
    }

    private static String getWid(String pid)
    throws IOException
    {
        Process p = new ProcessBuilder("sh", "-c", WID_PID_SCRIPT).start();
        String line;
        pid = ' ' + pid;
        try (BufferedReader out = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
            while ((line = out.readLine()) != null) {
                if (line.endsWith(pid)) {
                    return line.substring(0, line.indexOf(' '));
                }
            }
        }
        return null;
    }

    private void invokeXdotool(String op)
    {
        try {
            new ProcessBuilder("xdotool", op, wid).start().waitFor();
        } catch (InterruptedException | IOException ignored) {
        }
    }

    @Override
    public void hide()
    {
        invokeXdotool("windowunmap");
        visible = false;
    }

    @Override
    public void show()
    {
        invokeXdotool("windowmap");
        visible = true;
    }
}
