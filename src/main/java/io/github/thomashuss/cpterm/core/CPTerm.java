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

package io.github.thomashuss.cpterm.core;

import io.github.thomashuss.cpterm.artifacts.code.Languages;
import io.github.thomashuss.cpterm.artifacts.code.Watcher;
import io.github.thomashuss.cpterm.artifacts.html.ConversionException;
import io.github.thomashuss.cpterm.artifacts.html.Converter;
import io.github.thomashuss.cpterm.artifacts.html.ExternalConverter;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Desktop;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputFilter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashSet;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Implements the core logic of the CPTerm program.
 */
public class CPTerm
{
    private static final Logger logger = LoggerFactory.getLogger(CPTerm.class);
    /**
     * Filter for deserializing cookie files.
     */
    private static final ObjectInputFilter COOKIE_FILTER
            = ObjectInputFilter.Config.createFilter("java.base/*;org.openqa.selenium.Cookie;!*");
    /**
     * Default directory for program files.
     */
    private static final Path defaultProgramDir = Path.of(System.getProperty("user.home"), ".cpterm");
    /**
     * Name of properties file within program directory.
     */
    private static final Path PROP_FILE_NAME = Path.of("cpterm.properties");
    /**
     * Name of cookies file within program directory.
     */
    private static final Path COOKIE_FILE_NAME = Path.of("cookies.dat");
    /**
     * Preferences key for the default {@link WebDriver}.
     */
    private static final String DEFAULT_DRIVER = "default_driver";
    /**
     * Preferences value for the Firefox driver as the default driver.
     */
    private static final String FIREFOX = "firefox";
    /**
     * Preferences key for the Firefox driver path.
     */
    private static final String FIREFOX_DRIVER_PATH = "firefox_driver_path";
    /**
     * Preferences key for the Chrome driver path.
     */
    private static final String CHROME_DRIVER_PATH = "chrome_driver_path";
    /**
     * Preferences value for the Chrome driver as the default driver.
     */
    private static final String CHROME = "chrome";
    /**
     * Preferences key for the flag indicating whether to write the problem statement to a temporary file.
     */
    private static final String PROBLEM_USE_TEMP_FILE = "write_problem_to_temp_file";
    /**
     * Default value for the flag indicating whether to write the problem statement to a temporary file.
     */
    private static final String DEFAULT_PROBLEM_USE_TEMP_FILE = "true";
    /**
     * Preferences key for the path to the problem statement file if not using a temporary file.
     */
    private static final String PROBLEM_FILE_PATH = "problem_file_path";
    /**
     * Preferences key for the flag indicating whether to write the problem code to a temporary file.
     */
    private static final String CODE_USE_TEMP_FILE = "write_code_to_temp_file";
    /**
     * Default value for the flag indicating whether to write the problem code to a temporary file.
     */
    private static final String DEFAULT_CODE_USE_TEMP_FILE = "true";
    /**
     * Preferences key for the path to the problem code file if not using a temporary file.
     */
    private static final String CODE_FILE_PATH = "code_file_path";
    /**
     * Preferences value for the Open HTML to PDF problem converter.
     */
    private static final String OPEN_HTML_TO_PDF = "open_html_to_pdf";
    /**
     * Preferences value for the Pandoc problem converter.
     */
    private static final String PANDOC = "pandoc";
    /**
     * Preferences key for the path to Pandoc.
     */
    private static final String PANDOC_PATH = "pandoc_path";
    /**
     * Preferences key for additional Pandoc command line arguments.
     */
    private static final String PANDOC_ARGS = "pandoc_args";
    /**
     * Preferences value for the LibreOffice problem converter.
     */
    private static final String LIBREOFFICE = "libreoffice";
    /**
     * Preferences key for the path to LibreOffice.
     */
    private static final String LIBREOFFICE_PATH = "libreoffice_path";
    /**
     * Preferences key for additional LibreOffice command line arguments.
     */
    private static final String LIBREOFFICE_ARGS = "libreoffice_args";
    /**
     * Preferences value for the raw HTML problem converter.
     */
    private static final String RAW_HTML = "raw_html";
    /**
     * Preferences key for the flag indicating whether to render SVG when converting problems to raw HTML.
     */
    private static final String RAW_HTML_SHOULD_RENDER_SVG = "raw_html_should_render_svg";
    /**
     * Default value for the flag indicating whether to render SVG when converting problems to raw HTML.
     */
    private static final String DEFAULT_RAW_HTML_SHOULD_RENDER_SVG = "false";
    /**
     * Preferences key for the default problem converter.
     */
    private static final String DEFAULT_PROBLEM_CONVERTER = "default_problem_converter";
    /**
     * Default value for the default problem converter.
     */
    private static final String DEFAULT_DEFAULT_PROBLEM_CONVERTER = OPEN_HTML_TO_PDF;
    /**
     * Preferences key for the problem file suffix.
     */
    private static final String PROBLEM_FILE_SUFFIX = "problem_file_suffix";
    /**
     * Default value for the problem file suffix.
     */
    private static final String DEFAULT_PROBLEM_FILE_SUFFIX = ".pdf";
    /**
     * Preferences key for the default text editor.
     */
    private static final String EDITOR = "editor";
    /**
     * Preferences key for the default problem statement viewer.
     */
    private static final String PROBLEM_VIEWER = "problem_viewer";
    /**
     * Contains default properties for the program.
     */
    private static final Properties DEFAULTS = new Properties();

