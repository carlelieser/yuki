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
	modelId: string;
	categorize: (input: CategoryInput) => Promise<ListingCategory | null>;
};

const README_LIMIT = 4000;
const MAX_OUTPUT_TOKENS = 64;

const categorySchema = z.object({
	category: z.enum(listingCategory.enumValues).nullable()
});

const INSTRUCTIONS = `You categorize open-source Android apps for a catalog of apps that use Shizuku.
Pick the single category that best describes what the app is for, based on its description, topics and README.
Shizuku, ADB, root, shell access and Material You describe how an app works or looks, not what it is for. Only pick developer_tools when running commands or developing software is the app's purpose.
Return null when the repository is not an Android app or there is not enough information to tell.

Categories:
- system_tweaks: changes system settings or behaviour (status bar, quick settings, display, locale, multi-window, power and battery settings)
- app_management: installs, updates, freezes, disables, uninstalls or controls other apps
- file_management: file managers, file transfer and sharing, storage cleaners
- media: video, music, photo, reading, recording and downloading media
- gaming: games and tools for games (mods, save editors, controllers, overlays, boosters)
- automation: automates actions on the phone, including AI agents that operate the device
- networking: VPN, proxy, DNS, Wi-Fi, cellular, SIM, carrier and data usage
- privacy_security: privacy, security, firewalls, ad blocking, work profiles, device policy, authentication
- developer_tools: terminals, shells, ADB tools, logcat, IDEs, libraries and SDKs for developers, Shizuku itself and its forks
- device_specific: only works on one vendor, model or OS skin
- customization: themes, launchers, icons, wallpapers, fonts, widgets, lock screen and always-on display
- connectivity: Bluetooth, watches, cars, casting, remote control of or input sharing with another device
- utilities: general tools that fit no other category (monitors, timers, notes, calculators)`;

export function createCategorizer(model: LanguageModel): Categorizer {
	return {
		modelId: typeof model === 'string' ? model : model.modelId,
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

export function fingerprintCategoryInput(modelId: string, input: CategoryInput): string {
	return createHash('sha256').update(JSON.stringify({ modelId, input })).digest('hex');
}

function hasEvidence(input: CategoryInput): boolean {
	return (
		(input.description ?? '').trim() !== '' ||
		input.topics.length > 0 ||
		(input.readme ?? '').trim() !== ''
	);
}
