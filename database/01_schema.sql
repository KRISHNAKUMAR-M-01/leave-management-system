CREATE DATABASE IF NOT EXISTS leave_management
CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;

USE leave_management;
DROP DATABASE leave_management;

CREATE TABLE roles (
    id BIGINT NOT NULL AUTO_INCREMENT,
    role_name VARCHAR(30) NOT NULL,
    
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP 
    ON UPDATE CURRENT_TIMESTAMP,
    
    CONSTRAINT pk_roles PRIMARY KEY(id),
    CONSTRAINT uq_roles_name UNIQUE(role_name)
);

CREATE TABLE departments (
    id BIGINT NOT NULL AUTO_INCREMENT,
    department_name VARCHAR(50) NOT NULL,
    department_code VARCHAR(10) NOT NULL,
    
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP 
    ON UPDATE CURRENT_TIMESTAMP,
    
    CONSTRAINT pk_departments PRIMARY KEY(id),
    CONSTRAINT uq_departments_name UNIQUE(department_name),
    CONSTRAINT uq_departments_code UNIQUE(department_code)
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
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP 
    ON UPDATE CURRENT_TIMESTAMP,
    
    CONSTRAINT pk_employees PRIMARY key(id),
    CONSTRAINT uk_employees_email UNIQUE(email),
    
    CONSTRAINT fk_employees_department FOREIGN KEY(department_id) REFERENCES departments(id),
    CONSTRAINT fk_employees_role FOREIGN KEY(role_id) REFERENCES roles(id),
    CONSTRAINT fk_employees_manager FOREIGN KEY(manager_id) REFERENCES employees(id),
    
    INDEX idx_employees_full_name(full_name)
);

CREATE TABLE leave_types (
    id BIGINT NOT NULL AUTO_INCREMENT,
    leave_type_name VARCHAR(30) NOT NULL,
    days_per_year DECIMAL(4,1) NOT NULL,
    
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP 
    ON UPDATE CURRENT_TIMESTAMP,
    
    CONSTRAINT pk_leave_types PRIMARY KEY(id),
    CONSTRAINT uq_leave_types_name UNIQUE(leave_type_name),
    CONSTRAINT chk_leave_types_days_per_year CHECK(days_per_year > 0)
    
);

CREATE TABLE public_holidays (
    id BIGINT NOT NULL AUTO_INCREMENT,
    holiday_date DATE NOT NULL,
    holiday_name VARCHAR(100) NOT NULL,
    
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP 
    ON UPDATE CURRENT_TIMESTAMP,
    
    CONSTRAINT pk_public_holidays PRIMARY KEY(id),
    CONSTRAINT uk_public_holidays_holiday_date UNIQUE(holiday_date)
);

CREATE TABLE leave_balances (
    id BIGINT NOT NULL AUTO_INCREMENT,
    employee_id BIGINT NOT NULL,
    leave_type_id BIGINT NOT NULL,
    calendar_year INT NOT NULL,
    total_days DECIMAL(4,1) NOT NULL,
    used_days DECIMAL(4,1) NOT NULL,
    
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP 
    ON UPDATE CURRENT_TIMESTAMP,
    
    CONSTRAINT pk_leave_balances PRIMARY KEY(id),
    CONSTRAINT uq_leave_balances_employee_type_year UNIQUE(employee_id, leave_type_id, calendar_year),
    
    CONSTRAINT fk_leave_balances_employee_id FOREIGN KEY(employee_id) REFERENCES employees(id),
    CONSTRAINT fk_leave_balances_leave_type_id FOREIGN KEY(leave_type_id) REFERENCES leave_types(id),
    
    CONSTRAINT chk_leave_balances_total_days CHECK(total_days >= 0),
    CONSTRAINT chk_leave_balances_used_days CHECK(used_days >= 0),
    CONSTRAINT chk_leave_balances_used_within_total CHECK(used_days <= total_days)
); 

CREATE TABLE leave_requests (
    id BIGINT NOT NULL AUTO_INCREMENT,
    employee_id BIGINT NOT NULL,
    leave_type_id BIGINT NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    total_days DECIMAL(4,1) NOT NULL,
    is_half_day BOOLEAN NOT NULL DEFAULT FALSE,
    half_day_session VARCHAR(15) NULL,
    reason VARCHAR(500) NOT NULL,
    leave_status VARCHAR(20) NOT NULL,
    reviewed_by BIGINT NOT NULL,
    review_comment VARCHAR(500) NOT NULL,
    reviewed_at TIMESTAMP NOT NULL,
    
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP 
    ON UPDATE CURRENT_TIMESTAMP,
    
    CONSTRAINT pk_leave_requests PRIMARY KEY(id),
    CONSTRAINT uk_leave_requests_leave_status UNIQUE(leave_status),
    
    CONSTRAINT fk_leave_requests_employee_id FOREIGN KEY(employee_id) REFERENCES employees(id),
    CONSTRAINT fk_leave_requests_leave_type_id FOREIGN KEY(leave_type_id) REFERENCES leave_types(id),
    CONSTRAINT fk_leave_requests_reviewed_by FOREIGN KEY(reviewed_by) REFERENCES employees(id),
    
    INDEX idx_leave_requests_employee_status(employee_id,leave_status),
    INDEX idx_leave_requests_dates(start_date, end_date),
    
    CONSTRAINT chk_leave_requests_dates CHECK(end_date >= start_date),
    CONSTRAINT chk_leave_requests_total_days CHECK(total_days > 0),
    CONSTRAINT chk_leave_requests_leave_status CHECK(leave_status IN('PENDING', 'APPROVED', 'REJECTED', 'CANCELLED')),
    CONSTRAINT chk_leave_requests_half_day CHECK(
    (is_half_day = FALSE AND half_day_session IS NULL)
	OR
	(is_half_day = TRUE
	AND half_day_session IN ('FIRST_HALF', 'SECOND_HALF')
	AND start_date = end_date
	AND total_days = 0.5)
    ),
    CONSTRAINT chk_leave_requests_rejection_comment CHECK (leave_status <> 'REJECTED' OR review_comment IS NOT NULL)
);

SHOW CREATE TABLE leave_requests;
SHOW TABLES;
DESC leave_requests;
