export function safeRedirectTo(value: unknown): string {
	if (typeof value !== 'string' || value === '') return '/';
	if (!value.startsWith('/') || value.startsWith('//')) return '/';
	if (value.startsWith('/\\')) return '/';
	return value;
}
