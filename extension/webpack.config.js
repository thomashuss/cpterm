const path = require("path");

module.exports = {
    entry: {
        "background": "./src/background/background.ts",
        "content": "./src/content/content.ts",
        "scraper-inject": "./src/content/scraper-inject.ts",
        "options": "./src/options/options.ts"
    },
    output: {
        path: path.resolve(__dirname, "dist"),
        filename: "[name].js",
    },
    resolve: {
        extensions: [".ts", ".js"],
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
