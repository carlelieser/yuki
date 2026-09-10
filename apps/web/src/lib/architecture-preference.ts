import { parseArchitecture, type Architecture } from '@yuki/github';

export const ARCHITECTURE_PREFERENCE_KEY = 'yuki:architecture';

export type ArchitecturePreference = Architecture | null;

export function parseArchitecturePreference(raw: string | null): ArchitecturePreference {
	if (raw === null) return null;

	return parseArchitecture(raw.trim());
}

export function selectedArchitecture(
	preference: ArchitecturePreference,
	available: Architecture[] | null
): ArchitecturePreference {
	if (preference === null) return null;
	if (available === null) return preference;

	return available.includes(preference) ? preference : null;
}
