import { z } from 'zod';

export const MIN_PASSWORD_LENGTH = 8;

const email = z
	.string()
	.trim()
	.min(1, 'Email is required.')
	.pipe(z.email('Enter a valid email address.'));
const requiredPassword = z.string().min(1, 'Password is required.');
const newPassword = z
	.string()
	.min(MIN_PASSWORD_LENGTH, `Password must be at least ${MIN_PASSWORD_LENGTH} characters.`);

export const signInSchema = z.object({
	email,
	password: requiredPassword,
	redirectTo: z.string().default('/')
});

export const signUpSchema = z.object({
	name: z.string().trim().min(1, 'Name is required.'),
	email,
	password: newPassword
});

export const forgotPasswordSchema = z.object({ email });

export const resetPasswordSchema = z
	.object({
		token: z.string(),
		password: newPassword,
		confirmPassword: z.string()
	})
	.refine((values) => values.password === values.confirmPassword, {
		message: 'Passwords do not match.',
		path: ['confirmPassword']
	});

export type SignInInput = z.infer<typeof signInSchema>;
export type SignUpInput = z.infer<typeof signUpSchema>;
export type ForgotPasswordInput = z.infer<typeof forgotPasswordSchema>;
export type ResetPasswordInput = z.infer<typeof resetPasswordSchema>;
