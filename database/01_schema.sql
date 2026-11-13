CREATE DATABASE IF NOT EXISTS leave_management
CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;

USE leave_management;

CREATE TABLE roles (
    id BIGINT NOT NULL AUTO_INCREMENT,
    role_name VARCHAR(30) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_roles PRIMARY KEY (id),
    CONSTRAINT uq_roles_name UNIQUE (role_name)
);

CREATE TABLE departments (
    id BIGINT NOT NULL AUTO_INCREMENT,
    department_name VARCHAR(50) NOT NULL,
    department_code VARCHAR(10) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_departments PRIMARY KEY (id),
    CONSTRAINT uq_departments_name UNIQUE (department_name),
    CONSTRAINT uq_departments_code UNIQUE (department_code)
);

CREATE TABLE employees (
    id BIGINT NOT NULL AUTO_INCREMENT,
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    phone VARCHAR(15) NOT NULL,
    department_id BIGINT NOT NULL,
    designation VARCHAR(50) NOT NULL,
    joining_date DATE NOT NULL,
    role_id BIGINT NOT NULL,
    manager_id BIGINT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_employees PRIMARY KEY (id),
    CONSTRAINT uk_employees_email UNIQUE (email),
    CONSTRAINT fk_employees_department FOREIGN KEY (department_id)
        REFERENCES departments (id),
    CONSTRAINT fk_employees_role FOREIGN KEY (role_id)
        REFERENCES roles (id),
    CONSTRAINT fk_employees_manager FOREIGN KEY (manager_id)
        REFERENCES employees (id),
    INDEX idx_employees_full_name (full_name)
);

CREATE TABLE leave_types (
    id BIGINT NOT NULL AUTO_INCREMENT,
    leave_type_name VARCHAR(30) NOT NULL,
    days_per_year DECIMAL(4 , 1 ) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_leave_types PRIMARY KEY (id),
    CONSTRAINT uq_leave_types_name UNIQUE (leave_type_name),
    CONSTRAINT chk_leave_types_days_per_year CHECK (days_per_year > 0)
);

CREATE TABLE public_holidays (
    id BIGINT NOT NULL AUTO_INCREMENT,
    holiday_date DATE NOT NULL,
    holiday_name VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_public_holidays PRIMARY KEY (id),
    CONSTRAINT uk_public_holidays_holiday_date UNIQUE (holiday_date)
);

CREATE TABLE leave_balances (
    id BIGINT NOT NULL AUTO_INCREMENT,
    employee_id BIGINT NOT NULL,
    leave_type_id BIGINT NOT NULL,
    calendar_year INT NOT NULL,
    total_days DECIMAL(4 , 1 ) NOT NULL,
    used_days DECIMAL(4 , 1 ) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_leave_balances PRIMARY KEY (id),
    CONSTRAINT uq_leave_balances_employee_type_year UNIQUE (employee_id , leave_type_id , calendar_year),
    CONSTRAINT fk_leave_balances_employee_id FOREIGN KEY (employee_id)
        REFERENCES employees (id),
    CONSTRAINT fk_leave_balances_leave_type_id FOREIGN KEY (leave_type_id)
        REFERENCES leave_types (id),
    CONSTRAINT chk_leave_balances_total_days CHECK (total_days >= 0),
    CONSTRAINT chk_leave_balances_used_days CHECK (used_days >= 0),
    CONSTRAINT chk_leave_balances_used_within_total CHECK (used_days <= total_days)
);

