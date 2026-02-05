// @ts-check
import { defineConfig } from 'astro/config';
import starlight from '@astrojs/starlight';
import starlightThemeRapide from 'starlight-theme-rapide';

// https://astro.build/config
export default defineConfig({
	integrations: [
		starlight({
			plugins: [starlightThemeRapide()],
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
			markdown: {
				shikiConfig: {
					themes: ['github-light', 'github-dark'],
					langs: [],
					// CFG 语言将在后续通过自定义组件添加
				},
			},
		}),
	],
});
