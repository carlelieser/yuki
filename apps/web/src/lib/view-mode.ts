export const VIEW_MODE_COOKIE = 'yuki:view-mode';

export const VIEW_MODES = ['grid', 'list'] as const;

export type ViewMode = (typeof VIEW_MODES)[number];

export const DEFAULT_VIEW_MODE: ViewMode = 'grid';

export function parseViewMode(raw: string | null): ViewMode {
	if (raw === null) return DEFAULT_VIEW_MODE;

	return VIEW_MODES.find((mode) => mode === raw.trim()) ?? DEFAULT_VIEW_MODE;
}
