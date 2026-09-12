import { isAndroidStructurePath } from './queries.ts';
import type { EvidenceKind, ListingConfidence } from '@yuki/db/schema';

export type DetectedEvidence = {
	kind: EvidenceKind;
	detail: string | null;
};

export function hasAndroidStructure(paths: string[]): boolean {
	return paths.some(isAndroidStructurePath);
}

const STRONG_KINDS: EvidenceKind[] = ['provider_class', 'gradle_dependency'];
const PROBABLE_KINDS: EvidenceKind[] = ['legacy_gradle_dependency', 'source_filename'];

export function mergeEvidence(evidence: DetectedEvidence[]): DetectedEvidence[] {
	const byKind = new Map<EvidenceKind, DetectedEvidence>();
	for (const entry of evidence) {
		if (!byKind.has(entry.kind)) {
			byKind.set(entry.kind, entry);
		}
	}
	return [...byKind.values()];
}

const SELF_DECLARED_KINDS: EvidenceKind[] = ['repository_topic', 'readme_mention'];

export function isSelfDeclaredOnly(evidence: DetectedEvidence[]): boolean {
	if (evidence.length === 0) return true;
	return evidence.every((entry) => SELF_DECLARED_KINDS.includes(entry.kind));
}

export function scoreConfidence(evidence: DetectedEvidence[]): ListingConfidence {
	const kinds = new Set(evidence.map((entry) => entry.kind));

	if (STRONG_KINDS.some((kind) => kinds.has(kind))) return 'strong';
	if (PROBABLE_KINDS.some((kind) => kinds.has(kind))) return 'probable';
	return 'weak';
}
