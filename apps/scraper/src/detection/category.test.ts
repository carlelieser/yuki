import { describe, expect, it } from 'vitest';
import { MockLanguageModelV4 } from 'ai/test';
import { createCategorizer, fingerprintCategoryInput } from './category.ts';

function modelAnswering(text: string) {
	return new MockLanguageModelV4({
		modelId: 'mock-model',
		doGenerate: async () => ({
			content: [{ type: 'text', text }],
			finishReason: { unified: 'stop', raw: undefined },
			usage: {
				inputTokens: { total: 10, noCache: 10, cacheRead: undefined, cacheWrite: undefined },
				outputTokens: { total: 5, text: 5, reasoning: undefined }
			},
			warnings: []
		})
	});
}

describe('createCategorizer', () => {
	it('returns the category the model picks', async () => {
		const categorizer = createCategorizer(modelAnswering('{"category":"developer_tools"}'));

		const category = await categorizer.categorize({
			description: 'A local ADB shell',
			topics: ['adb'],
			readme: null
		});

		expect(category).toBe('developer_tools');
	});

	it('returns null when the model finds no fitting category', async () => {
		const categorizer = createCategorizer(modelAnswering('{"category":null}'));

		const category = await categorizer.categorize({
			description: 'My dotfiles',
			topics: [],
			readme: null
		});

		expect(category).toBeNull();
	});

	it('sends the repository as structured input with a category schema', async () => {
		const model = modelAnswering('{"category":"media"}');
		const categorizer = createCategorizer(model);

		await categorizer.categorize({
			description: 'Manga reader',
			topics: ['manga'],
			readme: '# Reader'
		});

		const [call] = model.doGenerateCalls;
		const user = call?.prompt.find((message) => message.role === 'user');
		const text = user?.content.find((part) => part.type === 'text');
		expect(JSON.parse(text?.type === 'text' ? text.text : '')).toEqual({
			description: 'Manga reader',
			topics: ['manga'],
			readme: '# Reader'
		});
		expect(call?.responseFormat).toMatchObject({ type: 'json' });
	});

	it('rejects a category outside the catalog', async () => {
		const categorizer = createCategorizer(modelAnswering('{"category":"social"}'));

		await expect(
			categorizer.categorize({ description: 'Chat app', topics: [], readme: null })
		).rejects.toThrow();
	});

	it('skips the model when there is no evidence', async () => {
		const model = modelAnswering('{"category":"media"}');
		const categorizer = createCategorizer(model);

		expect(await categorizer.categorize({ description: ' ', topics: [], readme: null })).toBeNull();
		expect(model.doGenerateCalls).toHaveLength(0);
	});

	it('reports the model it categorizes with', () => {
		expect(createCategorizer(modelAnswering('{}')).modelId).toBe('mock-model');
	});
});

describe('fingerprintCategoryInput', () => {
	const input = { description: 'Manga reader', topics: ['manga'], readme: '# Reader' };

	it('is stable for the same model and input', () => {
		expect(fingerprintCategoryInput('m', input)).toBe(fingerprintCategoryInput('m', input));
	});

	it('changes when the input or the model changes', () => {
		const base = fingerprintCategoryInput('m', input);
		expect(fingerprintCategoryInput('m', { ...input, readme: '# Reader 2' })).not.toBe(base);
		expect(fingerprintCategoryInput('other', input)).not.toBe(base);
	});
});
