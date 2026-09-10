import { getContext, setContext } from 'svelte';
import { resolve } from '$app/paths';
import {
	ARCHITECTURE_PREFERENCE_KEY,
	parseArchitecturePreference,
	type ArchitecturePreference
} from './architecture-preference.ts';

const CONTEXT_KEY = Symbol('architecture-preference');

class ArchitectureStore {
	#chosen = $state<ArchitecturePreference>(null);
	#hasChosen = $state(false);
	#stored: () => ArchitecturePreference;
	#isSignedIn: () => boolean;

	constructor(stored: () => ArchitecturePreference, isSignedIn: () => boolean) {
		this.#stored = stored;
		this.#isSignedIn = isSignedIn;
	}

	get preference(): ArchitecturePreference {
		return this.#hasChosen ? this.#chosen : this.#stored();
	}

	loadLocal(): void {
		if (this.#isSignedIn()) return;

		try {
			this.#chosen = parseArchitecturePreference(localStorage.getItem(ARCHITECTURE_PREFERENCE_KEY));
			this.#hasChosen = true;
		} catch {
			return;
		}
	}

	choose(architecture: ArchitecturePreference): void {
		this.#chosen = architecture;
		this.#hasChosen = true;

		if (this.#isSignedIn()) {
			void this.#save(architecture);
			return;
		}

		this.#writeLocal(architecture);
	}

	#writeLocal(architecture: ArchitecturePreference): void {
		try {
			if (architecture === null) {
				localStorage.removeItem(ARCHITECTURE_PREFERENCE_KEY);
				return;
			}

			localStorage.setItem(ARCHITECTURE_PREFERENCE_KEY, architecture);
		} catch {
			return;
		}
	}

	async #save(architecture: ArchitecturePreference): Promise<void> {
		try {
			await fetch(resolve('/api/preferences/architecture'), {
				method: 'POST',
				headers: { 'content-type': 'application/json' },
				body: JSON.stringify({ architecture })
			});
		} catch {
			return;
		}
	}
}

export function createArchitectureStore(
	stored: () => ArchitecturePreference,
	isSignedIn: () => boolean
): ArchitectureStore {
	return setContext(CONTEXT_KEY, new ArchitectureStore(stored, isSignedIn));
}

export function getArchitectureStore(): ArchitectureStore {
	return getContext<ArchitectureStore>(CONTEXT_KEY);
}
