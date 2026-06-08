export function apiBaseUrl(): string {
  const host = window.location.hostname;
  if (host === 'localhost' || host === '127.0.0.1') {
    return 'http://localhost:8080/api';
  }
  return `${window.location.origin}/bingo-api/api`;
}

export function appUrl(path: string): string {
  const cleanPath = path.replace(/^\/+/, '');
  const baseHref = document.querySelector('base')?.href ?? `${window.location.origin}/`;
  return new URL(cleanPath, baseHref).toString();
}
