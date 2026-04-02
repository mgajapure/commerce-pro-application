import { PageResponse } from '../common';

export interface IdentityUser {
  id: string;
  username: string;
  email: string;
  firstName?: string;
  lastName?: string;
  phone?: string;
  active: boolean;
  emailVerified: boolean;
  mfaEnabled: boolean;
  roleCodes: string[];
  createdAt?: string;
  lastLoginAt?: string;
}

export interface CreateIdentityUserRequest {
  username: string;
  email: string;
  password: string;
  firstName?: string;
  lastName?: string;
  phone?: string;
  sendWelcomeEmail?: boolean;
  initialRoleCodes?: string[];
}

export interface UpdateIdentityUserRequest {
  email?: string;
  firstName?: string;
  lastName?: string;
  phone?: string;
  active?: boolean;
  mfaEnabled?: boolean;
}

export interface IdentityRole {
  id: string;
  code: string;
  name: string;
  description?: string;
  type?: string;
  system: boolean;
  superAdmin: boolean;
  permissionCount: number;
  assignedUserCount: number;
  parentRoleId?: string;
  parentRoleName?: string;
}

export interface CreateIdentityRoleRequest {
  code: string;
  name: string;
  description?: string;
  parentRoleId?: string;
  permissionCodes?: string[];
  requiresMfa?: boolean;
  sessionTimeoutMinutes?: number;
  allowedIpPatterns?: string;
}

export interface UpdateIdentityRoleRequest {
  name?: string;
  description?: string;
  requiresMfa?: boolean;
  sessionTimeoutMinutes?: number;
  allowedIpPatterns?: string;
}

export interface IdentityPermission {
  code: string;
  name: string;
  description?: string;
  category?: string;
  system: boolean;
  requiresApproval: boolean;
  riskLevel: number;
  applicableScopes?: string[];
  roleCount: number;
}

export interface CreateIdentityPermissionRequest {
  code: string;
  name: string;
  description?: string;
  category?: string;
  riskLevel?: number;
  requiresApproval?: boolean;
  applicableScopes?: string[];
}

export interface IdentityUserDetail extends IdentityUser {
  failedLoginAttempts?: number;
  lockedUntil?: string;
  mustChangePassword?: boolean;
  passwordChangedAt?: string;
  lastLoginIp?: string;
  roleAssignments?: IdentityRoleAssignment[];
}

export interface IdentityRoleAssignment {
  id: string;
  roleCode: string;
  roleName: string;
  status: string;
  assignedAt: string;
  assignedBy?: string;
}

export interface ImpersonationToken {
  token: string;
  targetUserId: string;
  targetUsername: string;
  expiresAt: string;
}

export interface AuditLogEntry {
  id: string;
  action: string;
  actorUsername?: string;
  targetType?: string;
  targetIdentifier?: string;
  description?: string;
  success: boolean;
  ipAddress?: string;
  timestamp: string;
}

export interface SuperAdminConfigView {
  securityPolicy?: {
    requireMfa?: boolean;
    sessionTimeoutMinutes?: number;
    maxConcurrentSessions?: number;
    allowedIpRanges?: string[];
  };
  configManagement?: {
    canDeleteSystemRoles?: boolean;
    canModifyOwnRole?: boolean;
    minSuperAdmins?: number;
    allowRemoteConfigReload?: boolean;
  };
}

export interface IdentityDashboardSnapshot {
  usersPage: PageResponse<IdentityUser>;
  rolesPage: PageResponse<IdentityRole>;
  permissionsPage: PageResponse<IdentityPermission>;
  auditPage: PageResponse<AuditLogEntry>;
}
