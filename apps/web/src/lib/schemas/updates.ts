import { z } from 'zod';

export const MAX_UPDATE_CHECK_ENTRIES = 500;

const packageNameSchema = z
	.string()
	.trim()
	.min(1)
	.max(255)
	.regex(/^[a-z][a-z0-9_]*(\.[a-z0-9_]+)+$/i, 'Provide a valid Android package name.');

export const installedAppSchema = z.object({
	packageName: packageNameSchema,
	versionTag: z.string().trim().min(1).max(255),
	versionCode: z.string().trim().max(32).nullable().default(null)
});

export const updateCheckSchema = z.object({
	installed: z
		.array(installedAppSchema)
		.max(MAX_UPDATE_CHECK_ENTRIES, 'Too many installed apps in one request.')
		.default([]),
	includePrereleases: z.boolean().default(false)
});

export type InstalledApp = z.infer<typeof installedAppSchema>;
export type UpdateCheckInput = z.infer<typeof updateCheckSchema>;
