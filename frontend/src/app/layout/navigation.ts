import { Role } from "../core/models/auth.model";

export interface NavItem {
    label: string;
    icon :string;
    path: string;
    roles: Role[];
}

export const NAV_ITEMS: NavItem[] = [
    { 
        label: 'Dashboard', 
        icon: 'dashboard', 
        path: '/dashboard', 
        roles: ['ADMIN', 'MANAGER', 'EMPLOYEE'] 
    },
    { 
        label: 'Apply Leave', 
        icon: 'event_available', 
        path: '/leaves/apply', 
        roles: ['MANAGER', 'EMPLOYEE'] 
    },
    { 
        label: 'My Leave History', 
        icon: 'history', 
        path: '/leaves/history', 
        roles: ['MANAGER', 'EMPLOYEE'] 
    },
    { 
        label: 'Pending Approvals', 
        icon: 'task_alt', 
        path: '/leaves/approvals', 
        roles: ['ADMIN', 'MANAGER'] 
    },
    { 
        label: 'Employees', 
        icon: 'groups', 
        path: '/employees', 
        roles: ['ADMIN'] 
    },
];