CREATE TABLE leave_requests (
    id BIGINT NOT NULL AUTO_INCREMENT,
    employee_id BIGINT NOT NULL,
    leave_type_id BIGINT NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    total_days DECIMAL(4 , 1 ) NOT NULL,
    is_half_day BOOLEAN NOT NULL DEFAULT FALSE,
    half_day_session VARCHAR(15) NULL,
    reason VARCHAR(500) NOT NULL,
    leave_status VARCHAR(20) NOT NULL,
    reviewed_by BIGINT NULL,
    review_comment VARCHAR(500) NULL,
    reviewed_at TIMESTAMP NULL,
    medical_document_name VARCHAR(255) NULL,
    medical_document_type VARCHAR(100) NULL,
    medical_document LONGBLOB NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_leave_requests PRIMARY KEY (id),
    CONSTRAINT fk_leave_requests_employee_id FOREIGN KEY (employee_id)
        REFERENCES employees (id),
    CONSTRAINT fk_leave_requests_leave_type_id FOREIGN KEY (leave_type_id)
        REFERENCES leave_types (id),
    CONSTRAINT fk_leave_requests_reviewed_by FOREIGN KEY (reviewed_by)
        REFERENCES employees (id),
    INDEX idx_leave_requests_employee_status (employee_id , leave_status),
    INDEX idx_leave_requests_dates (start_date , end_date),
    CONSTRAINT chk_leave_requests_dates CHECK (end_date >= start_date),
    CONSTRAINT chk_leave_requests_total_days CHECK (total_days > 0),
    CONSTRAINT chk_leave_requests_leave_status CHECK (leave_status IN ('PENDING' , 'APPROVED', 'REJECTED', 'CANCELLED')),
    CONSTRAINT chk_leave_requests_half_day CHECK ((is_half_day = FALSE
        AND half_day_session IS NULL)
        OR (is_half_day = TRUE
        AND half_day_session IN ('FIRST_HALF' , 'SECOND_HALF')
        AND start_date = end_date
        AND total_days = 0.5)),
    CONSTRAINT chk_leave_requests_rejection_comment CHECK (leave_status <> 'REJECTED'
        OR review_comment IS NOT NULL)
);

ALTER TABLE leave_requests
ADD COLUMN medical_document_name VARCHAR(255) NULL,
ADD COLUMN medical_document_type VARCHAR(100) NULL,
ADD COLUMN medical_document LONGBLOB NULL;

SELECT * FROM leave_requests;

ALTER TABLE employees 
ADD COLUMN gender VARCHAR(20) NOT NULL DEFAULT 'UNSPECIFIED';

SET SQL_SAFE_UPDATES = 0;
UPDATE employees SET gender = 'MALE';
SET SQL_SAFE_UPDATES = 1;

INSERT INTO leave_types (leave_type_name, days_per_year)
VALUES 
    ('Maternity', 180.0),
    ('Paternity', 15.0)
ON DUPLICATE KEY UPDATE days_per_year = VALUES(days_per_year);

SELECT * FROM leave_types;

CREATE TABLE IF NOT EXISTS leave_policy_rules (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    role_id BIGINT NOT NULL,
    leave_type_id BIGINT NOT NULL,
    max_annual_quota INT NOT NULL DEFAULT 12,
    max_carry_forward INT NOT NULL DEFAULT 0,
    min_notice_days INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_policy_role FOREIGN KEY (role_id)
        REFERENCES roles (id),
    CONSTRAINT fk_policy_leave_type FOREIGN KEY (leave_type_id)
        REFERENCES leave_types (id),
    CONSTRAINT uq_role_leave_type UNIQUE (role_id , leave_type_id)
);

INSERT INTO leave_policy_rules (role_id, leave_type_id, max_annual_quota, max_carry_forward, min_notice_days)
SELECT r.id, lt.id, 12, 0, 0
FROM roles r, leave_types lt
WHERE r.role_name = 'EMPLOYEE' AND lt.leave_type_name = 'Casual'
ON DUPLICATE KEY UPDATE max_annual_quota = 12, min_notice_days = 0;

INSERT INTO leave_policy_rules (role_id, leave_type_id, max_annual_quota, max_carry_forward, min_notice_days)
SELECT r.id, lt.id, 10, 0, 0
FROM roles r, leave_types lt
WHERE r.role_name = 'EMPLOYEE' AND lt.leave_type_name = 'Sick'
ON DUPLICATE KEY UPDATE max_annual_quota = 10, min_notice_days = 0;

INSERT INTO leave_policy_rules (role_id, leave_type_id, max_annual_quota, max_carry_forward, min_notice_days)
SELECT r.id, lt.id, 15, 5, 2
FROM roles r, leave_types lt
WHERE r.role_name = 'EMPLOYEE' AND lt.leave_type_name = 'Annual'
ON DUPLICATE KEY UPDATE max_annual_quota = 15, min_notice_days = 2;

