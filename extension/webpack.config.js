const path = require("path");

module.exports = {
    entry: {
        background: "./src/background/background.ts",
        content: "./src/content/content.ts",
        scraper: "./src/content/scraper.ts",
        options: "./src/options/options.ts"
    },
    output: {
        path: path.resolve(__dirname, "dist"),
        filename: "[name].js",
    },
    resolve: {
        extensions: [".ts", ".js"],
        modules: [path.resolve(__dirname, "src"), "node_modules"]
    },
    module: {
        rules: [
            {
                test: /\.ts$/,
                use: "ts-loader",
                exclude: /node_modules/
            },
        ],
    },
    mode: "production",
};
