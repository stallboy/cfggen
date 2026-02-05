// @ts-check
import { defineConfig } from 'astro/config';
import starlight from '@astrojs/starlight';
import starlightThemeRapide from 'starlight-theme-rapide';
import starlightLinksValidator from 'starlight-links-validator';
import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';

// 读取 CFG TextMate grammar
const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const cfgGrammarPath = path.resolve(__dirname, '../cfgdev/vscode-cfg-extension/syntaxes/cfg.tmLanguage.json');
const cfgGrammar = JSON.parse(fs.readFileSync(cfgGrammarPath, 'utf-8'));

// 为 Shiki 添加语言标识
cfgGrammar.id = 'cfg';
cfgGrammar.aliases = ['cfg'];

// https://astro.build/config
export default defineConfig({
	integrations: [
		starlight({
			plugins: [starlightThemeRapide(), starlightLinksValidator()],
			title: 'cfggen 文档',
			description: 'excel/CSV/JSON object mapping. object database viewer and editor. generate reading code.',
			// logo: {
			// 	src: '/intro.png'
			// },
			locales: {
				root: {
					label: '简体中文',
					lang: 'zh-CN'
				}
			},
			sidebar: [
				{
					label: '简介',
					items: ['']
				},
				{
					label: '配表系统',
					autogenerate: { directory: 'cfggen' }
				},
				{
					label: '配置编辑器',
					autogenerate: { directory: 'cfgeditor' }
				},
				{
					label: 'AI 生成功能',
					autogenerate: { directory: 'aigen' }
				},
				{
					label: 'VSCode 扩展',
					items: ['vscodeExtension']
				}
			],
			expressiveCode: {
				shiki: {
					langs: [cfgGrammar],
				},
			},
		}),
	],
});
