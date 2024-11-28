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
archive.pipe(fs.createWriteStream(path.resolve(dist, `cpterm-${target}.zip`)));
archive.glob("*.js", { cwd: dist });
archive.directory("icons", "icons");
archive.file(`manifest-${target}.json`, { name: "manifest.json" });
archive.file(path.resolve("src", "options", "options.html"), { name: "options.html" });
archive.finalize();