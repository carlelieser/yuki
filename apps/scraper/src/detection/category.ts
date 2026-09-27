import { createHash } from 'node:crypto';
import { generateText, Output, type LanguageModel } from 'ai';
import { z } from 'zod';
import { listingCategory, type ListingCategory } from '@yuki/db/schema';

export type CategoryInput = {
	description: string | null;
	topics: string[];
	readme: string | null;
};

export type Categorizer = {
	fingerprint: (input: CategoryInput) => string;
	categorize: (input: CategoryInput) => Promise<ListingCategory | null>;
};

const README_LIMIT = 4000;
const MAX_OUTPUT_TOKENS = 64;

const categorySchema = z.object({
	category: z.enum(listingCategory.enumValues).nullable()
});

const INSTRUCTIONS = `You categorize open-source Android apps for an app catalog.
Read the app's description, topics and README, then pick the one category that matches the app's main purpose.
Return null when the repository is not an Android app or there is not enough information to tell what it does.

Categories:
- system_tweaks: Changes how Android itself works: its settings, system features and built-in behaviour.
- app_management: Manages the other apps on the phone: installing, updating, removing, freezing, restricting or cloning them.
- file_management: Works with the files on the phone: browsing, organising, cleaning up or transferring them.
- media: Lets the user watch, listen to, read, capture or download content such as video, music, images, books and comics.
- gaming: Exists to play games or to get more out of them.
- automation: Carries out actions on the phone for the user, from scheduled tasks to AI agents that operate the phone.
- networking: Controls how the phone connects to and uses networks: mobile network and carrier settings, Wi-Fi, VPNs, proxies, DNS and data usage.
- privacy_security: Protects the user's privacy or the phone's security, for example by blocking trackers and ads, isolating apps or restricting permissions.
- developer_tools: Serves people who build, debug or inspect software.
- device_specific: Only works on particular phone brands, models or manufacturer versions of Android, such as One UI or HyperOS. Choose this over every other category when it applies.
- customization: Changes how the phone looks.
- connectivity: Connects the phone to other devices, such as Bluetooth accessories, watches, cars, TVs and computers.
- utilities: A standalone everyday tool that none of the other categories describe.`;

export function createCategorizer(model: LanguageModel): Categorizer {
	return {
		fingerprint: (input) =>
			createHash('sha256')
				.update(JSON.stringify({ model: modelId(model), instructions: INSTRUCTIONS, input }))
				.digest('hex'),
		categorize: async (input) => {
			if (!hasEvidence(input)) return null;

			const { output } = await generateText({
				model,
				instructions: INSTRUCTIONS,
				prompt: JSON.stringify({
					description: input.description,
					topics: input.topics,
					readme: input.readme?.slice(0, README_LIMIT) ?? null
				}),
				output: Output.object({ schema: categorySchema }),
				temperature: 0,
				maxOutputTokens: MAX_OUTPUT_TOKENS,
				reasoning: 'none'
			});

			return output.category;
		}
	};
}

function modelId(model: LanguageModel): string {
	return typeof model === 'string' ? model : model.modelId;
}

function hasEvidence(input: CategoryInput): boolean {
	return (
		(input.description ?? '').trim() !== '' ||
		input.topics.length > 0 ||
		(input.readme ?? '').trim() !== ''
	);
}
