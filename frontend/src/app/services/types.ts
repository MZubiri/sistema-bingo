export type Role = 'ADMIN' | 'VENDEDOR';
export type OrganizationCode = 'GEOURP' | 'CIVIAL' | 'ACI' | 'ADMIN';
export type CardStatus = 'AVAILABLE' | 'ASSIGNED' | 'CANCELLED';

export interface LoginResponse {
  token: string;
  username: string;
  fullName: string;
  role: Role;
  organizationCode: OrganizationCode;
}

export interface VendorDashboard {
  fullName: string;
  organizationCode: OrganizationCode;
  totalInOrganization: number;
  assignedInOrganization: number;
  availableInOrganization: number;
}

export interface CardResponse {
  id: number;
  serial: string;
  buyerName: string | null;
  sellerName: string | null;
  organizationCode: OrganizationCode;
  status: CardStatus;
  numbersJson: string;
  assignedAt: string | null;
  cancelledAt?: string | null;
}

export interface AdminDashboard {
  totalCards: number;
  assignedCards: number;
  availableCards: number;
  cancelledCards: number;
  organizations: OrganizationStats[];
  sellers: SellerStats[];
}

export interface OrganizationStats {
  organizationCode: OrganizationCode;
  total: number;
  assigned: number;
  available: number;
  cancelled: number;
}

export interface SellerStats {
  userId: number;
  username: string;
  fullName: string;
  organizationCode: OrganizationCode;
  soldCards: number;
}

export interface AdminCard {
  id: number;
  serial: string;
  organizationCode: OrganizationCode;
  status: CardStatus;
  buyerName: string | null;
  sellerName: string | null;
  assignedAt: string | null;
  cancelledAt: string | null;
}

export interface AdminCardFilters {
  search?: string;
  organizationCode?: OrganizationCode | '';
  status?: CardStatus | '';
  assignedFrom?: string;
  assignedTo?: string;
}

export interface CardHistoryFilters {
  search?: string;
  status?: CardStatus | '';
  assignedFrom?: string;
  assignedTo?: string;
}

export interface VerifyResponse {
  serial: string;
  status: CardStatus;
  buyerName: string | null;
  sellerName: string | null;
  organizationCode: OrganizationCode;
  assignedAt: string | null;
  numbersJson: string;
}

export interface AvailableCard {
  serial: string;
  organizationCode: OrganizationCode;
  status: CardStatus;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  page: number;
  size: number;
}
