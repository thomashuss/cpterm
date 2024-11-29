package io.github.thomashuss.cpterm.installer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Locale;

public class Installer
{
    private static final boolean MAC;
    private static final boolean WINDOWS;
    private static final String MANIFEST_NAME = "io.github.thomashuss.CPTerm";
    private static final String MANIFEST_FNAME = MANIFEST_NAME + ".json";
    private static final String DESCRIPTION = "Allows the CPTerm extension to read and write scratch files";
    private static final String FIREFOX_EXT_ID = "cpterm@thomashuss.github.io";
    private static final String CHROME_EXT_ID = "chrome-extension://pfkaacnmmafpmdogookjkmdhepkabbkd/";
    private static final String FIREFOX_REG_KEY = "HKCU\\Software\\Mozilla\\NativeMessagingHosts\\" + MANIFEST_NAME;
    private static final String CHROME_REG_KEY = "HKCU\\Software\\Google\\Chrome\\NativeMessagingHosts\\" + MANIFEST_NAME;
    private static final String JAVA_OPTS = "java -Xmx256m -jar";
    private static final ObjectMapper mapper = new ObjectMapper();

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
    }

    private static Path getJar()
    {
        Path jar;
        try {
            jar = Path.of(Installer.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
        if (!jar.toString().endsWith(".jar")) {
            throw new RuntimeException("Not currently running from a jar");
        }
        return jar;
    }

    private static void writeManifest(Path bin, String extId, Path manifest)
    throws IOException
    {
        ObjectNode root = mapper.createObjectNode();
        root.put("name", MANIFEST_NAME);
        root.put("description", DESCRIPTION);
        root.put("path", bin.toAbsolutePath().toString());
        root.put("type", "stdio");
        root.set("allowed_extensions", mapper.createArrayNode().add(extId));
        mapper.writeValue(manifest.toFile(), root);
    }

    private static void putWinReg(String key, String value)
    throws IOException
    {
        try {
            new ProcessBuilder("reg", "add", key, "/ve", "/t", "REG_SZ", "/d", value, "/f").start().waitFor();
        } catch (InterruptedException e) {
            throw new IOException(e);
        }
    }

    private static void installFirefox(Path dir, Path bin)
    throws IOException
    {
        if (WINDOWS) {
            Path manifest = dir.resolve("manifest-firefox.json").toAbsolutePath();
            writeManifest(bin, FIREFOX_EXT_ID, manifest);
            putWinReg(FIREFOX_REG_KEY, manifest.toString());
        } else if (MAC) {
            writeManifest(bin, FIREFOX_EXT_ID, Path.of(System.getProperty("user.home"), "Library",
                    "Application Support", "Mozilla", "NativeMessagingHosts", MANIFEST_FNAME));
        } else {
            writeManifest(bin, FIREFOX_EXT_ID, Path.of(System.getProperty("user.home"), ".mozilla",
                    "native-messaging-hosts", MANIFEST_FNAME));
        }
    }

    private static void installChrome(Path dir, Path bin)
    throws IOException
    {
        if (WINDOWS) {
            Path manifest = dir.resolve("manifest-chrome.json").toAbsolutePath();
            writeManifest(bin, CHROME_EXT_ID, manifest);
            putWinReg(CHROME_REG_KEY, manifest.toString());
        } else if (MAC) {
            writeManifest(bin, CHROME_EXT_ID, Path.of(System.getProperty("user.home"), "Library",
                    "Application Support", "Google", "Chrome", "NativeMessagingHosts", MANIFEST_FNAME));
        } else {
            writeManifest(bin, CHROME_EXT_ID, Path.of(System.getProperty("user.home"), ".config",
                    "google-chrome", "NativeMessagingHosts", MANIFEST_FNAME));
        }
    }

    private static void installChromium(Path dir, Path bin)
    throws IOException
    {
        if (WINDOWS) {
            Path manifest = dir.resolve("manifest-chromium.json").toAbsolutePath();
            writeManifest(bin, CHROME_EXT_ID, manifest);
            putWinReg(CHROME_REG_KEY, manifest.toString());
        } else if (MAC) {
            writeManifest(bin, CHROME_EXT_ID, Path.of(System.getProperty("user.home"), "Library",
                    "Application Support", "Chromium", "NativeMessagingHosts", MANIFEST_FNAME));
        } else {
            writeManifest(bin, CHROME_EXT_ID, Path.of(System.getProperty("user.home"), ".config",
                    "chromium", "NativeMessagingHosts", MANIFEST_FNAME));
        }
    }

    private static Path createBin(Path dir, Path jar)
    throws IOException
    {
        Path bin;
        if (WINDOWS) {
            bin = dir.resolve("cpterm-host.bat");
            Files.writeString(bin, "@echo off\n" + JAVA_OPTS + " \"%~dp0/"
                    + jar.getFileName().toString() + "\" %*");
        } else {
            bin = dir.resolve("cpterm-host");
            Files.writeString(bin, "#!/bin/sh\nexec " + JAVA_OPTS + " '"
                    + jar.toAbsolutePath().toString().replace("'", "'\"'\"'") + "'");
        }
        if (!bin.toFile().setExecutable(true)) {
            throw new RuntimeException("Could not mark executable");
        }
        return bin;
    }

    public static void install(Path dir, List<Browser> browsers)
    throws IOException
    {
        Files.createDirectories(dir);
        Path currentJar = getJar();
        Path bin = createBin(dir,
                Files.copy(currentJar, dir.resolve(currentJar.getFileName()), StandardCopyOption.REPLACE_EXISTING));

        for (Browser b : browsers) {
            switch (b) {
                case FIREFOX -> installFirefox(dir, bin);
                case CHROME -> installChrome(dir, bin);
                case CHROMIUM -> installChromium(dir, bin);
            }
        }
    }
}