    static {
        DEFAULTS.setProperty(CHROME_DRIVER_PATH, "");
        DEFAULTS.setProperty(CODE_FILE_PATH, "");
        DEFAULTS.setProperty(CODE_USE_TEMP_FILE, DEFAULT_CODE_USE_TEMP_FILE);
        DEFAULTS.setProperty(DEFAULT_PROBLEM_CONVERTER, DEFAULT_DEFAULT_PROBLEM_CONVERTER);
        DEFAULTS.setProperty(DEFAULT_DRIVER, "");
        DEFAULTS.setProperty(EDITOR, "");
        DEFAULTS.setProperty(FIREFOX_DRIVER_PATH, "");
        DEFAULTS.setProperty(LIBREOFFICE_ARGS, "");
        DEFAULTS.setProperty(LIBREOFFICE_PATH, "");
        DEFAULTS.setProperty(PANDOC_ARGS, "");
        DEFAULTS.setProperty(PANDOC_PATH, "");
        DEFAULTS.setProperty(PROBLEM_FILE_PATH, "");
        DEFAULTS.setProperty(PROBLEM_FILE_SUFFIX, DEFAULT_PROBLEM_FILE_SUFFIX);
        DEFAULTS.setProperty(PROBLEM_USE_TEMP_FILE, DEFAULT_PROBLEM_USE_TEMP_FILE);
        DEFAULTS.setProperty(PROBLEM_VIEWER, "");
        DEFAULTS.setProperty(RAW_HTML_SHOULD_RENDER_SVG, DEFAULT_RAW_HTML_SHOULD_RENDER_SVG);
    }

    /**
     * Perform some tasks asynchronously.
     */
    private final ExecutorService exe = Executors.newCachedThreadPool();
    /**
     * Keep all driver operations on the same thread.
     */
    private final ExecutorService driverExe = new ThreadPoolExecutor(1, 1, 0L,
            TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(), Executors.defaultThreadFactory())
    {
        @Override
        protected void afterExecute(Runnable r, Throwable t)
        {
            super.afterExecute(r, t);
            if (t != null) {
                logger.error("Exception occurred on WebDriver thread", t);
            }
        }
    };
    /**
     * Problem code file.
     */
    private final ScratchFile codeFile
            = new ScratchFile(CODE_USE_TEMP_FILE, CODE_FILE_PATH, EDITOR);
    /**
     * Problem statement file.
     */
    private final ScratchFile problemFile
            = new ScratchFile(PROBLEM_USE_TEMP_FILE, PROBLEM_FILE_PATH, PROBLEM_VIEWER);
    /**
     * Files to clean.
     */
    private final Set<Path> cleanable = new HashSet<>();
    /**
     * Runtime properties of the program.
     */
    private final Properties prop;
    /**
     * Currently loaded program directory.
     */
    private final Path programDir;
    /**
     * Active {@code WebDriver} instance; effectively final.
     */
    WebDriver driver;
    /**
     * {@code Options} for the current {@code driver}; convenience.
     */
    WebDriver.Options options;
    /**
     * {@code WebDriverWait} for the current {@code driver}; convenience.
     */
    WebDriverWait wait;
    /**
     * Used for opening files.
     */
    private Desktop desktop;
    /**
     * Whether an attempt was made at setting {@code desktop}.
     */
    private boolean desktopTried;
    /**
     * Whether the driver is ready after loading a new site.
     */
    private volatile boolean ready;
    /**
     * Currently used file watcher.
     */
    private CodeFileWatcher codeFileWatcher;
    /**
     * Currently loaded site.
     */
    private Site site;

