export type NumberFormatPreset = 'decimal' | 'integer' | 'compact' | 'percent' | 'currency';

export type NumberFormatOptions = Intl.NumberFormatOptions & {
	preset?: NumberFormatPreset;
	locale?: string | string[];
};

const presets = {
	decimal: {},
	integer: { maximumFractionDigits: 0 },
	compact: { notation: 'compact', maximumFractionDigits: 1 },
	percent: { style: 'percent', maximumFractionDigits: 0 },
	currency: { style: 'currency', currencyDisplay: 'narrowSymbol' }
} satisfies Record<NumberFormatPreset, Intl.NumberFormatOptions>;

const cache = new Map<string, Intl.NumberFormat>();

function defined<T extends object>(options: T): T {
	return Object.fromEntries(
		Object.entries(options).filter(([, value]) => value !== undefined)
	) as T;
}

export function formatNumber(value: number | bigint, options: NumberFormatOptions = {}) {
	const { preset = 'decimal', locale, ...overrides } = defined(options);
	const key = `${JSON.stringify(locale ?? null)}|${preset}|${JSON.stringify(overrides)}`;

	let formatter = cache.get(key);
	if (!formatter) {
		formatter = new Intl.NumberFormat(locale, { ...presets[preset], ...overrides });
		cache.set(key, formatter);
	}

	return formatter.format(value);
}
