declare global {
	namespace App {
		interface Locals {
			db: import('@yuki/db').Database;
			user: import('@yuki/auth').SessionUser | null;
			session: import('@yuki/auth').SessionData | null;
		}
	}
}

export {};
