import { listingCategory, type ListingCategory } from '@yuki/db/schema';

export type CategoryOption = {
	value: ListingCategory;
	label: string;
};

const LABELS: Record<ListingCategory, string> = {
	system_tweaks: 'System',
	app_management: 'Apps',
	file_management: 'Files',
	media: 'Media',
	gaming: 'Gaming',
	automation: 'Automation',
	networking: 'Network',
	privacy_security: 'Privacy',
	developer_tools: 'Developer',
	device_specific: 'Device',
	customization: 'Customization',
	connectivity: 'Connectivity',
	utilities: 'Utilities'
};

export const CATEGORY_OPTIONS: CategoryOption[] = listingCategory.enumValues.map((value) => ({
	value,
	label: LABELS[value]
}));

export function categoryLabel(category: ListingCategory): string {
	return LABELS[category];
}

export function readCategory(raw: string | null): ListingCategory | null {
	return listingCategory.enumValues.find((value) => value === raw) ?? null;
}

export type { ListingCategory };
