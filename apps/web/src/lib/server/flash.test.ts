import { describe, expect, it } from 'vitest';
import { isRedirect, type Cookies } from '@sveltejs/kit';
import { takeQueryFlash } from './flash.ts';

function cookieJar() {
	const values = new Map<string, string>();
	return {
		get: (name: string) => values.get(name),
		set: (name: string, value: string) => void values.set(name, value),
		delete: (name: string) => void values.delete(name)
	} as unknown as Cookies;
}

function take(path: string, cookies: Cookies) {
	try {
		return {
			value: takeQueryFlash({ url: new URL(`https://yuki.test${path}`), cookies }, 'error')
		};
	} catch (thrown) {
		if (isRedirect(thrown)) return { redirectedTo: thrown.location };
		throw thrown;
	}
}

describe('takeQueryFlash', () => {
	it('redirects to the same page without the parameter, keeping the rest', () => {
		const cookies = cookieJar();

		expect(take('/signin?redirectTo=%2Fbrowse&error=access_denied', cookies)).toEqual({
			redirectedTo: '/signin?redirectTo=%2Fbrowse'
		});
	});

	it('hands the value to the next load exactly once', () => {
		const cookies = cookieJar();
		take('/signin?error=access_denied', cookies);

		expect(take('/signin', cookies)).toEqual({ value: 'access_denied' });
		expect(take('/signin', cookies)).toEqual({ value: null });
	});
});
