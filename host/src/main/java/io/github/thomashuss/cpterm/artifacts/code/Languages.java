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

package io.github.thomashuss.cpterm.artifacts.code;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Get the file extension for a code snippet based on the language it's written in.
 */
public class Languages
{
    public static final String C = "C";
    public static final String CLOJURE = "Clojure";
    public static final String CPP = "C++";
    public static final String CSHARP = "C#";
    public static final String DART = "Dart";
    public static final String ELIXIR = "Elixir";
    public static final String ERLANG = "Erlang";
    public static final String GO = "Go";
    public static final String HASKELL = "Haskell";
    public static final String JAVA = "Java";
    public static final String JAVASCRIPT = "JavaScript";
    public static final String JULIA = "Julia";
    public static final String KOTLIN = "Kotlin";
    public static final String LUA = "Lua";
    public static final String OBJC = "Objective-C";
    public static final String PERL = "Perl";
    public static final String PHP = "PHP";
    public static final String PYTHON = "Python";
    public static final String R = "R";
    public static final String RACKET = "Racket";
    public static final String RUBY = "Ruby";
    public static final String RUST = "Rust";
    public static final String SCALA = "Scala";
    public static final String SWIFT = "Swift";
    public static final String TYPESCRIPT = "TypeScript";

    private static final String[] LANGUAGES = {C, CSHARP, CPP, CLOJURE, DART, ELIXIR, ERLANG, GO, HASKELL,
            JAVA, JAVASCRIPT, JULIA, KOTLIN, LUA, OBJC, PERL, PHP, PYTHON, R, RACKET, RUBY, RUST, SCALA, SWIFT,
            TYPESCRIPT};
    private static final Pattern[] PATTERNS = {compile("C(?!\\+{2})\\b"), compile("C#"),
            compile("C\\+\\+"), compile("Clojure"), compile("Dart"),
            compile("Elixir"), compile("Erlang"), compile("Go"),
            compile("Haskell"), compile("Java(?!Script)"), compile("JavaScript|Node"),
            compile("Julia"), compile("Kotlin"), compile("Lua"),
            compile("Objective-C"), compile("Perl"), compile("PHP"),
            compile("Python|Pypy"), compile("R\\b"), compile("Racket"),
            compile("Ruby"), compile("Rust"), compile("Scala"),
            compile("Swift"), compile("TypeScript")};
    private static final String[] EXTS = {"c", "cs", "cpp", "clj", "dart", "ex", "erl", "go",
            "hs", "java", "js", "jl", "kt", "lua", "m", "pl", "php", "py", "R", "rkt", "rb", "rs", "scala", "swift",
            "ts"};

    private static Pattern compile(String pattern)
    {
        return Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
    }

    /**
     * Get the file extension based on the best match of a language name.
     *
     * @param name fuzzy name of language
     * @return file extension (without leading {@code .})
     */
    public static String getExt(String name)
    {
        int idx = Arrays.binarySearch(LANGUAGES, name, String::compareToIgnoreCase);
        if (idx < 0) {
            Matcher m = PATTERNS[0].matcher(name);
            if (!m.find()) {
                for (idx = 1; idx < PATTERNS.length; idx++) {
                    if (m.reset().usePattern(PATTERNS[idx]).find()) {
                        return EXTS[idx];
                    }
                }
            }
            return EXTS[0];
        } else return EXTS[idx];
    }
}
