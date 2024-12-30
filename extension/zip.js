const target = process.argv[2];
if (target !== "firefox" && target !== "chrome") {
    console.error("specify a browser target");
}

const path = require("path");
const fs = require("fs");
const archiver = require("archiver");

const archive = archiver("zip", { zlib: { level: 9 } });

archive.on("warning", (err) => {
    if (err.code === "ENOENT") {
        console.error(err.message);
    } else {
        throw err;
    }
});
archive.on("error", (err) => {
    throw err;
});

const dist = path.resolve(__dirname, "dist");
const icons = path.resolve(__dirname, "icons");
const outStream = fs.createWriteStream(path.resolve(dist, `cpterm-${target}.zip`));
archive.pipe(outStream);
archive.glob("*.js", { cwd: dist });
if (target === "firefox") {
    archive.file(path.resolve(icons, "cpterm-dist.svg"), { name: "cpterm.svg" });
} else {
    archive.glob("*.png", { cwd: icons });
}
archive.file(path.resolve(__dirname, `manifest-${target}.json`), { name: "manifest.json" });
archive.file(path.resolve(__dirname, "src", "options", "options.html"), { name: "options.html" });
archive.finalize();
outStream.end();