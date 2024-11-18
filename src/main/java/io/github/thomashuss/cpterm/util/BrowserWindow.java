package io.github.thomashuss.cpterm.util;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;

import java.util.Objects;

/**
 * Abstract controls for a browser window.
 */
public abstract class BrowserWindow
{
    private static final boolean IS_LINUX = System.getProperty("os.name").contains("Linux");
    /**
     * Update after successfully changing window state.
     */
    protected boolean visible = true;

    private static String getPID(FirefoxDriver ff)
    {
        return Objects.requireNonNull(ff.getCapabilities().getCapability("moz:processID")).toString();
    }

    /**
     * Get the best {@link BrowserWindow} implementation depending on the {@link WebDriver} instance
     * and the host platform.
     *
     * @param driver driver to control
     * @return a new {@link BrowserWindow}, or {@code null} if no implementation will work
     */
    public static BrowserWindow of(WebDriver driver)
    {
        if (IS_LINUX && driver instanceof FirefoxDriver ff && X11BrowserWindow.isSupported()) {
            return X11BrowserWindow.of(getPID(ff));
        }
        if (!IS_LINUX) {
            return new WebDriverBrowserWindow(driver);
        }
        return null;
    }

    /**
     * Hide the browser window.
     */
    public abstract void hide();

    /**
     * Show the browser window.
     */
    public abstract void show();

    /**
     * Determine the state of the browser window.  The window state is tracked by this object and is not queried,
     * so it may be inaccurate in exceptional cases.
     *
     * @return true of the window is visible
     */
    public boolean isVisible()
    {
        return visible;
    }
}
