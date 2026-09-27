import type { ListingCategory } from '@yuki/db/schema';

type Tier = { weight: number; terms: string[] };

const RULES: Record<ListingCategory, Tier[]> = {
	gaming: [
		{
			weight: 3,
			terms: [
				'skin injector',
				'keymapper',
				'gamepad',
				'game translation',
				'fps unlocker',
				'gaming booster',
				'game performance',
				'game controller',
				'gamepad keymapper',
				'gacha',
				'keyboard and mouse',
				'mouse and keyboard',
				'input mapper'
			]
		},
		{
			weight: 2,
			terms: [
				'game',
				'games',
				'gaming',
				'codm',
				'mobile legends',
				'retroarch',
				'handheld',
				'emulator',
				'游戏'
			]
		},
		{ weight: 1, terms: ['tuning', 'resolution', 'boost', 'xbox'] }
	],
	media: [
		{
			weight: 3,
			terms: [
				'manga reader',
				'anime',
				'video player',
				'music player',
				'screen recorder',
				'call recorder',
				'voice recorder',
				'equalizer',
				'漫画',
				'media player',
				'ebook',
				'epub'
			]
		},
		{
			weight: 2,
			terms: [
				'mihon',
				'tachiyomi',
				'komikku',
				'dantotsu',
				'mpv',
				'reader',
				'player',
				'downloader',
				'audio',
				'video',
				'photo',
				'yt-dlp',
				'photos',
				'videos',
				'gallery',
				'bilibili',
				'哔哩哔哩',
				'视频'
			]
		},
		{ weight: 1, terms: ['library', 'stream', 'playback', 'fork'] }
	],
	automation: [
		{
			weight: 3,
			terms: [
				'automation',
				'automate',
				'ai agent',
				'tasker',
				'auto-click',
				'自动化',
				'screen tapping',
				'autonomous',
				'自动打卡',
				'gui automation',
				'device agent',
				'语音自动化',
				'全自动',
				'自动签到'
			]
		},
		{
			weight: 2,
			terms: [
				'llm',
				'accessibility service',
				'rule',
				'workflow',
				'automatically',
				'智能体',
				'自然语言',
				'无障碍'
			]
		},
		{ weight: 1, terms: ['schedule', 'trigger', 'macro'] }
	],
	app_management: [
		{
			weight: 3,
			terms: [
				'app store',
				'apk installer',
				'debloat',
				'uninstall',
				'package manager',
				'hibernation',
				'freeze',
				'app manager',
				'app management',
				'应用管理',
				'app catalog',
				'package installer',
				'silent install',
				'静默安装',
				'packageinstaller',
				'debloater',
				'debloating'
			]
		},
		{
			weight: 2,
			terms: [
				'install',
				'installer',
				'update',
				'updates',
				'apk',
				'f-droid',
				'aurora store',
				'disable apps',
				'unused apps',
				'magisk module',
				'magisk modules'
			]
		},
		{ weight: 1, terms: ['store', 'catalog', 'version'] }
	],
	file_management: [
		{
			weight: 3,
			terms: [
				'file manager',
				'文件管理',
				'file browser',
				'file sharing',
				'filemanager',
				'webdav',
				'文件共享',
				'file transfer',
				'文件传输',
				'传输文件'
			]
		},
		{
			weight: 2,
			terms: ['files', 'folder', 'archive', 'compress', 'zip', 'transfer', 'storage', '目录']
		},
		{ weight: 1, terms: ['browse', 'share', 'sync'] }
	],
	networking: [
		{
			weight: 3,
			terms: [
				'vpn',
				'proxy',
				'v2ray',
				'xray',
				'sing-box',
				'clash',
				'mihomo',
				'tunnel',
				'network mode',
				'5g',
				'lte',
				'hotspot',
				'wifi',
				'wifi password',
				'wifi passwords',
				'volte',
				'ims',
				'apn',
				'data usage',
				'network usage',
				'代理'
			]
		},
		{
			weight: 2,
			terms: ['dns', 'traffic', 'telephony', 'cellular', 'cell', 'modem', 'sim', 'carrier', '网络']
		},
		{ weight: 1, terms: ['connection', 'server'] }
	],
	privacy_security: [
		{
			weight: 3,
			terms: [
				'privacy',
				'tracker blocker',
				'isolation',
				'work profile',
				'device owner',
				'device policy',
				'anti-theft',
				'permission audit',
				'shoulder surfing',
				'隐私',
				'sandbox',
				'firewall',
				'exploit',
				'privilege escalation',
				'cve'
			]
		},
		{ weight: 2, terms: ['secure', 'security', 'encrypt', 'block ads', 'spoof', 'audit'] },
		{ weight: 1, terms: ['protect', 'private'] }
	],
	developer_tools: [
		{
			weight: 3,
			terms: [
				'ide',
				'terminal emulator',
				'code editor',
				'developer options',
				'logcat',
				'开发者',
				'adb manager',
				'debugger',
				'adb shell',
				'shell commands',
				'adb commands',
				'执行命令',
				'system apis',
				'app_process',
				'developer guide',
				'shizuku fork'
			]
		},
		{
			weight: 2,
			terms: [
				'fastboot',
				'compiler',
				'lsp',
				'language server',
				'emulator',
				'scripting',
				'javascript engine',
				'linux terminal',
				'commands',
				'debugging'
			]
		},
		{ weight: 1, terms: ['sdk', 'toolchain', 'adb'] }
	],
	device_specific: [
		{
			weight: 3,
			terms: [
				'samsung',
				'xiaomi',
				'miui',
				'hyperos',
				'nothing phone',
				'meta quest',
				'oneplus',
				'galaxy',
				'inmo',
				'nothing os',
				'quest',
				'ayn thor'
			]
		},
		{ weight: 2, terms: ['one ui', 'coloros', 'realme', 'oppo', 'vivo', 'honor', 'foldable'] },
		{ weight: 1, terms: [] }
	],
	customization: [
		{
			weight: 3,
			terms: [
				'theme',
				'launcher',
				'icon pack',
				'wallpaper',
				'dynamic island',
				'always on display',
				'notification shade',
				'主题',
				'美化',
				'force dark',
				'dark mode',
				'customization',
				'customize'
			]
		},
		{ weight: 2, terms: ['widget', 'font', 'skin', 'style', 'indicator', '外观'] },
		{ weight: 1, terms: ['material you', 'design', 'appearance'] }
	],
	connectivity: [
		{
			weight: 3,
			terms: [
				'android auto',
				'headunit',
				'home assistant',
				'clipboard sync',
				'cast',
				'device-to-device',
				'bluetooth bridge',
				'投屏',
				'wear os',
				'wearos',
				'smartwatch'
			]
		},
		{ weight: 2, terms: ['ble', 'bluetooth', 'sync', 'remote control', 'companion', 'nearby'] },
		{ weight: 1, terms: ['connect', 'bridge'] }
	],
	system_tweaks: [
		{
			weight: 3,
			terms: [
				'status bar',
				'power menu',
				'dpi',
				'system settings',
				'immersive mode',
				'freeform',
				'window manager',
				'system ui',
				'系统设置',
				'link handling',
				'locale',
				'app languages',
				'multi window',
				'floating window',
				'multitasking',
				'desktop mode',
				'screen off',
				'息屏',
				'volume control',
				'folding state',
				'folding states',
				'power optimization',
				'gsi',
				'gsis',
				'background limit'
			]
		},
		{
			weight: 2,
			terms: ['tweak', 'modify system', 'system-level', 'settings', 'overlay', 'quick settings']
		},
		{ weight: 1, terms: ['toggle', 'enable', 'hide'] }
	],
	utilities: [
		{
			weight: 3,
			terms: [
				'toolbox',
				'工具箱',
				'alarm',
				'calculator',
				'clock',
				'notes',
				'converter',
				'system monitor',
				'task manager',
				'process manager',
				'running services'
			]
		},
		{ weight: 2, terms: ['utility', 'tool', 'helper', 'monitor'] },
		{ weight: 1, terms: ['simple', 'lightweight'] }
	]
};

