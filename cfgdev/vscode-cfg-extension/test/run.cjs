const fs = require('node:fs');
const path = require('node:path');
const {spawn} = require('node:child_process');
const root = path.resolve(__dirname, '..');
const executable = process.env.VSCODE_EXECUTABLE || 'C:/Program Files/Microsoft VS Code/Code.exe';
if (!fs.existsSync(executable)) throw new Error('Set VSCODE_EXECUTABLE to an installed VSCode executable (no download is performed)');
const runDir = fs.mkdtempSync(path.join(root, '.vscode-test-'));
process.env.CFGGEN_TEST_RUN_DIR = runDir;
fs.mkdirSync(path.join(runDir, 'user/User'), {recursive: true});
fs.writeFileSync(path.join(runDir, 'user/User/settings.json'), JSON.stringify({
    'chat.disableAIFeatures': true, 'telemetry.telemetryLevel': 'off', 'update.mode': 'none',
    'workbench.startupEditor': 'none', 'extensions.ignoreRecommendations': true,
}));
const args = [path.join(root, 'test/workspace'), '--disable-extensions', '--disable-workspace-trust', '--disable-gpu',
    '--disable-extension=github.copilot', '--disable-extension=github.copilot-chat',
    '--skip-welcome', '--skip-release-notes', '--new-window', '--user-data-dir=' + path.join(runDir, 'user'),
    '--extensions-dir=' + path.join(runDir, 'extensions'), '--extensionDevelopmentPath=' + root,
    '--extensionTestsPath=' + path.join(root, 'out/test/suite/index.js')];
let child;
if (process.platform === 'win32') {
    const quote = value => "'" + value.replace(/'/g, "''") + "'";
    const stdout = path.join(runDir, 'stdout.log'), stderr = path.join(runDir, 'stderr.log');
    const command = `$p = Start-Process -FilePath ${quote(executable)} -ArgumentList @(${args.map(arg => quote('"' + arg + '"')).join(',')}) -WindowStyle Hidden -PassThru -Wait -RedirectStandardOutput ${quote(stdout)} -RedirectStandardError ${quote(stderr)}; exit $p.ExitCode`;
    child = spawn('powershell.exe', ['-NoProfile', '-NonInteractive', '-Command', command], {windowsHide: true, stdio: 'inherit'});
    child.on('exit', code => {
        const output = fs.existsSync(stdout) ? fs.readFileSync(stdout, 'utf8') : '';
        const passed = code === 0 && /^CFG ExtensionHost: \d+ tests passed/m.test(output);
        process.stdout.write(output.split(/\r?\n/).filter(line => /^(PASS |CFG ExtensionHost:)/.test(line)).join('\n') + '\n');
        if (!passed) {
            console.error('ExtensionHost tests did not complete; logs:', runDir);
            if (fs.existsSync(stderr)) process.stderr.write(fs.readFileSync(stderr));
        }
        process.exitCode = passed ? 0 : 1;
    });
} else {
    child = spawn(executable, args, {stdio: 'inherit'});
    child.on('exit', code => { process.exitCode = code ?? 1; });
}
child.on('error', error => { console.error(error); process.exitCode = 1; });
