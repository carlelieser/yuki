import { createOpenAICompatible } from '@ai-sdk/openai-compatible';
import type { LanguageModel } from 'ai';

const DEFAULT_CATEGORY_MODEL = 'qwen3.5:9b';
const DEFAULT_OLLAMA_URL = 'http://localhost:11434/v1';

export function localCategoryModel(): LanguageModel {
	const ollama = createOpenAICompatible({
		name: 'ollama',
		baseURL: process.env.OLLAMA_BASE_URL || DEFAULT_OLLAMA_URL,
		supportsStructuredOutputs: true
	});
	return ollama(process.env.CATEGORY_MODEL || DEFAULT_CATEGORY_MODEL);
}
