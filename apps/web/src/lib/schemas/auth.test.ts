import { describe, expect, it } from 'vitest';
import { forgotPasswordSchema, resetPasswordSchema, signInSchema, signUpSchema } from './auth.ts';

describe('signUpSchema', () => {
	it('accepts a complete submission', () => {
		const result = signUpSchema.safeParse({
			name: 'Ada',
			email: 'ada@example.com',
			password: 'correct horse'
		});

		expect(result.success).toBe(true);
		expect(result.data).toEqual({
			name: 'Ada',
			email: 'ada@example.com',
			password: 'correct horse'
		});
	});

	it('never surfaces the submitted password in the error tree', () => {
		const result = signUpSchema.safeParse({
			name: '',
			email: 'not-an-email',
			password: 'hunter2-secret'
		});

		expect(result.success).toBe(false);
		expect(JSON.stringify(result.error?.issues)).not.toContain('hunter2-secret');
	});

	it('rejects short passwords', () => {
		const result = signUpSchema.safeParse({
			name: 'Ada',
			email: 'ada@example.com',
			password: 'short'
		});

		expect(result.success).toBe(false);
		expect(result.error?.issues.find((issue) => issue.path[0] === 'password')?.message).toContain(
			'at least 8'
		);
	});

	it('trims surrounding whitespace from the name and email', () => {
		const result = signUpSchema.safeParse({
			name: '  Ada  ',
			email: '  ada@example.com  ',
			password: 'correct horse'
		});

		expect(result.success).toBe(true);
		expect(result.data?.name).toBe('Ada');
		expect(result.data?.email).toBe('ada@example.com');
	});

	it('requires a name', () => {
		const result = signUpSchema.safeParse({
			name: '   ',
			email: 'ada@example.com',
			password: 'correct horse'
		});

		expect(result.success).toBe(false);
		expect(result.error?.issues.find((issue) => issue.path[0] === 'name')?.message).toBe(
			'Name is required.'
		);
	});
});

describe('signInSchema', () => {
	it('accepts an email and password pair', () => {
		const result = signInSchema.safeParse({
			email: 'ada@example.com',
			password: 'secret-value',
			redirectTo: '/'
		});

		expect(result.success).toBe(true);
	});

	it('reports a missing password', () => {
		const result = signInSchema.safeParse({ email: 'ada@example.com', password: '' });

		expect(result.success).toBe(false);
		expect(result.error?.issues.find((issue) => issue.path[0] === 'password')?.message).toBe(
			'Password is required.'
		);
	});

	it('defaults redirectTo to the root', () => {
		const result = signInSchema.safeParse({
			email: 'ada@example.com',
			password: 'secret-value'
		});

		expect(result.data?.redirectTo).toBe('/');
	});
});

describe('forgotPasswordSchema', () => {
	it('requires an email', () => {
		expect(forgotPasswordSchema.safeParse({ email: '' }).success).toBe(false);
		expect(forgotPasswordSchema.safeParse({ email: 'ada@example.com' }).success).toBe(true);
	});
});

describe('resetPasswordSchema', () => {
	it('accepts a matching confirmation', () => {
		const result = resetPasswordSchema.safeParse({
			token: 'reset-token',
			password: 'correct horse',
			confirmPassword: 'correct horse'
		});

		expect(result.success).toBe(true);
	});

	it('reports a mismatch on the confirmation field', () => {
		const result = resetPasswordSchema.safeParse({
			token: 'reset-token',
			password: 'correct horse',
			confirmPassword: 'different horse'
		});

		expect(result.success).toBe(false);
		expect(result.error?.issues[0]?.path).toEqual(['confirmPassword']);
		expect(result.error?.issues[0]?.message).toBe('Passwords do not match.');
	});
});
