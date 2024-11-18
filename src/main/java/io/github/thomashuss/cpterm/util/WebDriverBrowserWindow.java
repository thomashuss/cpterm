package io.github.thomashuss.cpterm.util;

import org.openqa.selenium.WebDriver;

/**
 * Simply uses the {@link org.openqa.selenium.WebDriver.Window} to minimize and maximize the window.
 */
public class WebDriverBrowserWindow
        extends BrowserWindow
{
    private final WebDriver.Window window;

    WebDriverBrowserWindow(WebDriver driver)
    {
        window = driver.manage().window();
    }

    @Override
    public void hide()
    {
        window.minimize();
        visible = false;
    }

    @Override
    public void show()
    {
        window.maximize();
        visible = true;
    }
}
