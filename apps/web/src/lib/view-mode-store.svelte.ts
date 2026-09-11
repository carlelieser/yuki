import { getContext, setContext } from 'svelte';
import { DEFAULT_VIEW_MODE, VIEW_MODE_COOKIE, type ViewMode } from './view-mode.ts';

const CONTEXT_KEY = Symbol('view-mode');
const COOKIE_MAX_AGE = 60 * 60 * 24 * 365;

class ViewModeStore {
	#chosen = $state<ViewMode>(DEFAULT_VIEW_MODE);
	#hasChosen = $state(false);
	#stored: () => ViewMode;

	constructor(stored: () => ViewMode) {
		this.#stored = stored;
	}

	get mode(): ViewMode {
		return this.#hasChosen ? this.#chosen : this.#stored();
	}

	choose(mode: ViewMode): void {
		this.#chosen = mode;
		this.#hasChosen = true;
		document.cookie = `${VIEW_MODE_COOKIE}=${mode}; path=/; max-age=${COOKIE_MAX_AGE}; samesite=lax`;
	}
}

export function createViewModeStore(stored: () => ViewMode): ViewModeStore {
	return setContext(CONTEXT_KEY, new ViewModeStore(stored));
}

export function getViewModeStore(): ViewModeStore {
	return getContext<ViewModeStore>(CONTEXT_KEY);
}