INSERT INTO leave_policy_rules (role_id, leave_type_id, max_annual_quota, max_carry_forward, min_notice_days)
SELECT r.id, lt.id, 180, 0, 7
FROM roles r, leave_types lt
WHERE r.role_name = 'EMPLOYEE' AND lt.leave_type_name = 'Maternity'
ON DUPLICATE KEY UPDATE max_annual_quota = 180, min_notice_days = 7;

INSERT INTO leave_policy_rules (role_id, leave_type_id, max_annual_quota, max_carry_forward, min_notice_days)
SELECT r.id, lt.id, 15, 0, 3
FROM roles r, leave_types lt
WHERE r.role_name = 'EMPLOYEE' AND lt.leave_type_name = 'Paternity'
ON DUPLICATE KEY UPDATE max_annual_quota = 15, min_notice_days = 3;

INSERT INTO leave_policy_rules (role_id, leave_type_id, max_annual_quota, max_carry_forward, min_notice_days)
SELECT r.id, lt.id, 12, 0, 0
FROM roles r, leave_types lt
WHERE r.role_name = 'MANAGER' AND lt.leave_type_name = 'Casual'
ON DUPLICATE KEY UPDATE max_annual_quota = 12, min_notice_days = 0;

INSERT INTO leave_policy_rules (role_id, leave_type_id, max_annual_quota, max_carry_forward, min_notice_days)
SELECT r.id, lt.id, 10, 0, 0
FROM roles r, leave_types lt
WHERE r.role_name = 'MANAGER' AND lt.leave_type_name = 'Sick'
ON DUPLICATE KEY UPDATE max_annual_quota = 10, min_notice_days = 0;

INSERT INTO leave_policy_rules (role_id, leave_type_id, max_annual_quota, max_carry_forward, min_notice_days)
SELECT r.id, lt.id, 18, 10, 1
FROM roles r, leave_types lt
WHERE r.role_name = 'MANAGER' AND lt.leave_type_name = 'Annual'
ON DUPLICATE KEY UPDATE max_annual_quota = 18, min_notice_days = 1;

INSERT INTO leave_policy_rules (role_id, leave_type_id, max_annual_quota, max_carry_forward, min_notice_days)
SELECT r.id, lt.id, 180, 0, 7
FROM roles r, leave_types lt
WHERE r.role_name = 'MANAGER' AND lt.leave_type_name = 'Maternity'
ON DUPLICATE KEY UPDATE max_annual_quota = 180, min_notice_days = 7;

INSERT INTO leave_policy_rules (role_id, leave_type_id, max_annual_quota, max_carry_forward, min_notice_days)
SELECT r.id, lt.id, 15, 0, 3
FROM roles r, leave_types lt
WHERE r.role_name = 'MANAGER' AND lt.leave_type_name = 'Paternity'
ON DUPLICATE KEY UPDATE max_annual_quota = 15, min_notice_days = 3;

SELECT * FROM leave_policy_rules;
SELECT * FROM leave_balances;

SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE leave_requests;
TRUNCATE TABLE leave_balances;
SET FOREIGN_KEY_CHECKS = 1;

INSERT INTO leave_balances (employee_id, leave_type_id, calendar_year, total_days, used_days)
SELECT e.id, lt.id, YEAR(CURDATE()), COALESCE(lpr.max_annual_quota, lt.days_per_year), 0.0
FROM employees e
JOIN roles r ON r.id = e.role_id
CROSS JOIN leave_types lt
LEFT JOIN leave_policy_rules lpr ON lpr.role_id = r.id AND lpr.leave_type_id = lt.id
WHERE r.role_name <> 'ADMIN';

SELECT e.email, r.role_name, lt.leave_type_name, lb.calendar_year, lb.total_days, lb.used_days
FROM leave_balances lb
JOIN employees e ON e.id = lb.employee_id
JOIN roles r ON r.id = e.role_id
JOIN leave_types lt ON lt.id = lb.leave_type_id
ORDER BY e.email, lt.leave_type_name;