    /**
     * Create a new instance of the program with the default program directory.
     *
     * @throws IOException if there was a problem loading the properties file
     */
    public CPTerm()
    throws IOException
    {
        this(defaultProgramDir);
    }

    /**
     * Create a new instance of the program with the specified program directory.
     *
     * @param programDir path to program directory
     * @throws IOException if there was a problem loading the properties file
     */
    public CPTerm(Path programDir)
    throws IOException
    {
        prop = new Properties(DEFAULTS);
        File propertiesFile = programDir.resolve(PROP_FILE_NAME).toFile();
        this.programDir = programDir;
        if (propertiesFile.exists()) {
            try (FileInputStream fis = new FileInputStream(propertiesFile)) {
                prop.load(fis);
            }
        } else {
            ensureProgramDirectory();
            try (FileOutputStream fos = new FileOutputStream(propertiesFile)) {
                DEFAULTS.store(fos, "cpterm configuration file");
                logger.info("Created configuration file with defaults at {}", propertiesFile);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static Set<Cookie> castCookieSet(Object o)
    {
        return (Set<Cookie>) o;
    }

    /**
     * Set the parameters for an {@link ExternalConverter}.
     *
     * @param converter Converter to configure
     * @param pathKey   preferences key for executable path
     * @param argsKey   preferences key for command line arguments
     * @return {@code converter}, for convenience
     * @throws InvalidPrefsException if the executable path is not set
     */
    private ExternalConverter configExternalConverter(ExternalConverter converter,
                                                      String pathKey,
                                                      String argsKey)
    throws InvalidPrefsException
    {
        String path;
        String args;
        path = prop.getProperty(pathKey);
        args = prop.getProperty(argsKey);
        converter.setArgs(args);
        try {
            converter.setExePath(path.isEmpty() ? null : Path.of(path));
        } catch (IllegalArgumentException e) {
            throw new InvalidPrefsException(e.getMessage());
        }
        return converter;
    }

    /**
     * Configure and return the default {@link Converter}.
     *
     * @return a converter
     * @throws MissingPrefsException if the default problem converter is not set
     * @throws InvalidPrefsException if the set value for the default problem converter is not recognized
     */
    private Converter getConverter()
    throws MissingPrefsException, InvalidPrefsException
    {
        String defaultConverter;
        defaultConverter = prop.getProperty(DEFAULT_PROBLEM_CONVERTER);
        if (defaultConverter.isEmpty()) {
            throw new MissingPrefsException(DEFAULT_PROBLEM_CONVERTER);
        }
        return switch (defaultConverter) {
            case OPEN_HTML_TO_PDF -> Converter.OPEN_HTML_TO_PDF;
            case PANDOC -> configExternalConverter(Converter.PANDOC, PANDOC_PATH, PANDOC_ARGS);
            case LIBREOFFICE -> configExternalConverter(Converter.LIBREOFFICE, LIBREOFFICE_PATH, LIBREOFFICE_ARGS);
            case RAW_HTML -> {
                Converter.RAW_HTML.setRenderSvg(Boolean.parseBoolean(prop.getProperty(RAW_HTML_SHOULD_RENDER_SVG)));
                yield Converter.RAW_HTML;
            }
            default -> throw new InvalidPrefsException(DEFAULT_PROBLEM_CONVERTER);
        };
    }

    /**
     * Invoke when ready to start the {@link WebDriver}.
     *
     * @throws MissingPrefsException if a required preferences value is missing
     * @throws InvalidPrefsException if a required preferences value exists but is invalid
     */
    public void init()
    throws MissingPrefsException, InvalidPrefsException
    {
        boolean hasPath = false;
        String ffDriverPath;
        String cDriverPath;
        String driverName;
        File test;
        String driverPath;
        Runnable driverInit;

        ffDriverPath = prop.getProperty(FIREFOX_DRIVER_PATH);
        cDriverPath = prop.getProperty(CHROME_DRIVER_PATH);
        driverName = prop.getProperty(DEFAULT_DRIVER);
        if (!ffDriverPath.isEmpty()) {
            System.setProperty("webdriver.firefox.driver", ffDriverPath);
            hasPath = true;
        }
        if (!cDriverPath.isEmpty()) {
            System.setProperty("webdriver.chrome.driver", cDriverPath);
            hasPath = true;
        }
        if (!hasPath) throw new MissingPrefsException(DEFAULT_DRIVER);

        switch (driverName) {
            case FIREFOX -> {
                logger.info("Using Firefox driver");
                driverPath = ffDriverPath;
                driverInit = this::initFirefox;
            }
            case CHROME -> {
                logger.info("Using Chrome driver");
                driverPath = cDriverPath;
                driverInit = this::initChrome;
            }
            default -> throw new MissingPrefsException(DEFAULT_DRIVER);
        }
        if (!(test = new File(driverPath)).exists() || !test.canExecute()) {
            throw new InvalidPrefsException(DEFAULT_DRIVER);
        }
        try {
            driverExe.submit(postDriverInit(driverInit)).get();
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Failed to start driver", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Curried utility method for running common post-initialization tasks for the WebDriver.
     *
     * @param r initializes the driver
     * @return a function for running on another thread
     */
    private Runnable postDriverInit(Runnable r)
    {
        return () -> {
            r.run();
            options = driver.manage();
            wait = new WebDriverWait(driver, Duration.ofSeconds(30));
        };
    }

    /**
     * Create and set a new Firefox WebDriver.
     */
    private void initFirefox()
    {
        driver = new FirefoxDriver();
    }

    /**
     * Create and set a new Chrome WebDriver.
     */
    private void initChrome()
    {
        driver = new ChromeDriver();
    }

    /**
     * Load a new site in the browser asynchronously.  There is no guarantee that the browser
     * will be in a ready state when returning from this method.  Use {@link #isReady}
     * to check if the browser is ready to start a problem.
     *
     * @param siteFactory yields some site
     */
    public void startSite(Supplier<Site> siteFactory)
    {
        ready = false;
        driverExe.submit(() -> {
            site = siteFactory.get();
            site.driver = this;
            String home = site.getHome();
            driver.get(site.getUrl());
            if (loadCookies()) {
                if (home != null) {
                    driver.get(home);
                } else {
                    driver.navigate().refresh();
                }
            }
            site.onReady();
            ready = true;
        });
    }

    /**
     * Whether the browser is ready to "start" a problem; i.e., convert to a file and get code.
     *
     * @return {@code true} if the browser could start a problem if one were loaded; does not mean a problem is
     * currently loaded
     */
    public boolean isReady()
    {
        return ready;
    }

    /**
     * The user has indicated that a problem is currently loaded in the browser, so create the files, open
     * them, and listen for changes.
     *
     * @throws PrefsException if there is a problem with the configuration for the default problem converter
     */
    public void startProblem()
    throws PrefsException
    {
        if (!ready) return;
        Future<?> pf = exe.submit(() -> {
            Converter pe = getConverter();
            Path pp;
            try {
                pp = problemFile.create(prop.getProperty(PROBLEM_FILE_SUFFIX));
            } catch (IOException e) {
                logger.error("Failed to create problem file", e);
                throw new RuntimeException(e);
            }
            String problem;
            String url;
            try {
                problem = driverExe.submit(
                        () -> Objects.requireNonNull(
                                site.getChallengeStatement())).get();
                url = driverExe.submit(driver::getCurrentUrl).get();
            } catch (InterruptedException | ExecutionException e) {
                return null;
            }
            try {
                pe.convert(problem, url, pp.toAbsolutePath());
                problemFile.open();
            } catch (ConversionException ignored) {
            }
            return null;
        });

        Future<?> cf = exe.submit(() -> {
            Path cp;
            String lang;
            try {
                try {
                    lang = driverExe.submit(site::getLanguage).get();
                } catch (InterruptedException | ExecutionException e) {
                    throw new RuntimeException(e);
                }
                cp = codeFile.create('.' + Languages.getExt(lang));
                codeFileWatcher = new CodeFileWatcher(cp);
                codeFileWatcher.writeCode();
                codeFileWatcher.start();
                codeFile.open();
            } catch (IOException e) {
                logger.error("Failed to create and start watcher for code file", e);
                throw new RuntimeException(e);
            }
        });

        try {
            cf.get();
            pf.get();
        } catch (InterruptedException | ExecutionException e) {
            if (e.getCause() instanceof PrefsException pe) {
                throw pe;
            }
            throw new RuntimeException(e);
        }
    }

    /**
     * Asynchronously clean up after the user has closed a problem.
     */
    public void endProblem()
    {
        if (!ready) return;
        exe.submit(() -> {
            codeFileWatcher.stop();
            codeFile.clean();
            problemFile.clean();
            return null;
        });
        exe.submit(this::saveCookies);
    }

    /**
     * Invoke on WebDriver thread.
     */
    private boolean loadCookies()
    {
        Set<Cookie> cookies;
        File cookieFile = programDir.resolve(COOKIE_FILE_NAME).toFile();
        if (cookieFile.exists()) {
            try {
                try (FileInputStream fis = new FileInputStream(cookieFile);
                     ObjectInputStream ois = new ObjectInputStream(fis)) {
                    ois.setObjectInputFilter(COOKIE_FILTER);
                    cookies = castCookieSet(ois.readObject());
                }
                cookies.forEach(options::addCookie);
                return true;
            } catch (IOException | ClassNotFoundException e) {
                logger.error("Couldn't load cookies", e);
            }
        } else {
            logger.info("Cookie file does not exist");
        }
        return false;
    }

    /**
     * DO NOT invoke on WebDriver thread.
     */
    private void saveCookies()
    {
        try {
            Set<Cookie> cookies = driverExe.submit(options::getCookies).get();
            ensureProgramDirectory();
            try (FileOutputStream fos = new FileOutputStream(programDir.resolve(COOKIE_FILE_NAME).toFile());
                 BufferedOutputStream bos = new BufferedOutputStream(fos);
                 ObjectOutputStream oos = new ObjectOutputStream(bos)) {
                oos.writeObject(cookies);
            }
        } catch (IOException | InterruptedException | ExecutionException e) {
            logger.error("Couldn't write cookies", e);
        }
    }

    private void ensureProgramDirectory()
    throws IOException
    {
        Files.createDirectories(programDir);
    }

    /**
     * Quit the program by gracefully cleaning up.
     */
    public void quit()
    {
        if (driver != null) {
            driverExe.submit(driver::quit);
        }

        if (codeFileWatcher != null) {
            codeFileWatcher.stop();
        }
        codeFile.clean();
        problemFile.clean();

        for (Path p : cleanable) {
            try {
                Files.delete(p);
            } catch (IOException e) {
                logger.warn("Couldn't delete scratch file", e);
            }
        }

        try {
            Watcher.close();
        } catch (IOException e) {
            logger.warn("Couldn't close the file watcher", e);
        }

        driverExe.shutdown();
        exe.shutdown();

        try {
            if (!driverExe.awaitTermination(60, TimeUnit.SECONDS)) {
                logger.error("WebDriver thread didn't terminate");
            }
        } catch (InterruptedException e) {
            logger.warn("WebDriver thread interrupted while waiting", e);
        }

        try {
            if (!exe.awaitTermination(60, TimeUnit.SECONDS)) {
                logger.error("Background task thread didn't terminate");
            }
        } catch (InterruptedException e) {
            logger.warn("Background task thread interrupted while waiting", e);
        }
    }

    private boolean hasDesktop()
    {
        if (!desktopTried) {
            desktopTried = true;
            if (Desktop.isDesktopSupported()) {
                try {
                    desktop = Desktop.getDesktop();
                    return true;
                } catch (UnsupportedOperationException e) {
                    logger.error("Unable to open files with desktop", e);
                    return false;
                }
            } else return false;
        } else return desktop != null;
    }

    private class CodeFileWatcher
            extends Watcher
    {
        private final File file;

        private CodeFileWatcher(Path path)
        {
            super(path);
            file = path.toFile();
        }

        /**
         * Write the site's current problem code to the file on this thread.  Obtains current code on the driver thread.
         *
         * @throws IOException if there was a problem writing to the file or obtaining the code
         */
        private void writeCode()
        throws IOException
        {
            Future<String> f = driverExe.submit(site::getCode);
            String code;
            try {
                code = f.get();
            } catch (ExecutionException | InterruptedException e) {
                throw new IOException(e.getCause());
            }
            try (FileOutputStream fos = new FileOutputStream(file);
                 OutputStreamWriter osw = new OutputStreamWriter(fos);
                 BufferedWriter bw = new BufferedWriter(osw)) {
                bw.write(code);
            }
        }

        /**
         * Place the contents of the file in the site.
         *
         * @throws IOException if there was a problem reading the file
         */
        @Override
        protected void modified()
        throws IOException
        {
            String lines;
            try (FileInputStream fis = new FileInputStream(file);
                 InputStreamReader isr = new InputStreamReader(fis);
                 BufferedReader br = new BufferedReader(isr)) {
                lines = br.lines().collect(Collectors.joining("\n"));
            }
            driverExe.submit(() -> site.setCode(lines));
        }
    }

    private class ScratchFile
    {
        private static final Object lock = new Object();
        private final String tempKey;
        private final String pathKey;
        private final String handlerKey;
        private Path path;
        private String lastPrefix;
        private boolean isTemp;

        /**
         * Create a new interface to a scratch file.
         *
         * @param tempKey    prefs key for whether the file should be temporary
         * @param pathKey    prefs key for the file prefix
         * @param handlerKey prefs key for the file handler
         */
        private ScratchFile(String tempKey, String pathKey, String handlerKey)
        {
            this.tempKey = tempKey;
            this.pathKey = pathKey;
            this.handlerKey = handlerKey;
        }

        /**
         * Create a blank scratch file according to preferences set by the user.  An old file
         * is added to the deletion list if needed.
         *
         * @param suffix should be appended to the file name
         * @return path to newly created file
         * @throws IOException if there was a problem creating a temp file
         */
        private synchronized Path create(String suffix)
        throws IOException
        {
            String prefix = null;
            if (!Boolean.parseBoolean(prop.getProperty(tempKey))) {
                prefix = prop.getProperty(pathKey);
            }
            if (prefix == null || prefix.isEmpty()) {
                if (isTemp && path != null) {
                    cleanable.add(path);
                }
                isTemp = true;
                path = Files.createTempFile("cpterm_", suffix);
            } else {
                if (!isTemp && path != null && prefix.equals(lastPrefix)) {
                    cleanable.add(path);
                }
                isTemp = false;
                lastPrefix = prefix;
                path = Path.of(prefix + suffix);
            }
            logger.info("Using scratch file {}", path);
            return path;
        }

        /**
         * Add the existing file to the deletion list if needed.
         */
        private synchronized void clean()
        {
            if (isTemp && path != null) {
                cleanable.add(path);
            }
            path = null;
        }

        /**
         * Open the file with its handler as defined in {@link #prop}.  Use {@link Desktop} if no handler is set.
         */
        private synchronized void open()
        {
            String handler;
            handler = prop.getProperty(handlerKey);
            if (handler.isEmpty()) {
                synchronized (lock) {
                    if (hasDesktop()) {
                        try {
                            desktop.open(path.toFile());
                        } catch (IOException e) {
                            logger.error("Unable to open file with desktop", e);
                        }
                    }
                }
            } else {
                try {
                    new ProcessBuilder(handler, path.toString()).start();
                } catch (IOException e) {
                    logger.error("Unable to open file with handler", e);
                }
            }
        }
    }
}
