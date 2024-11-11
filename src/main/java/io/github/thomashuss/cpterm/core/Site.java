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

import java.security.SecureRandom;

/**
 * An interface with a challenge website.
 */
public abstract class Site
{
    static final SecureRandom RANDOM = new SecureRandom();
    CPTerm driver;

    /**
     * Get the link to this site's top-level address.
     *
     * @return URL representing site root
     */
    public abstract String getUrl();

    /**
     * Get the link to the site's challenge homepage.  This may differ from the URL returned by {@link #getUrl}.
     * Returns {@code null} if this site's challenge homepage does not differ from its root homepage.
     *
     * @return URL representing the page serving as the root for challenges
     */
    public abstract String getHome();

    /**
     * Perform some tasks after the site is first loaded.
     */
    abstract void onReady();

    /**
     * Get the outer HTML of the challenge statement.
     *
     * @return String representation of the challenge statement HTML
     */
    public abstract String getChallengeStatement();

    /**
     * Get the current code displayed in the in-browser editor.
     *
     * @return code from editor
     */
    public abstract String getCode();

    /**
     * Replace the code in the in-browser editor.
     *
     * @param code new code
     */
    public abstract void setCode(String code);

    /**
     * Get a potentially fuzzy name for the challenge's language.
     *
     * @return something resembling a language name.
     */
    public abstract String getLanguage();
}
