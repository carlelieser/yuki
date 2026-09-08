export function initialsOf(name: string, fallback?: string): string {
	const initials = name
		.split(' ')
		.filter(Boolean)
		.slice(0, 2)
		.map((part) => part[0]?.toUpperCase() ?? '')
		.join('');

	return initials || fallback?.[0]?.toUpperCase() || '?';
}
