export function getTokenExpiryMs(token: string): number | null {
  try {
    const payload = token.split('.')[1];
    const json = atob(payload.replace(/-/g, '+').replace(/_/g, '/'));
    const expiry = JSON.parse(json).exp;
    return typeof expiry === 'number' ? expiry * 1000 : null;
  } catch {
    return null;
  }
}
