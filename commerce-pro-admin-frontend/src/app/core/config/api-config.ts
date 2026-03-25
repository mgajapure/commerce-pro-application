import { environment } from '../../../environments/environment';

/**
 * Base URL for all backend HTTP calls.
 * Dev:  http://localhost:8080  (from environment.ts)
 * Prod: value of NG_APP_API_URL build env var (from environment.prod.ts)
 */
export const API_HOST: string = environment.apiUrl;
