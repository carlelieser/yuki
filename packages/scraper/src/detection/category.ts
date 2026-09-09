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
				'controller'
			]
		},
		{
			weight: 2,
			terms: [
				'game',
				'games',
				'codm',
				'mobile legends',
				'retroarch',
				'handheld',
				'emulator',
				'游戏'
			]
		},
		{ weight: 1, terms: ['tuning', 'resolution', 'boost', 'xbox', 'play'] }
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
				'yt-dlp'
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
				'语音自动化'
			]
		},
		{
			weight: 2,
			terms: ['llm', 'accessibility service', 'rule', 'workflow', '智能体', '自然语言', '无障碍']
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
				'应用管理',
				'app catalog'
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
				'magisk module'
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
				'文件共享'
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
				'代理'
			]
		},
		{ weight: 2, terms: ['dns', 'traffic', 'telephony', 'cellular', 'sim', 'carrier', '网络'] },
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
				'anti-theft',
				'permission audit',
				'shoulder surfing',
				'隐私',
				'sandbox'
			]
		},
		{ weight: 2, terms: ['secure', 'security', 'encrypt', 'block ads', 'spoof', 'audit', '权限'] },
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
				'debugger'
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
				'linux terminal'
			]
		},
		{ weight: 1, terms: ['sdk', 'toolchain'] }
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
				'quest'
			]
		},
		{ weight: 2, terms: ['oem', 'one ui', 'coloros', 'realme', 'oppo', 'vivo', 'honor'] },
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
				'dark mode'
			]
		},
		{ weight: 2, terms: ['material you', 'widget', 'font', 'skin', 'style', '外观'] },
		{ weight: 1, terms: ['design', 'appearance'] }
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
				'投屏'
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
				'link handling'
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
			terms: ['toolbox', '工具箱', 'alarm', 'calculator', 'clock', 'notes', 'converter']
		},
		{ weight: 2, terms: ['utility', 'tool', 'helper'] },
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
	/(mobile legends|mlbb|codm|call of duty|umamusume|polyfield|genshin|pubg|minecraft|roblox|maimai|chunithm|retroarch|arcaea)/i;

const DEVICE_LOCK =
	/(only (works|for|on) [^.]{0,40}(samsung|xiaomi|quest|nothing|pixel|galaxy)|no other devices|for (samsung|xiaomi|quest|nothing) (phones|devices)|(quest|nothing os|hyperos|miui|inmo)[- ]native|supported samsung model)/i;

const PRECEDENCE: { winner: ListingCategory; rival: ListingCategory; conditions: string[] }[] = [
	{ winner: 'gaming', rival: 'media', conditions: ['game', 'games', 'visual novel', '游戏'] },
	{
		winner: 'gaming',
		rival: 'file_management',
		conditions: ['skin', 'map', 'game asset', 'mod']
	},
	{ winner: 'automation', rival: 'utilities', conditions: ['agent', 'automation', '自动'] },
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

const README_BUDGET = 1500;
const MIN_DESCRIPTION_LENGTH = 10;
const MIN_README_LENGTH = 40;

export type CategoryInput = {
	description: string | null;
	topics: string[];
	readme: string | null;
};

function stripBoilerplate(text: string): string {
	return BOILERPLATE.reduce((current, pattern) => current.replace(pattern, ' '), text);
}

function hasTerm(term: string, text: string): boolean {
	if (!/[a-z0-9]/.test(term)) return text.includes(term);

	const escaped = term.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
	return new RegExp(`(?<![a-z0-9])${escaped}(?![a-z0-9])`).test(text);
}

function zones(input: CategoryInput): { purpose: string; readme: string } {
	const description = (input.description ?? '').toLowerCase();
	const topics = input.topics.join(' ').toLowerCase();
	const readme = stripBoilerplate((input.readme ?? '').slice(0, README_BUDGET).toLowerCase());

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
				const inReadme = category === 'device_specific' ? false : hasTerm(term, readme);
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

	if (scores.has('developer_tools')) {
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
			if (!rule.conditions.some((condition) => text.includes(condition))) continue;
			return rule.winner;
		}
	}

	return top[0];
}
