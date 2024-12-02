# cpterm

![cpterm logo](extension/icons/cpterm-128.png)

A Firefox and Chrome extension that brings competitive programming problems (à
la LeetCode, HackerRank, etc.) to your favorite code editor, PDF reader,
terminal, whatever.  Sync solutions from any editor to the browser and export
problem statements to PDF or HTML, or another format using Pandoc or
LibreOffice.

## Rationale

While the problems provided by competitive programming platforms are
high-quality, a browser-based coding experience is not; especially if you
already have a preferred way of writing code on your computer.  CPTerm is most
useful to those who fall into that category, such as vim/emacs/IDE users.  Using
a barebones editor like Windows Notepad or gedit is probably not worth the
overhead of this extension.

## Installation

### Quick start

You will need:

- CPTerm extension
  - for Firefox
  - for Chrome
- CPTerm host
  - Dependency: [Java Runtime Environment 8](https://www.java.com/) (newer version recommended)
  - Latest release as a `jar` file

### Host

Much of CPTerm's functionality is not possible without [native
messaging](https://developer.mozilla.org/en-US/docs/Mozilla/Add-ons/WebExtensions/Native_messaging)
because extensions themselves cannot access the OS filesystem, among other
limitations.  As such, a native messaging host is required for this extension to
be at all useful.  After installing an appropriate JRE, run the latest release
of the CPTerm host installer by either double-clicking the `jar` file or running
`java -jar cpterm-XXX.jar` where `XXX` is the version.  Select the browser(s)
you plan to use with CPTerm and leave the location as the default.

<details>
<summary>Troubleshooting</summary>
Ensure that the <code>java</code> binary of the JRE is in your
<code>PATH</code>.  This should happen by default if you used an installer or
your Linux distribution's package manager, but run <code>java -version</code> in
a terminal or command prompt to make sure.
</details>

## Development

See the `README` files in the `extension` and `host` directories.