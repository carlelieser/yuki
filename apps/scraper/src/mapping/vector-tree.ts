const ELEMENT = /<(\/?)(vector|group|path|clip-path|image)\b([^>]*?)(\/?)>/g;

const MAX_NODES = 256;
const MAX_DEPTH = 32;

export type VectorPath = { tag: string; body: string };

export type VectorGroup = {
	tag: string;
	clips: string[];
	children: VectorNode[];
};

export type VectorNode =
	| { kind: 'group'; group: VectorGroup }
	| { kind: 'path'; path: VectorPath }
	| { kind: 'image'; tag: string };

function bodyOf(source: string, from: number, name: string): { body: string; end: number } {
	const close = `</${name}>`;
	const at = source.indexOf(close, from);
	if (at === -1) return { body: '', end: source.length };

	return { body: source.slice(from, at), end: at + close.length };
}

export function parseVectorTree(source: string): VectorGroup | null {
	const root: VectorGroup = { tag: '', clips: [], children: [] };
	const stack: VectorGroup[] = [root];

	let nodes = 0;
	ELEMENT.lastIndex = 0;

	for (let match = ELEMENT.exec(source); match !== null; match = ELEMENT.exec(source)) {
		const isClosing = match[1] === '/';
		const name = match[2] ?? '';
		const attributes = match[3] ?? '';
		const isSelfClosing = match[4] === '/';
		const current = stack[stack.length - 1];
		if (current === undefined) return null;

		if (name === 'vector') continue;

		if (name === 'clip-path') {
			const data = attributes.match(/android:pathData="([^"]*)"/)?.[1];
			if (data !== undefined) current.clips.push(data);
			continue;
		}

		if (name === 'image') {
			if (isClosing) continue;
			if (nodes >= MAX_NODES) break;
			nodes += 1;

			current.children.push({ kind: 'image', tag: match[0] });
			continue;
		}

		if (name === 'path') {
			if (isClosing) continue;
			if (nodes >= MAX_NODES) break;
			nodes += 1;

			const body = isSelfClosing ? '' : bodyOf(source, match.index + match[0].length, 'path').body;
			current.children.push({ kind: 'path', path: { tag: match[0], body } });
			continue;
		}

		if (isClosing) {
			if (stack.length > 1) stack.pop();
			continue;
		}

		if (nodes >= MAX_NODES || stack.length >= MAX_DEPTH) break;
		nodes += 1;

		const group: VectorGroup = { tag: match[0], clips: [], children: [] };
		current.children.push({ kind: 'group', group });
		if (!isSelfClosing) stack.push(group);
	}

	return root;
}
