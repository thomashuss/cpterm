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
import io.github.thomashuss.cpterm.core.message.LogEntry;
import io.github.thomashuss.cpterm.core.message.Message;
import io.github.thomashuss.cpterm.core.message.NewProblem;
import io.github.thomashuss.cpterm.core.message.SetCode;
import io.github.thomashuss.cpterm.core.message.SetPrefs;
import io.github.thomashuss.cpterm.nm.Host;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Implements the core logic of the CPTerm native messaging host.
 */
public class CPTermHost
        extends Host<Message>
{
    private static final Logger logger = LoggerFactory.getLogger(CPTermHost.class);
    private static final Object lock = new Object();
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
     * Preferences key for the flag indicating whether to render problem statements.
     */
    private static final String RENDER_PROBLEM = "render_problem";
    /**
     * Default value for the flag indicating whether to render problem statements.
     */
    private static final String DEFAULT_RENDER_PROBLEM = "true";
    /**
     * Preferences key for the flag indicating whether to re-generate the problem statement
     * if the same problem is submitted twice.
     */
    private static final String RELOAD_PROBLEM = "reload_problem";
    /**
     * Default behavior for reloading the same problem.
     */
    private static final String DEFAULT_RELOAD_PROBLEM = "false";
    /**
     * Preferences key for the default text editor.
     */
    private static final String EDITOR = "editor";
    /**
     * Preferences key for the default problem statement viewer.
     */
    private static final String PROBLEM_VIEWER = "problem_viewer";
    /**
     * File to run after creating problem files.
     */
    private static final String POST_PROBLEM_HOOK = "post_problem_hook";
    /**
     * Contains default properties for the program.
     */
    private static final Properties DEFAULTS = new Properties();

    static {
        DEFAULTS.setProperty(CODE_FILE_PATH, "");
        DEFAULTS.setProperty(CODE_USE_TEMP_FILE, DEFAULT_CODE_USE_TEMP_FILE);
        DEFAULTS.setProperty(DEFAULT_PROBLEM_CONVERTER, DEFAULT_DEFAULT_PROBLEM_CONVERTER);
        DEFAULTS.setProperty(EDITOR, "");
        DEFAULTS.setProperty(LIBREOFFICE_ARGS, "");
        DEFAULTS.setProperty(LIBREOFFICE_PATH, "");
        DEFAULTS.setProperty(PANDOC_ARGS, "");
        DEFAULTS.setProperty(PANDOC_PATH, "");
        DEFAULTS.setProperty(POST_PROBLEM_HOOK, "");
        DEFAULTS.setProperty(PROBLEM_FILE_PATH, "");
        DEFAULTS.setProperty(PROBLEM_FILE_SUFFIX, DEFAULT_PROBLEM_FILE_SUFFIX);
        DEFAULTS.setProperty(PROBLEM_USE_TEMP_FILE, DEFAULT_PROBLEM_USE_TEMP_FILE);
        DEFAULTS.setProperty(PROBLEM_VIEWER, "");
        DEFAULTS.setProperty(RAW_HTML_SHOULD_RENDER_SVG, DEFAULT_RAW_HTML_SHOULD_RENDER_SVG);
        DEFAULTS.setProperty(RELOAD_PROBLEM, DEFAULT_RELOAD_PROBLEM);
        DEFAULTS.setProperty(RENDER_PROBLEM, DEFAULT_RENDER_PROBLEM);
    }

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
    private final Properties prop = new Properties(DEFAULTS);
    /**
     * Default converter as set in properties.
     */
    private Converter defaultConverter = Converter.OPEN_HTML_TO_PDF;
    /**
     * Used for opening files.
     */
    private Desktop desktop;
    /**
     * Whether an attempt was made at setting {@code desktop}.
     */
    private boolean desktopTried;
    /**
     * Currently used file watcher.
     */
    private CodeFileWatcher codeFileWatcher;
    /**
     * URL of last generated problem file.
     */
    private String lastUrl;

    public CPTermHost()
    {
        super(Message.class);
    }

    public static void run()
    throws IOException
    {
        CPTermHost h = new CPTermHost();
        try {
            h.listen();
        } finally {
            h.quit();
        }
    }

    /**
     * Log an error message to {@link CPTermHost#logger} and the browser extension, if possible.
     *
     * @param s string message
     * @param t throwable if available (can be {@code null})
     */
    private void err(String s, Throwable t)
    {
        if (t == null) {
            logger.error(s);
            try {
                send(new LogEntry("error", s));
            } catch (IOException ignored) {
            }
        } else {
            logger.error(s, t);
            try {
                send(new LogEntry("error", s + "\n" + t.getMessage()));
            } catch (IOException ignored) {
            }
        }
    }

    /**
     * Apply new preferences.
     *
     * @param p pref key -> string representation of value
     */
    public void setPrefs(Map<String, String> p)
    {
        prop.putAll(p);
        if (Boolean.parseBoolean(prop.getProperty(RENDER_PROBLEM))) {
            switch (prop.getProperty(DEFAULT_PROBLEM_CONVERTER)) {
                case OPEN_HTML_TO_PDF:
                    defaultConverter = Converter.OPEN_HTML_TO_PDF;
                    break;
                case PANDOC:
                    defaultConverter = configExternalConverter(Converter.PANDOC, PANDOC_PATH, PANDOC_ARGS);
                    break;
                case LIBREOFFICE:
                    defaultConverter = configExternalConverter(Converter.LIBREOFFICE, LIBREOFFICE_PATH,
                            LIBREOFFICE_ARGS);
                    break;
                case RAW_HTML:
                    Converter.RAW_HTML.setRenderSvg(Boolean.parseBoolean(prop.getProperty(RAW_HTML_SHOULD_RENDER_SVG)));
                    defaultConverter = Converter.RAW_HTML;
                    break;
                default:
                    defaultConverter = null;
            }
        }
    }

    /**
     * Set the parameters for an {@link ExternalConverter}.
     *
     * @param converter Converter to configure
     * @param pathKey   preferences key for executable path
     * @param argsKey   preferences key for command line arguments
     * @return {@code converter}, for convenience
     */
    private ExternalConverter configExternalConverter(ExternalConverter converter,
                                                      String pathKey,
                                                      String argsKey)
    {
        String path;
        String args;
        path = prop.getProperty(pathKey);
        args = prop.getProperty(argsKey);
        converter.setArgs(args);
        try {
            converter.setExePath(path.isEmpty() ? null : Paths.get(path));
        } catch (IllegalArgumentException e) {
            logger.error("Invalid path", e);
            return null;
        }
        return converter;
    }

    /**
     * A new problem was received, so create the files, open them, and listen for changes.
     */
    public void startProblem(NewProblem np)
    {
        if (defaultConverter == null) {
            err("Default problem converter is improperly configured", null);
            logger.info("Problem converter is set to {}", prop.getProperty(DEFAULT_PROBLEM_CONVERTER));
            return;
        }

        if (codeFileWatcher != null) {
            codeFileWatcher.stop();
        }
        codeFile.clean();
        problemFile.clean();

        String url = np.getUrl();
        Path pp = null;
        if (Boolean.parseBoolean(prop.getProperty(RENDER_PROBLEM)) &&
                (Boolean.parseBoolean(prop.getProperty(RELOAD_PROBLEM)) || !url.equals(lastUrl))) {
            try {
                pp = problemFile.create(prop.getProperty(PROBLEM_FILE_SUFFIX));
            } catch (IOException e) {
                err("Failed to create problem file", e);
                return;
            }
            try {
                defaultConverter.convert(np.getProblem(), url, pp.toAbsolutePath());
                problemFile.open();
            } catch (ConversionException e) {
                err("Conversion error", e);
                return;
            }
            lastUrl = url;
        }

        Path cp = null;
        try {
            cp = codeFile.create('.' + Languages.getExt(np.getLanguage()));
            codeFileWatcher = new CodeFileWatcher(cp);
            codeFileWatcher.write(np.getCode());
            codeFileWatcher.start();
            codeFile.open();
        } catch (IOException e) {
            err("Failed to create and start watcher for code file", e);
        }

        String hook = prop.getProperty(POST_PROBLEM_HOOK);
        if (!hook.isEmpty()) {
            try {
                new ProcessBuilder(hook, cp == null ? "" : cp.toAbsolutePath().toString(),
                        pp == null ? "" : pp.toAbsolutePath().toString()).start();
            } catch (IOException e) {
                err("Failed to run hook", e);
            }
        }
    }

    /**
     * Gracefully clean up.
     */
    public void quit()
    {
        logger.info("Quitting gracefully");
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
    }

    /**
     * Determine whether the {@link Desktop} can be used.  If it can, but the {@link CPTermHost#desktop}
     * field has not been set, retrieve a new instance.
     *
     * @return {@code true} if {@link Desktop} can be used, {@code false} otherwise
     */
    private boolean hasDesktop()
    {
        if (!desktopTried) {
            desktopTried = true;
            if (Desktop.isDesktopSupported()) {
                try {
                    desktop = Desktop.getDesktop();
                    return true;
                } catch (UnsupportedOperationException e) {
                    err("Unable to open files with desktop", e);
                    return false;
                }
            } else return false;
        } else return desktop != null;
    }

    @Override
    public boolean received(Message message)
    {
        if (message instanceof NewProblem) {
            startProblem((NewProblem) message);
        } else if (message instanceof SetPrefs) {
            setPrefs(((SetPrefs) message).getPrefs());
        }
        return true;
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
         * Write the code to the file.
         *
         * @throws IOException if there was a problem writing to the file or obtaining the code
         */
        private void write(String code)
        throws IOException
        {
            try (FileOutputStream fos = new FileOutputStream(file);
                 OutputStreamWriter osw = new OutputStreamWriter(fos);
                 BufferedWriter bw = new BufferedWriter(osw)) {
                bw.write(code);
            }
        }

        /**
         * Send the contents of the file to the browser.
         */
        @Override
        protected void modified()
        {
            String lines;
            try (FileInputStream fis = new FileInputStream(file);
                 InputStreamReader isr = new InputStreamReader(fis);
                 BufferedReader br = new BufferedReader(isr)) {
                lines = br.lines().collect(Collectors.joining("\n"));
            } catch (IOException e) {
                err("Could not read file", e);
                return;
            }
            try {
                send(new SetCode(lines));
            } catch (IOException e) {
                logger.error("Could not send code file", e);
            }
        }
    }

    private class ScratchFile
    {
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
                path = Paths.get(prefix + suffix);
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
                            err("Unable to open file with desktop", e);
                        }
                    }
                }
            } else {
                try {
                    new ProcessBuilder(handler, path.toString()).start();
                } catch (IOException e) {
                    err("Unable to open file with handler", e);
                }
            }
        }
    }
}
