import type { ReviewCandidate } from '../persistence/review.ts';

export type ReviewCommand =
	| { kind: 'pending'; limit: number }
	| { kind: 'published'; limit: number }
	| { kind: 'publish'; slugs: string[] }
	| { kind: 'unpublish'; slugs: string[] }
	| { kind: 'help' }
	| { kind: 'invalid'; reason: string };

const DEFAULT_LIMIT = 25;

export function parseArgs(argv: string[]): ReviewCommand {
	const [command, ...rest] = argv.filter((value) => value !== '');

	if (command === undefined || command === 'help' || command === '--help') {
		return { kind: 'help' };
	}

	if (command === 'pending' || command === 'published') {
		const limitFlag = rest.find((value) => value.startsWith('--limit='));
		const parsed =
			limitFlag === undefined
				? DEFAULT_LIMIT
				: Number.parseInt(limitFlag.slice('--limit='.length), 10);

		if (!Number.isFinite(parsed) || parsed <= 0) {
			return { kind: 'invalid', reason: `--limit needs a positive number` };
		}

		return { kind: command, limit: parsed };
	}

	if (command === 'publish' || command === 'unpublish') {
		const slugs = rest.filter((value) => !value.startsWith('--'));
		if (slugs.length === 0) {
			return { kind: 'invalid', reason: `${command} needs at least one slug` };
		}
		return { kind: command, slugs };
	}

	return { kind: 'invalid', reason: `Unknown command "${command}"` };
}

export function formatHelp(): string {
	return [
		'Usage: bun run listings <command>',
		'',
		'  pending [--limit=N]      Listings awaiting review',
		'  published [--limit=N]    Listings currently on the storefront',
		'  publish <slug>...        Put listings on the storefront',
		'  unpublish <slug>...      Take listings back off',
		''
	].join('\n');
}

function formatFlags(candidate: ReviewCandidate): string {
	const flags: string[] = [];
	if (candidate.isArchived) flags.push('archived');
	if (candidate.isFork) flags.push('fork');
	if (candidate.iconUrl === null) flags.push('no icon');
	if (candidate.bannerUrl === null) flags.push('no banner');
	if (candidate.screenshotCount === 0) flags.push('no screenshots');
	return flags.length === 0 ? '' : `  [${flags.join(', ')}]`;
}

export function formatCandidate(candidate: ReviewCandidate): string {
	const lines = [
		`  ${candidate.slug}`,
		`    ${candidate.title} — ${candidate.stars}★  ${candidate.confidence}  ${candidate.license ?? 'no license'}${formatFlags(candidate)}`
	];

	if (candidate.description !== null) {
		lines.push(`    ${candidate.description}`);
	}

	for (const entry of candidate.evidence) {
		lines.push(`    ${entry.kind}${entry.detail === null ? '' : `: ${entry.detail}`}`);
	}

	lines.push(
		`    ${candidate.screenshotCount} screenshots, ${candidate.versionCount} versions`,
		`    ${candidate.repositoryUrl}`
	);

	return lines.join('\n');
}

export function formatCandidateList(candidates: ReviewCandidate[], emptyMessage: string): string {
	if (candidates.length === 0) return emptyMessage;

	return candidates.map(formatCandidate).join('\n\n');
}
