export function readListFlag(argv: string[], flag: string): string[] {
	const prefix = `${flag}=`;

	return argv
		.filter((value) => value.startsWith(prefix))
		.flatMap((value) => value.slice(prefix.length).split(','))
		.map((value) => value.trim())
		.filter((value) => value !== '');
}

export function findMissingSlugs(requested: string[], matched: string[]): string[] {
	const found = new Set(matched);
	return requested.filter((slug) => !found.has(slug));
}
