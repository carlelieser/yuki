export type Fidelity = { unresolved: boolean; reasons: string[] };

const MAX_REASONS = 8;

export function createFidelity(): Fidelity {
	return { unresolved: false, reasons: [] };
}

export function markUnresolved(fidelity: Fidelity, reason: string): void {
	fidelity.unresolved = true;
	if (fidelity.reasons.length < MAX_REASONS && !fidelity.reasons.includes(reason)) {
		fidelity.reasons.push(reason);
	}
}