const MECHANISM = new Set([
	'adb',
	'shell',
	'api',
	'sdk',
	'root',
	'binder',
	'runtime',
	'build',
	'debug',
	'script'
]);

const RELEASE_NOISE = new Set([
	'install',
	'installer',
	'update',
	'updates',
	'apk',
	'version',
	'store'
]);

const BOILERPLATE: RegExp[] = [
	/download[^.\n]{0,40}(apk|release)[^.\n]{0,30}/gi,
	/(grab|get) the apk[^.\n]{0,40}/gi,
	/(build|install) (from source|instructions?)[^.\n]{0,40}/gi,
	/\.\/gradlew[^\s]*/gi,
	/adb install[^\n]*/gi,
	/requires? (shizuku|root|android \d+)[^.\n]{0,30}/gi,
	/(powered|works?) (by|with) shizuku/gi,
	/minimum (sdk|android)[^.\n]{0,20}/gi
];

const NAMED_GAME =
	/(mobile legends|mlbb|codm|call of duty|umamusume|polyfield|genshin|mihoyo|hoyoverse|honkai|pubg|minecraft|roblox|maimai|chunithm|retroarch|arcaea|stardew[ -]valley|fate[/ -]grand[ -]order|last origin)/i;

const DEVICE_LOCK =
	/(only (works|for|on) [^.]{0,40}(samsung|xiaomi|quest|nothing|pixel|galaxy)|no other devices|for (samsung|xiaomi|quest|nothing) (phones|devices)|(quest|nothing os|hyperos|miui|inmo)[- ]native|supported samsung model|supported (google |samsung )?(pixel|galaxy|samsung)( galaxy)? (phones|devices|models?|firmware))/i;

