export interface Employee {
  id: number;
  fullName: string;
  email: string;
  phone: string;
  departmentId: number;
  departmentName: string;
  designation: string;
  joiningDate: string;
  roleName: string;
  managerId: number | null;
  managerName: string | null;
  active: boolean;
}

export interface EmployeeFilter {
  search?: string;
  departmentId?: number;
  roleName?: string;
  active?: boolean;
  page?: number;
  size?: number;
  sortBy?: string;
  direction?: string;
}