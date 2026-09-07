import { error, redirect, type RequestEvent } from '@sveltejs/kit';
import type { SessionUser } from '@yuki/auth';

export function requireUser(event: RequestEvent): SessionUser {
	const user = event.locals.user;

	if (!user) {
		const redirectTo = `${event.url.pathname}${event.url.search}`;
		redirect(303, `/signin?redirectTo=${encodeURIComponent(redirectTo)}`);
	}

	return user;
}

export function requireDeveloper(event: RequestEvent): SessionUser {
	const user = requireUser(event);

	if (user.role !== 'developer') {
		error(403, 'Developer access required');
	}

	return user;
}
