import type { Component } from 'svelte';
import type { ListingCategory } from './categories.ts';
import BluetoothIcon from '@lucide/svelte/icons/bluetooth';
import ClapperboardIcon from '@lucide/svelte/icons/clapperboard';
import CodeXmlIcon from '@lucide/svelte/icons/code-xml';
import FolderIcon from '@lucide/svelte/icons/folder';
import Gamepad2Icon from '@lucide/svelte/icons/gamepad-2';
import NetworkIcon from '@lucide/svelte/icons/network';
import PackageIcon from '@lucide/svelte/icons/package';
import PaletteIcon from '@lucide/svelte/icons/palette';
import Settings2Icon from '@lucide/svelte/icons/settings-2';
import ShieldCheckIcon from '@lucide/svelte/icons/shield-check';
import SmartphoneIcon from '@lucide/svelte/icons/smartphone';
import WorkflowIcon from '@lucide/svelte/icons/workflow';
import WrenchIcon from '@lucide/svelte/icons/wrench';

const ICONS: Record<ListingCategory, Component> = {
	system_tweaks: Settings2Icon,
	app_management: PackageIcon,
	file_management: FolderIcon,
	media: ClapperboardIcon,
	gaming: Gamepad2Icon,
	automation: WorkflowIcon,
	networking: NetworkIcon,
	privacy_security: ShieldCheckIcon,
	developer_tools: CodeXmlIcon,
	device_specific: SmartphoneIcon,
	customization: PaletteIcon,
	connectivity: BluetoothIcon,
	utilities: WrenchIcon
};

export function categoryIcon(category: ListingCategory): Component {
	return ICONS[category];
}
