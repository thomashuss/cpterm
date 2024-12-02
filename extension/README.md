# cpterm extension

The extension is responsible for

- sending problems to the native messaging host,
- updating code in the browser when changes are made, and
- configuring the native messaging host.

## Building

Install dependencies:
```
npm install
```

Compile for packaging:
```
npm run build
```

Zip for each browser:
```
npm run zip:firefox
npm run zip:chrome
```

You will now see `cpterm-firefox.zip` and/or `cpterm-chrome.zip` in `dist/`.
