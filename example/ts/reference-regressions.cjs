const assert = require('node:assert/strict');
const {spawnSync} = require('node:child_process');
const {cpSync, existsSync, mkdirSync, mkdtempSync, readFileSync, realpathSync, rmSync, writeFileSync} = require('node:fs');
const {dirname, join, resolve} = require('node:path');

const example = resolve(__dirname, '..');
const jar = resolve(example, '../app/build/libs/cfggen.jar');
const main = join(__dirname, 'dist/main.js');
const build = join(example, 'build');

function replaceFixture(path, pattern, replacement, count = 1) {
    const text = readFileSync(path, 'utf8');
    assert.equal([...text.matchAll(pattern)].length, count, `Fixture changed: ${path}`);
    // Preserve raw long tokens such as 9007199254740993; JSON.parse would round them.
    writeFileSync(path, text.replace(pattern, replacement));
}

function run(command, args, cwd) {
    const result = spawnSync(command, args, {cwd, encoding: 'utf8', timeout: 60000});
    if (result.error) throw result.error;
    assert.equal(result.signal, null, `${command} terminated by ${result.signal}`);
    return result;
}

const cases = [
    {
        name: 'self-loop',
        error: 'combat cycle Idle->Hit',
        change(data) {
            replaceFixture(join(data, 'combat/_ability/1001.json'),
                /("code":\s*"Combat",\s*"phases":\s*\[\s*\{\s*"key":\s*"Idle",\s*"next":\s*)"Hit"/g,
                '$1"Idle"');
        },
    },
    {
        name: 'empty-targets',
        error: 'all mixed-source targets loaded',
        change(data) {
            replaceFixture(join(data, 'combat/_cast/1.json'), /("targets":\s*)\[[^\]]*\]/g, '$1[]', 2);
        },
    },
    {
        name: 'map-extra-entry',
        change(data) {
            replaceFixture(join(data, 'combat/timeline.csv'), /"\(Hit,0\),Damage\(25\)"/g,
                '"(Warmup,1),Heal(1),(Hit,0),Damage(25)"');
        },
    },
];

assert(existsSync(jar), 'Build app/build/libs/cfggen.jar before running example tests');
assert(existsSync(main), 'Compile the TypeScript example before running reference regressions');
mkdirSync(build, {recursive: true});
const work = mkdtempSync(join(build, 'reference-regressions-'));
try {
    for (const test of cases) {
        const project = join(work, test.name);
        const data = join(project, 'config');
        cpSync(join(example, 'config'), data, {recursive: true});
        test.change(data);

        const generated = run('java', ['--sun-misc-unsafe-memory-access=allow', '-jar', jar, '-datadir', data, '-gen', 'bytes'], project);
        assert.equal(generated.status, 0, `${test.name}: generation failed\n${generated.stdout}${generated.stderr}`);
        assert(existsSync(join(project, 'base.bytes')), `${test.name}: no generated package`);

        const checked = run(process.execPath, [main], project);
        assert.equal(checked.status, test.error ? 1 : 0, `${test.name}: unexpected result\n${checked.stdout}${checked.stderr}`);
        if (test.error) {
            assert(checked.stderr.includes(`AssertionError [ERR_ASSERTION]: ${test.error}`),
                `${test.name}: failed outside the expected assertion\n${checked.stderr}`);
        } else {
            assert(checked.stdout.includes('ts: old and internal-reference configuration assertions passed'),
                `${test.name}: example did not complete`);
        }
        console.log(`PASS reference regression: ${test.name}`);
    }
} finally {
    assert.equal(dirname(realpathSync(work)), realpathSync(build), 'Refusing to remove a directory outside the test build folder');
    rmSync(work, {recursive: true, force: true});
}
