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

	it('reads an adb shell app as a developer tool despite its design language', () => {
		expect(
			categorize({
				description: 'A material you designed app for your ADB needs',
				topics: ['adb', 'debugging', 'material-design', 'shell', 'shizuku', 'wireless-debugging'],
				readme:
					'<img alt="GitHub stars" src="https://img.shields.io/github/stars/x/y?style=for-the-badge" />\n' +
					'> **aShell You** is a shell utility app with **Material Design 3 UI**, letting you run **ADB**, **root** and **shell** commands'
			})
		).toBe('developer_tools');
	});

	it('ignores image paths and badge links in the readme', () => {
		expect(
			categorize({
				description: 'A high-performance app management powered by Shizuku',
				topics: ['apps', 'debloating', 'shizuku'],
				readme:
					'![AppVaultX](app/src/main/res/mipmap-xxxhdpi/ic_launcher.webp?raw=true)\n' +
					'[![](https://img.shields.io/badge/F--Droid-blue?style=flat)](https://f-droid.org)'
			})
		).toBe('app_management');
	});

	it('reads an adb toolkit as a developer tool', () => {
		expect(
			categorize({
				description: 'A powerful ADB toolkit running on the watch!',
				topics: ['adb', 'wearos'],
				readme: null
			})
		).toBe('developer_tools');
	});

	it('reads an on-device coding agent as a developer tool', () => {
		expect(
			categorize({
				description: 'A native AI coding agent harness app for Android',
				topics: ['ai-agent', 'coding-agent'],
				readme: 'Runs shell commands on the device through Shizuku.'
			})
		).toBe('developer_tools');
	});

	it('reads a library from its gradle dependency snippet', () => {
		expect(
			categorize({
				description: null,
				topics: [],
				readme:
					'With this API your app can call Android APIs as the shell user.\n' +
					'```groovy\nimplementation "dev.rikka.shizuku:api:13.1.5"\n```'
			})
		).toBe('developer_tools');
	});

	it('matches a hyphenated topic against a spaced phrase', () => {
		expect(
			categorize({
				description: 'Modern Android system monitoring app',
				topics: ['material-design', 'system-monitor'],
				readme: null
			})
		).toBe('utilities');
	});

	it('matches precedence conditions at word starts only', () => {
		expect(
			categorize({
				description: 'Playing around with reading cell stuff on Android',
				topics: [],
				readme:
					'View information about your cellular connection and other available connections. ' +
					'See nearby towers reported by the modem. There is also a Wear OS companion app.'
			})
		).toBe('networking');
	});

	it('prefers file transfer over the network channels it uses', () => {
		expect(
			categorize({
				description: '多轨快传，同时使用USB和5G与2.4GWIFI等通道传输文件到电脑',
				topics: [],
				readme: '一个可以同时使用USB和WIFI等多张网卡传输文件到电脑的软件。'
			})
		).toBe('file_management');
	});

	it('locks to device_specific for an app limited to supported Pixel phones', () => {
		expect(
			categorize({
				description: 'Jailbreak supported Google Pixel phones with CVE-2026-43499',
				topics: ['exploit', 'pixel', 'root'],
				readme: 'An application designed to automate root access on Google Pixel devices.'
			})
		).toBe('device_specific');
	});

	it('reads an exploit built for one named model as device_specific', () => {
		expect(
			categorize({
				description: 'GhostLock CVE-2026-43499 port for Galaxy Z Fold 8 (h8q)',
				topics: [],
				readme: 'Temporary root exploit payloads for the h8q firmware.'
			})
		).toBe('device_specific');
	});

	it('ignores a model named only as a test device in the readme', () => {
		expect(
			categorize({
				description: 'Virtual touchpad for Android desktop mode on external displays',
				topics: [],
				readme: 'Tested on a Pixel 8 running Android 16.'
			})
		).toBe('system_tweaks');
	});

	it('does not lock to device_specific on an OEM named as an example', () => {
		expect(
			categorize({
				description:
					'A simple app that enables you to add unsupported languages to your locale settings, if the OEM (ahem Xiaomi) does not let you.',
				topics: [],
				readme: null
			})
		).toBe('system_tweaks');
	});

	it('treats a named game in the readme as decisive', () => {
		expect(
			categorize({
				description: 'An Android app that allows you to sync your saves across multiple devices.',
				topics: ['cloud-sync'],
				readme: 'Sync your Stardew Valley saves using cloud storage services like Dropbox.'
			})
		).toBe('gaming');
	});

	it('reads a gaming overlay as gaming rather than a floating window tweak', () => {
		expect(
			categorize({
				description: 'Android gaming overlay, performance monitor and per-game profile manager',
				topics: ['floating-window', 'game-overlay', 'performance-monitor'],
				readme: null
			})
		).toBe('gaming');
	});

	it('prefers privacy for a firewall that runs through the vpn slot', () => {
		expect(
			categorize({
				description: 'Privacy is not default. Take it back with a firewall and package control',
				topics: [],
				readme:
					'Blocks network access per app using a local VPN or iptables, over Wi-Fi and mobile data.'
			})
		).toBe('privacy_security');
	});

	it('locks to device_specific when the readme says it was made only for one vendor', () => {
		expect(
			categorize({
				description: null,
				topics: [],
				readme:
					'An Android utility for triggering Circle to Search. It is only designed for Chinese OriginOS.'
			})
		).toBe('device_specific');
	});

	it('reads a screen mirroring client as connectivity', () => {
		expect(
			categorize({
				description: 'Better screen mirroring for Android devices',
				topics: [],
				readme: 'Control the other device with keyboard and mouse.'
			})
		).toBe('connectivity');
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
