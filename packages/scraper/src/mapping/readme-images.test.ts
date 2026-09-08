import { describe, expect, it } from 'vitest';
import {
	MAX_SCREENSHOTS,
	extractReadmeImages,
	findBannerUrl,
	resolveImageUrl
} from './readme-images.ts';

const owner = 'acme';
const name = 'app';
const branch = 'main';

function extract(markdown: string, exclude: string | null = null) {
	return extractReadmeImages(markdown, owner, name, branch, exclude);
}

function banner(markdown: string) {
	return findBannerUrl(markdown, owner, name, branch);
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
		const images = extract('![chart](docs/chart.png)\n![diagram](docs/diagram.png)');

		expect(images).toHaveLength(2);
	});

	it('rejects store and donation buttons, which are chrome rather than screenshots', () => {
		const images = extract(`
			![Get it on F-Droid](.github/resources/fdroid-button.png)
			![Get it on IzzyOnDroid](.github/resources/izzyondroid-button.png)
			![Buy me a coffee](.github/resources/bmc-button.png)
			![Features](.github/resources/features.png)
		`);

		expect(images.map((image) => image.url)).toEqual([
			'https://raw.githubusercontent.com/acme/app/main/.github/resources/features.png'
		]);
	});

	it('rejects store and donation badges that carry no chrome word', () => {
		const images = extract(`
			![IzzyOnDroid](assets/IzzyOnDroid.png)
			![Ko-fi](assets/kofi1.png?v=2)
			![Play Store](assets/get-it-on-google-play.png)
			![Home](assets/home.png)
		`);

		expect(images.map((image) => image.url)).toEqual([
			'https://raw.githubusercontent.com/acme/app/main/assets/home.png'
		]);
	});

	it('rejects telegram group invites, including theme-swapped pairs', () => {
		const images = extract(`
			<img src="./source/tg_group_dark.png#gh-dark-mode-only" />
			<img src="./source/tg_group_light.png#gh-light-mode-only" />
			![Main](source/main-screen.png)
		`);

		expect(images.map((image) => image.url)).toEqual([
			'https://raw.githubusercontent.com/acme/app/main/source/main-screen.png'
		]);
	});

	it('keeps images whose name merely mentions a platform without inviting to it', () => {
		const images = extract('![cards](docs/operit2-matrix-cards-zh-cn.png)');

		expect(images.map((image) => image.url)).toEqual([
			'https://raw.githubusercontent.com/acme/app/main/docs/operit2-matrix-cards-zh-cn.png'
		]);
	});

	it('rejects launcher icons used as readme headers', () => {
		expect(extract('![app](art/ic_launcher-web.png)')).toEqual([]);
	});

	it('rejects logos and icons even when nothing else is present', () => {
		expect(extract('![logo](docs/logo.png)\n![icon](docs/app-icon.png)')).toEqual([]);
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

describe('findBannerUrl', () => {
	it('picks a keyword image and resolves it to a raw url', () => {
		expect(banner('![](assets/banner.png)')).toBe(
			'https://raw.githubusercontent.com/acme/app/main/assets/banner.png'
		);
	});

	it('matches hero, cover and header as well as banner', () => {
		expect(banner('![](docs/hero.png)')).toContain('docs/hero.png');
		expect(banner('![](docs/cover.jpg)')).toContain('docs/cover.jpg');
		expect(banner('![](docs/header.webp)')).toContain('docs/header.webp');
	});

	it('returns null when the readme has images but none are a banner', () => {
		expect(banner('![](docs/shot.png)\n![](docs/other.png)')).toBeNull();
	});

	it('returns null for a readme with no images', () => {
		expect(banner('# Title\n\nSome prose.')).toBeNull();
	});

	it('does not treat a rejected image as a banner', () => {
		expect(banner('![](assets/banner.svg)')).toBeNull();
		expect(banner('![](https://img.shields.io/banner/build.png)')).toBeNull();
	});

	it('ignores a keyword that only appears in the query string', () => {
		expect(
			banner('<img src="https://discordapp.com/api/guilds/1137/widget.png?style=banner2">')
		).toBeNull();
		expect(banner('![](docs/shot.png?style=banner2)')).toBeNull();
	});

	it('rejects an svg that carries a query string', () => {
		expect(banner('![](assets/banner.svg?raw=true)')).toBeNull();
	});

	it('matches whole words, not substrings inside other words', () => {
		expect(banner('![](images/bannerman-logo.png)')).toBeNull();
		expect(banner('![](docs/discoverable.png)')).toBeNull();
		expect(banner('![](docs/whatsoever.png)')).toBeNull();
	});

	it('ignores interface chrome that happens to carry a keyword', () => {
		expect(banner('![](img/hero-icon.png)')).toBeNull();
		expect(banner('![](assets/header-nav-arrow.png)')).toBeNull();
		expect(banner('![](assets/banner-logo.png)')).toBeNull();
	});

	it('matches regardless of case and separator', () => {
		expect(banner('![](images/Banner.png)')).toContain('images/Banner.png');
		expect(banner('![](docs/app_hero.png)')).toContain('docs/app_hero.png');
		expect(banner('![](docs/hero-image.png)')).toContain('docs/hero-image.png');
	});

	it('only accepts raster images', () => {
		expect(banner('![](assets/banner.mp4)')).toBeNull();
		expect(banner('![](assets/banner.pdf)')).toBeNull();
		expect(banner('![](assets/banner.webp)')).toContain('assets/banner.webp');
	});

	it('rejects generated images served from an api path', () => {
		expect(banner('![](https://example.com/api/v1/banner.png)')).toBeNull();
	});

	it('takes the first match when several images qualify', () => {
		expect(banner('![](docs/hero.png)\n![](docs/banner.png)')).toContain('docs/hero.png');
	});

	it('reads an html img tag', () => {
		expect(banner('<img src="assets/banner.png" width="800">')).toContain('assets/banner.png');
	});
});

describe('extractReadmeImages with an excluded banner', () => {
	it('omits the banner and keeps positions dense from zero', () => {
		const markdown = '![](docs/hero-shot.png)\n![](docs/a.png)\n![](docs/b.png)';
		const excluded = banner(markdown);

		const images = extract(markdown, excluded);

		expect(images.map((image) => image.url)).toEqual([
			'https://raw.githubusercontent.com/acme/app/main/docs/a.png',
			'https://raw.githubusercontent.com/acme/app/main/docs/b.png'
		]);
		expect(images.map((image) => image.position)).toEqual([0, 1]);
	});

	it('is unchanged when nothing is excluded', () => {
		expect(extract('![](docs/a.png)', null)).toHaveLength(1);
	});
});
