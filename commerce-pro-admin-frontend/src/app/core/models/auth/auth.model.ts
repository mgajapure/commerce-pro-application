export interface AuthRequest {
  username: string;
  password: string;
  mfaCode?: string;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  issuedAt: string;
  userId: string;
  username: string;
  superAdmin: boolean;
  mustChangePassword?: boolean;
}

export interface AuthSession {
  accessToken: string;
  refreshToken: string;
  userId: string;
  username: string;
  superAdmin: boolean;
  authorities: string[];
  expiresAtMs?: number;
  mustChangePassword?: boolean;
}

export interface ChangePasswordRequest {
  currentPassword: string;
  newPassword: string;
}

export interface MfaSetupResponse {
  secret: string;
  otpauthUrl: string;
  issuer: string;
  accountName: string;
}

export interface MfaEnableRequest {
  code: string;
}

export interface MfaDisableRequest {
  password: string;
  code: string;
}
