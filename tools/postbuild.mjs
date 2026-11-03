#!/usr/bin/env node

/**
 * Fixes up the bob build output for the declaration files which are shipped as-is
 * (currently the ones for the vendored event-target-shim). bob mishandles them in
 * both directions, so for every .d.ts file in src/ we:
 *
 * - Copy it into the typescript output. That target builds with
 *   `tsc --emitDeclarationOnly`, and tsc never emits output for .d.ts inputs, so the
 *   relative imports pointing at them fail to resolve for consumers using the
 *   declaration files, which silently strips the EventTarget members off our classes.
 *
 * - Delete the empty module babel emitted for it in the commonjs and module outputs.
 *   Those targets compile every source file and rewrite the extension to .js, turning
 *   index.d.ts into an index.d.js which holds no code and which nothing imports.
 */

import fs from 'node:fs';
import path from 'node:path';
import process from 'node:process';

const root = path.resolve(import.meta.dirname, '..');
const source = path.join(root, 'src');
const output = path.join(root, 'lib');
const declarationsOutput = path.join(output, 'typescript');
const compiledOutputs = [ path.join(output, 'commonjs'), path.join(output, 'module') ];

/**
 * Finds every declaration file below the given directory, as paths relative to it.
 */
function findDeclarations(directory, prefix = '') {
    return fs.readdirSync(directory, { withFileTypes: true }).flatMap(entry => {
        const entryPath = path.join(prefix, entry.name);

        if (entry.isDirectory()) {
            return findDeclarations(path.join(directory, entry.name), entryPath);
        }

        return entry.name.endsWith('.d.ts') ? [ entryPath ] : [];
    });
}

if (!fs.existsSync(output)) {
    console.error(`${path.relative(root, output)} not found, run "bob build" first.`);
    process.exit(1);
}

for (const declaration of findDeclarations(source)) {
    const target = path.join(declarationsOutput, declaration);

    fs.mkdirSync(path.dirname(target), { recursive: true });
    fs.copyFileSync(path.join(source, declaration), target);

    console.info(`Copied src/${declaration} -> ${path.relative(root, target)}`);

    for (const compiledOutput of compiledOutputs) {
        const compiled = path.join(compiledOutput, declaration.replace(/\.ts$/, '.js'));

        for (const artifact of [ compiled, `${compiled}.map` ]) {
            if (fs.existsSync(artifact)) {
                fs.rmSync(artifact);

                console.info(`Removed ${path.relative(root, artifact)}`);
            }
        }
    }
}
