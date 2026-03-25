// Angular 17+ (esbuild) replaces process.env['NG_APP_*'] at build time.
// On Render Static Site, add build env var: NG_APP_API_URL=https://your-backend.onrender.com

export const environment = {
  production: true,
  apiUrl: 'https://commerce-pro-backend.onrender.com',
};
