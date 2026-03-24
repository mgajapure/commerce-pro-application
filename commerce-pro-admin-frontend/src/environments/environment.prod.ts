// Angular 17+ (esbuild) replaces process.env['NG_APP_*'] at build time.
// On Render Static Site, add build env var: NG_APP_API_URL=https://your-backend.onrender.com
declare const process: { env: Record<string, string> };

export const environment = {
  production: true,
  apiUrl: process.env['NG_APP_API_URL'] || 'https://commerce-pro-backend.onrender.com',
};
