import { describe, expect, it } from 'vitest';
import { categorize } from './category.ts';

describe('categorize', () => {
	it('returns null when there is no evidence', () => {
		expect(categorize({ description: null, topics: [], readme: null })).toBeNull();
		expect(categorize({ description: '', topics: [], readme: '   ' })).toBeNull();
	});

	it('reads the category from the description', () => {
		expect(
			categorize({
				description: 'Free and open source manga reader for Android',
				topics: ['manga', 'reader'],
				readme: null
			})
		).toBe('media');
	});

	it('treats a named game as decisive over the surrounding verb', () => {
		expect(
			categorize({
				description: 'Android Skin Script Installer',
				topics: [],
				readme: 'Importing, organizing and installing Mobile Legends skin script folders.'
			})
		).toBe('gaming');
	});

	it('does not match a term inside a longer word', () => {
		expect(
			categorize({
				description: 'Connects a self hosted library to the system Photo Picker',
				topics: [],
				readme: 'Apps that provide images can browse the photo library.'
			})
		).not.toBe('developer_tools');
	});

	it('ignores shizuku mechanism prose when scoring', () => {
		expect(
			categorize({
				description: 'Status bar and system icon hider',
				topics: ['status-bar'],
				readme: 'Uses adb shell permissions through Shizuku to hide the status bar.'
			})
		).toBe('system_tweaks');
	});

	it('ignores release boilerplate in the readme', () => {
		expect(
			categorize({
				description: 'Per app force dark mode',
				topics: ['dark-mode'],
				readme: 'Download the APK from releases and install it. Requires Android 10.'
			})
		).toBe('customization');
	});

	it('locks to device_specific only on an explicit restriction', () => {
		expect(
			categorize({
				description: 'Overlay pill',
				topics: [],
				readme: 'Overlay pill for one Samsung Galaxy S25. Sideload only. No other devices.'
			})
		).toBe('device_specific');
	});

	it('does not lock to device_specific on a compatibility note', () => {
		expect(
			categorize({
				description: 'AI automation assistant that controls the phone',
				topics: ['automation'],
				readme: 'Automation agent. Tested on MIUI and HyperOS devices.'
			})
		).toBe('automation');
	});

	it('prefers the traffic tunnel over the privacy claim', () => {
		expect(
			categorize({
				description: 'A secure VPN proxy client with privacy protection',
				topics: ['proxy', 'vpn'],
				readme: 'Rule, global and direct proxy modes.'
			})
		).toBe('networking');
	});

	it('ignores a game mentioned once in passing', () => {
		expect(
			categorize({
				description: 'Utilize an integrated firewall to manage application components',
				topics: [],
				readme: 'Blocker controls components. Also useful while playing a game.'
			})
		).not.toBe('gaming');
	});

	it('still reads a readme that is about games throughout', () => {
		expect(
			categorize({
				description: 'Performance booster',
				topics: [],
				readme: 'Boost your games. Tune each game and relaunch the game for best results.'
			})
		).toBe('gaming');
	});

	it('does not treat a generic controller as a game controller', () => {
		expect(
			categorize({
				description: 'An open-source volume controller for Android',
				topics: [],
				readme: 'Routes the hardware volume keys between screens.'
			})
		).not.toBe('gaming');
	});

	it('reads a real gamepad app as gaming', () => {
		expect(
			categorize({
				description: 'Use the Steam Controller as a standard Android gamepad',
				topics: [],
				readme: 'Exposes the controller to Android as a virtual gamepad.'
			})
		).toBe('gaming');
	});

	it('is deterministic across repeated calls', () => {
		const input = {
			description: 'Android file manager with dual pane browsing',
			topics: ['filemanager'],
			readme: 'Copy, move and compress files.'
		};
		expect(categorize(input)).toBe(categorize(input));
	});
});
