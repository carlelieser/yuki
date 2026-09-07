export type FieldErrors = Record<string, string>;

export type ParseResult<T> =
	{ ok: true; value: T } | { ok: false; email: string; errors: FieldErrors };

export type SignInInput = { email: string; password: string };
export type SignUpInput = { name: string; email: string; password: string };

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const MIN_PASSWORD_LENGTH = 8;

function readString(data: FormData, key: string): string {
	const value = data.get(key);
	return typeof value === 'string' ? value.trim() : '';
}

function validateEmail(email: string, errors: FieldErrors): void {
	if (!email) {
		errors.email = 'Email is required.';
	} else if (!EMAIL_PATTERN.test(email)) {
		errors.email = 'Enter a valid email address.';
	}
}

export function parseSignIn(data: FormData): ParseResult<SignInInput> {
	const email = readString(data, 'email');
	const password = data.get('password');
	const errors: FieldErrors = {};

	validateEmail(email, errors);
	if (typeof password !== 'string' || password.length === 0) {
		errors.password = 'Password is required.';
	}

	if (Object.keys(errors).length > 0) {
		return { ok: false, email, errors };
	}

	return { ok: true, value: { email, password: password as string } };
}

export function parseSignUp(data: FormData): ParseResult<SignUpInput> {
	const name = readString(data, 'name');
	const email = readString(data, 'email');
	const password = data.get('password');
	const errors: FieldErrors = {};

	if (!name) {
		errors.name = 'Name is required.';
	}

	validateEmail(email, errors);

	if (typeof password !== 'string' || password.length === 0) {
		errors.password = 'Password is required.';
	} else if (password.length < MIN_PASSWORD_LENGTH) {
		errors.password = `Password must be at least ${MIN_PASSWORD_LENGTH} characters.`;
	}

	if (Object.keys(errors).length > 0) {
		return { ok: false, email, errors };
	}

	return { ok: true, value: { name, email, password: password as string } };
}

export function safeRedirectTo(value: unknown): string {
	if (typeof value !== 'string' || value === '') return '/';
	if (!value.startsWith('/') || value.startsWith('//')) return '/';
	if (value.startsWith('/\\')) return '/';
	return value;
}
