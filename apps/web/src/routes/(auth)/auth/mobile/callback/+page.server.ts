import type { PageServerLoad } from './$types';

const APP_PACKAGE = 'app.yuki';

export const load = (({ url, setHeaders }) => {
	setHeaders({ 'cache-control': 'no-store', 'referrer-policy': 'no-referrer' });

	const intent = `intent://${url.host}${url.pathname}${url.search}#Intent;scheme=${url.protocol.slice(0, -1)};package=${APP_PACKAGE};end`;

	return { appUrl: intent };
}) satisfies PageServerLoad;