const LIBRARY_DEPENDENCY =
	/(implementation|compileonly|api)\s*\(?\s*["'][a-z0-9_.-]+:[a-z0-9_.$-]+/;
const LIBRARY_WEIGHT = 6;

const PRECEDENCE: { winner: ListingCategory; rival: ListingCategory; conditions: string[] }[] = [
	{ winner: 'gaming', rival: 'media', conditions: ['game', 'games', 'visual novel', '游戏'] },
	{
		winner: 'gaming',
		rival: 'file_management',
		conditions: ['skin', 'map', 'game asset', 'mod']
	},
	{ winner: 'automation', rival: 'utilities', conditions: ['agent', 'automation', '自动'] },
	{
		winner: 'file_management',
		rival: 'networking',
		conditions: ['file transfer', '传输文件', '文件传输']
	},
	{ winner: 'app_management', rival: 'customization', conditions: ['disable', 'freeze'] },
	{ winner: 'networking', rival: 'privacy_security', conditions: ['vpn', 'proxy', 'tunnel'] },
	{
		winner: 'app_management',
		rival: 'system_tweaks',
		conditions: ['debloat', 'uninstall', 'freeze', 'hibernat']
	},
	{ winner: 'file_management', rival: 'connectivity', conditions: ['file', '文件'] },
	{
		winner: 'developer_tools',
		rival: 'system_tweaks',
		conditions: ['adb', 'developer options', 'terminal']
	},
	{
		winner: 'connectivity',
		rival: 'networking',
		conditions: ['bluetooth low energy', 'ble', 'pair', 'another device', 'controller device']
	}
];

// A game named once in passing ("works in games too") is not a gaming app, but
// a real one says it repeatedly. Measured on the labeled sample: requiring two
// readme mentions drops every false positive and costs one true positive, which
// its description still catches.
const AMBIGUOUS_README_TERMS = new Set(['game', 'games', 'gaming', 'emulator', 'handheld', '游戏']);
const MIN_AMBIGUOUS_README_HITS = 2;

const README_BUDGET = 1500;
const MIN_DESCRIPTION_LENGTH = 10;
const MIN_README_LENGTH = 40;

export type CategoryInput = {
	description: string | null;
	topics: string[];
	readme: string | null;
};

const MARKUP: RegExp[] = [
	/<!--[\s\S]*?-->/g,
	/<[^>]+>/g,
	/!\[[^\]]*\]\([^)]*\)/g,
	/\]\([^)]*\)/g,
	/^\s*\[[^\]]+\]:\s*\S+.*$/gm,
	/https?:\/\/\S+/g,
	/[*`]+/g
];

function stripMarkup(text: string): string {
	return MARKUP.reduce((current, pattern) => current.replace(pattern, ' '), text);
}

function stripBoilerplate(text: string): string {
	return BOILERPLATE.reduce((current, pattern) => current.replace(pattern, ' '), text);
}

function termSource(term: string): string {
	return term.replace(/[.*+?^${}()|[\]\\]/g, '\\$&').replace(/[ -]/g, '[\\s-]');
}

function countTerm(term: string, text: string): number {
	const escaped = termSource(term);
	const pattern = /[a-z0-9]/.test(term)
		? new RegExp(`(?<![a-z0-9])${escaped}(?![a-z0-9])`, 'g')
		: new RegExp(escaped, 'g');

	return text.match(pattern)?.length ?? 0;
}

function hasTerm(term: string, text: string): boolean {
	if (!/[a-z0-9]/.test(term)) return text.includes(term);

	const escaped = termSource(term);
	return new RegExp(`(?<![a-z0-9])${escaped}(?![a-z0-9])`).test(text);
}

function startsWord(prefix: string, text: string): boolean {
	if (!/[a-z0-9]/.test(prefix)) return text.includes(prefix);

	return new RegExp(`(?<![a-z0-9])${termSource(prefix)}`).test(text);
}

function zones(input: CategoryInput): { purpose: string; readme: string } {
	const description = (input.description ?? '').toLowerCase();
	const topics = input.topics.join(' ').toLowerCase();
	const readme = stripBoilerplate(
		stripMarkup(input.readme ?? '')
			.replace(/\s+/g, ' ')
			.slice(0, README_BUDGET)
			.toLowerCase()
	);

	return { purpose: `${description} ${topics}`, readme };
}

function scoreCategories(input: CategoryInput): Map<ListingCategory, number> {
	const { purpose, readme } = zones(input);
	const scores = new Map<ListingCategory, number>();

	for (const [category, tiers] of Object.entries(RULES) as [ListingCategory, Tier[]][]) {
		let total = 0;
		let matches = 0;
		let hasStrong = false;

		for (const tier of tiers) {
			for (const term of tier.terms) {
				const inPurpose = hasTerm(term, purpose);
				const inReadme =
					category === 'device_specific'
						? false
						: AMBIGUOUS_README_TERMS.has(term)
							? countTerm(term, readme) >= MIN_AMBIGUOUS_README_HITS
							: hasTerm(term, readme);
				if (!inPurpose && !inReadme) continue;
				if (RELEASE_NOISE.has(term) && !inPurpose) continue;

				let weight = inPurpose ? tier.weight * 2 : tier.weight;
				if (MECHANISM.has(term) && category !== 'developer_tools') {
					weight = Math.max(1, Math.floor(weight / 2));
				}

				total += weight;
				matches += 1;
				if (weight >= 3) hasStrong = true;
			}
		}

		if (total === 0) continue;
		if (total < 4 && !hasStrong && matches < 2) continue;
		scores.set(category, total);
	}

	if (LIBRARY_DEPENDENCY.test((input.readme ?? '').toLowerCase())) {
		scores.set('developer_tools', (scores.get('developer_tools') ?? 0) + LIBRARY_WEIGHT);
	} else if (scores.has('developer_tools')) {
		const named = RULES.developer_tools.some((tier) =>
			tier.terms.some((term) => hasTerm(term, purpose))
		);
		if (!named) scores.delete('developer_tools');
	}

	return scores;
}

export function categorize(input: CategoryInput): ListingCategory | null {
	const description = (input.description ?? '').trim();
	const readme = (input.readme ?? '').trim();

	const hasEvidence =
		description.length >= MIN_DESCRIPTION_LENGTH ||
		readme.length >= MIN_README_LENGTH ||
		input.topics.length > 0;
	if (!hasEvidence) return null;

	const { purpose, readme: body } = zones(input);
	const text = `${purpose} ${body}`;

	if (NAMED_GAME.test(text)) return 'gaming';
	if (DEVICE_LOCK.test(purpose) || DEVICE_LOCK.test(text)) return 'device_specific';

	const scores = scoreCategories(input);
	if (scores.size === 0) return null;

	const ranked = [...scores.entries()].sort((left, right) => right[1] - left[1]);
	const [top] = ranked;
	if (top === undefined) return null;

	const runner = ranked[1];
	if (runner !== undefined) {
		for (const rule of PRECEDENCE) {
			const pair = new Set([top[0], runner[0]]);
			if (pair.size !== 2 || !pair.has(rule.winner) || !pair.has(rule.rival)) continue;
			if (!rule.conditions.some((condition) => startsWord(condition, text))) continue;
			return rule.winner;
		}
	}

	return top[0];
}
