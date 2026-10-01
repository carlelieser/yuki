import publishers from './platform-publishers.json';

export const PLATFORM_CERTIFICATES: string[] = publishers.flatMap((publisher) =>
	publisher.certificates.map((certificate) => certificate.sha256)
);
