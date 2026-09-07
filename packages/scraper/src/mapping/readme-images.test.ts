import { describe, expect, it } from 'vitest';
import { MAX_SCREENSHOTS, extractReadmeImages, resolveImageUrl } from './readme-images.ts';

const owner = 'acme';
const name = 'app';
const branch = 'main';

function extract(markdown: string) {
	return extractReadmeImages(markdown, owner, name, branch);
}

describe('resolveImageUrl', () => {
	it('resolves a relative path against raw.githubusercontent.com', () => {
		expect(resolveImageUrl('docs/shot.png', owner, name, branch)).toBe(
			'https://raw.githubusercontent.com/acme/app/main/docs/shot.png'
		);
	});

	it('normalises leading ./ and / the same way', () => {
		expect(resolveImageUrl('./docs/shot.png', owner, name, branch)).toBe(
			'https://raw.githubusercontent.com/acme/app/main/docs/shot.png'
		);
		expect(resolveImageUrl('/docs/shot.png', owner, name, branch)).toBe(
			'https://raw.githubusercontent.com/acme/app/main/docs/shot.png'
		);
	});

	it('leaves absolute urls alone and upgrades protocol-relative ones', () => {
		expect(resolveImageUrl('https://cdn.example.com/a.png', owner, name, branch)).toBe(
			'https://cdn.example.com/a.png'
		);
		expect(resolveImageUrl('//cdn.example.com/a.png', owner, name, branch)).toBe(
			'https://cdn.example.com/a.png'
		);
	});

	it('rejects data uris and anchors', () => {
		expect(resolveImageUrl('data:image/png;base64,AAAA', owner, name, branch)).toBeNull();
		expect(resolveImageUrl('#section', owner, name, branch)).toBeNull();
	});
});

describe('extractReadmeImages', () => {
	it('rejects shields, badgen, anything named badge, and svgs', () => {
		const images = extract(`
			![build](https://img.shields.io/badge/build-passing-green)
			![version](https://badgen.net/npm/v/app)
			![status](https://example.com/my-badge-thing.png)
			![logo](docs/logo.svg)
			![shot](docs/shot.png)
		`);

		expect(images.map((image) => image.url)).toEqual([
			'https://raw.githubusercontent.com/acme/app/main/docs/shot.png'
		]);
	});

	it('uses only screenshot-named images when any are present', () => {
		const images = extract(`
			![logo](docs/logo.png)
			![one](docs/screenshots/one.png)
			![two](docs/preview/two.png)
		`);

		expect(images.map((image) => image.url)).toEqual([
			'https://raw.githubusercontent.com/acme/app/main/docs/screenshots/one.png',
			'https://raw.githubusercontent.com/acme/app/main/docs/preview/two.png'
		]);
	});

	it('falls back to every image when none is screenshot-named', () => {
		const images = extract('![logo](docs/logo.png)\n![diagram](docs/diagram.png)');

		expect(images).toHaveLength(2);
	});

	it('caps the result and keeps positions dense from zero', () => {
		const markdown = Array.from(
			{ length: MAX_SCREENSHOTS + 4 },
			(_, index) => `![shot ${index}](docs/shot-${index}.png)`
		).join('\n');

		const images = extract(markdown);

		expect(images).toHaveLength(MAX_SCREENSHOTS);
		expect(images.map((image) => image.position)).toEqual([0, 1, 2, 3, 4, 5, 6, 7]);
	});

	it('reads html img tags and their alt text', () => {
		const images = extract('<img src="docs/shot.png" alt="Home screen" width="200">');

		expect(images).toEqual([
			{
				url: 'https://raw.githubusercontent.com/acme/app/main/docs/shot.png',
				alt: 'Home screen',
				position: 0
			}
		]);
	});

	it('keeps markdown alt text and nulls an empty one', () => {
		const images = extract('![Home](docs/a.png)\n![](docs/b.png)');

		expect(images.map((image) => image.alt)).toEqual(['Home', null]);
	});

	it('deduplicates a repeated image', () => {
		const images = extract('![a](docs/shot.png)\n![b](./docs/shot.png)');

		expect(images).toHaveLength(1);
	});

	it('returns nothing for a readme with no images', () => {
		expect(extract('# Title\n\nSome prose.')).toEqual([]);
	});
});
