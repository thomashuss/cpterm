import { resolve } from "path";
import { createWriteStream, promises as fs } from "fs";
import archiver from "archiver";

const target = process.argv[2];
if (target === "firefox" || target === "chrome") {
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

    const cwd = resolve();
    const dist = resolve(cwd, "dist");
    const icons = resolve(cwd, "icons");

    archive.pipe(createWriteStream(resolve(dist, `cpterm-${target}.zip`)));
    archive.glob("*.js", { cwd: dist });

    const manifestFile = await fs.readFile(resolve(cwd, "manifest.json"));
    const manifest = JSON.parse(manifestFile);
    const iconSizes = ["16", "32", "48", "64", "96", "128"];
    if (target === "firefox") {
        archive.file(resolve(icons, "cpterm-dist.svg"), { name: "cpterm.svg" });
        for (let size of iconSizes) {
            manifest.icons[size] = "cpterm.svg";
        }
        manifest.browser_specific_settings = {
            gecko: {
                id: "cpterm-scraper@thomashuss.github.io",
                strict_min_version: "128.0"
            }
        };
        manifest.background = { scripts: ["background.js"] };
    } else {
        archive.glob("*.png", { cwd: icons });
        for (let size of iconSizes) {
            manifest.icons[size] = `cpterm-${size}.png`;
        }
        manifest.minimum_chrome_version = "126";
        manifest.background = { service_worker: "background.js" };
    }

    archive.append(JSON.stringify(manifest), { name: "manifest.json" });
    archive.file(resolve(cwd, "src", "options", "options.html"), { name: "options.html" });
    archive.finalize();
} else {
    throw "specify a browser target";
}
