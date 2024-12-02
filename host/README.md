# cpterm host

The host is responsible for

- writing initial problem code to the filesystem,
- watching for changes in problem code, and
- rendering the problem statement to a file

The main class invokes the installer when run with no CLI arguments, or the
native messaging host otherwise.  Installation in this case means the `jar`
copies itself to an appropriate location for the OS and places the native
messaging `manifest.json` appropriately for the selected browsers.  The
installer defaults to an uninstaller if run from a `jar` in the same directory
where `.install.json` (describing a prior install of CPTerm) is present.

## Building

```
mvn clean package
```