package io.github.thomashuss.cpterm.installer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Locale;

public class Installer
{
    private static final boolean MAC;
    private static final boolean WINDOWS;
    private static final String JAR_NAME = "cpterm-host.jar";
    private static final String INSTALLATION_FILE_NAME = ".cpterm_install.json";
    private static final String MANIFEST_NAME = "io.github.thomashuss.CPTerm";
    private static final String MANIFEST_FNAME = MANIFEST_NAME + ".json";
    private static final String DESCRIPTION = "Allows the CPTerm extension to read and write scratch files";
    private static final String FIREFOX_EXT_ID = "cpterm@thomashuss.github.io";
    private static final String CHROME_EXT_ID = "chrome-extension://pfkaacnmmafpmdogookjkmdhepkabbkd/";
    private static final String FIREFOX_REG_KEY = "HKCU\\Software\\Mozilla\\NativeMessagingHosts\\" + MANIFEST_NAME;
    private static final String CHROME_REG_KEY = "HKCU\\Software\\Google\\Chrome\\NativeMessagingHosts\\" + MANIFEST_NAME;
    private static final String JAVA_OPTS = "java -Xmx256m -jar";
    private static final String HOME = System.getProperty("user.home");
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final Path JAR;

    static {
        String os = System.getProperty("os.name").toLowerCase(Locale.ROOT);
        if (os.contains("windows")) {
            WINDOWS = true;
            MAC = false;
        } else if (os.contains("mac") || os.contains("darwin")) {
            MAC = true;
            WINDOWS = false;
        } else {
            MAC = false;
            WINDOWS = false;
        }

        Path jar;
        try {
            jar = Paths.get(Installer.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            if (!jar.toString().endsWith(".jar")) {
                jar = null;
            }
        } catch (URISyntaxException e) {
            jar = null;
        }
        JAR = jar;
    }

    private final Path dir;
    private final Installation installation = new Installation();

    private Installer(Path dir)
    {
        this.dir = dir;
    }

    private static Path getJar()
    {
        if (JAR == null) {
            throw new RuntimeException("Not currently running from a jar");
        }
        return JAR;
    }

    private static void delWinReg(String key)
    throws IOException
    {
        try {
            new ProcessBuilder("REG", "DELETE", key, "/f").start().waitFor();
        } catch (InterruptedException e) {
            throw new IOException(e);
        }
    }

    public static Path getInstallationFilePath()
    {
        if (JAR != null) {
            Path ret = JAR.getParent().resolve(INSTALLATION_FILE_NAME);
            if (Files.exists(ret)) {
                return ret;
            }
        }
        return null;
    }

    public static boolean uninstall(Path installFile)
    throws IOException
    {
        boolean deleted = true;
        Installation installation = mapper.readValue(installFile.toFile(), Installation.class);
        List<File> files = installation.getFiles();
        List<String> regs = installation.getRegs();
        if (files != null) {
            for (File file : files) {
                if (!file.delete()) {
                    deleted = false;
                }
            }
        }
        if (regs != null) {
            for (String reg : regs) {
                delWinReg(reg);
            }
        }
        Files.delete(installFile);
        return deleted;
    }

    public static Path getDefaultDir()
    {
        if (WINDOWS) {
            return Paths.get(System.getenv("LOCALAPPDATA"), "cpterm");
        } else if (MAC) {
            return Paths.get(HOME, "Library", "Application Support", "cpterm");
        } else {
            return Paths.get(HOME, ".local", "share", "cpterm");
        }
    }

    public static void install(Path dir, List<Browser> browsers)
    throws IOException
    {
        new Installer(dir).installFor(browsers);
    }

    private void putWinReg(String key, String value)
    throws IOException
    {
        try {
            new ProcessBuilder("REG", "ADD", key, "/ve", "/t", "REG_SZ", "/d", value, "/f").start().waitFor();
            installation.addReg(key);
        } catch (InterruptedException e) {
            throw new IOException(e);
        }
    }

    private void writeManifest(Path bin, String extId, Path manifest)
    throws IOException
    {
        ObjectNode root = mapper.createObjectNode();
        File manifestFile = manifest.toFile();
        root.put("name", MANIFEST_NAME);
        root.put("description", DESCRIPTION);
        root.put("path", bin.toAbsolutePath().toString());
        root.put("type", "stdio");
        root.set("allowed_extensions", mapper.createArrayNode().add(extId));
        mapper.writeValue(manifestFile, root);
        installation.addFile(manifestFile);
    }

    private void installFirefox(Path bin)
    throws IOException
    {
        if (WINDOWS) {
            Path manifest = dir.resolve("manifest-firefox.json").toAbsolutePath();
            writeManifest(bin, FIREFOX_EXT_ID, manifest);
            putWinReg(FIREFOX_REG_KEY, manifest.toString());
        } else if (MAC) {
            writeManifest(bin, FIREFOX_EXT_ID, Paths.get(HOME, "Library",
                    "Application Support", "Mozilla", "NativeMessagingHosts", MANIFEST_FNAME));
        } else {
            writeManifest(bin, FIREFOX_EXT_ID, Paths.get(HOME, ".mozilla",
                    "native-messaging-hosts", MANIFEST_FNAME));
        }
    }

    private void installChrome(Path bin)
    throws IOException
    {
        if (WINDOWS) {
            Path manifest = dir.resolve("manifest-chrome.json").toAbsolutePath();
            writeManifest(bin, CHROME_EXT_ID, manifest);
            putWinReg(CHROME_REG_KEY, manifest.toString());
        } else if (MAC) {
            writeManifest(bin, CHROME_EXT_ID, Paths.get(HOME, "Library",
                    "Application Support", "Google", "Chrome", "NativeMessagingHosts", MANIFEST_FNAME));
        } else {
            writeManifest(bin, CHROME_EXT_ID, Paths.get(HOME, ".config",
                    "google-chrome", "NativeMessagingHosts", MANIFEST_FNAME));
        }
    }

    private void installChromium(Path bin)
    throws IOException
    {
        if (WINDOWS) {
            Path manifest = dir.resolve("manifest-chromium.json").toAbsolutePath();
            writeManifest(bin, CHROME_EXT_ID, manifest);
            putWinReg(CHROME_REG_KEY, manifest.toString());
        } else if (MAC) {
            writeManifest(bin, CHROME_EXT_ID, Paths.get(HOME, "Library",
                    "Application Support", "Chromium", "NativeMessagingHosts", MANIFEST_FNAME));
        } else {
            writeManifest(bin, CHROME_EXT_ID, Paths.get(HOME, ".config",
                    "chromium", "NativeMessagingHosts", MANIFEST_FNAME));
        }
    }

    private Path createBin(Path jar)
    throws IOException
    {
        Path bin;
        File binFile;
        if (WINDOWS) {
            bin = dir.resolve("cpterm-host.bat");
            binFile = bin.toFile();
            try (FileOutputStream fos = new FileOutputStream(binFile);
                 PrintWriter pw = new PrintWriter(fos)) {
                pw.println("@echo off");
                pw.print(JAVA_OPTS);
                pw.print(" \"%~dp0/");
                pw.print(jar.getFileName());
                pw.println("\" %*");
            }
        } else {
            bin = dir.resolve("cpterm-host");
            binFile = bin.toFile();
            try (FileOutputStream fos = new FileOutputStream(binFile);
                 PrintWriter pw = new PrintWriter(fos)) {
                pw.println("#!/bin/sh");
                pw.print("exec ");
                pw.print(JAVA_OPTS);
                pw.print(" '");
                pw.print(jar.toAbsolutePath().toString().replace("'", "'\"'\"'"));
                pw.println("' \"$@\"");
            }
        }
        installation.addFile(binFile);
        if (!binFile.setExecutable(true)) {
            throw new RuntimeException("Could not mark executable");
        }
        return bin;
    }

    private void installFor(List<Browser> browsers)
    throws IOException
    {
        Files.createDirectories(dir);
        Path currentJar = getJar();
        Path newJar = Files.copy(currentJar, dir.resolve(JAR_NAME), StandardCopyOption.REPLACE_EXISTING);
        File installationFile = newJar.getParent().resolve(INSTALLATION_FILE_NAME).toFile();
        Path bin = createBin(newJar);
        for (Browser b : browsers) {
            if (b == Browser.FIREFOX) installFirefox(bin);
            else if (b == Browser.CHROME) installChrome(bin);
            else if (b == Browser.CHROMIUM) installChromium(bin);
        }
        mapper.writeValue(installationFile, installation);
    }
}