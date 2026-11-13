USE leave_management;
SELECT * FROM employees;

SET SQL_SAFE_UPDATES = 0;
DELETE FROM leave_requests WHERE reason LIKE 'test%';
SET SQL_SAFE_UPDATES = 1;

DELETE FROM leave_requests WHERE reason LIKE 'test%';
UPDATE leave_balances SET used_days = 0;

SELECT * FROM leave_types WHERE id = 2;
SELECT * FROM leave_balances WHERE calendar_year = 2026;

INSERT INTO roles(role_name) VALUES
('ADMIN'),
('MANAGER'),
('EMPLOYEE');

INSERT INTO leave_types(leave_type_name, days_per_year) VALUES
('Casual', 12),
('Sick', 10),
('Annual', 15);

INSERT INTO departments(department_name, department_code) VALUES
('Human Resources', 'HR'),
('Software Engineering', 'ENG'),
('Finance & Accounting', 'FIN'),
('Product Management', 'PROD'),
('Sales & Marketing', 'SALES'),
('IT Support', 'IT');

INSERT INTO public_holidays(holiday_date, holiday_name) VALUES
('2027-01-01', 'New Year''s Day'),
('2027-01-15', 'Pongal'),
('2027-01-16', 'Thiruvalluvar Day'),
('2027-01-17', 'Uzhavar Thirunal'),
('2027-01-26', 'Republic Day'),
('2027-03-10', 'Ramzan (Idul Fitr)'),
('2027-04-14', 'Tamil New Year'),
('2027-05-01', 'May Day'),
('2027-08-15', 'Independence Day'),
('2027-09-04', 'Vinayagar Chathurthi'),
('2027-10-02', 'Gandhi Jayanti'),
('2027-10-08', 'Ayudha Pooja'),
('2027-10-09', 'Vijayadashami'),
('2027-10-28', 'Deepavali'),
('2027-12-25', 'Christmas');

SELECT * FROM public_holidays;

INSERT INTO employees(full_name, email, password_hash, phone, department_id, designation, joining_date, role_id)
VALUES (
'System Admin', 
'admin@codeboard.com',
'$2a$10$4BkwWV284h8me3p2IP1/re9dK1zDZlmPhLMYfaebvUCETJRjUDJsW', 
'9876543212', 
(SELECT ID FROM departments WHERE department_code = 'IT'), 
'Administrator', 
CURdate(), 
(SELECT id FROM roles WHERE role_name = 'ADMIN') 
);

SELECT id, full_name, email, LEFT(password_hash, 7) AS hash_start,
       department_id, role_id, manager_id, is_active
FROM employees;

SHOW TABLES;
SELECT * FROM leave_types;
SELECT * FROM leave_balances;

ALTER TABLE leave_balances MODIFY COLUMN used_days INT DEFAULT 0;

INSERT INTO leave_balances (employee_id, leave_type_id, calendar_year, total_days)
SELECT e.id, lt.id, YEAR(CURDATE()), lt.days_per_year
FROM employees e
JOIN roles r ON r.id = e.role_id
CROSS JOIN leave_types lt
WHERE r.role_name <> 'ADMIN'
  AND NOT EXISTS (SELECT 1 FROM leave_balances lb WHERE lb.employee_id = e.id);

SELECT * FROM leave_balances;
SELECT * FROM employees;

SELECT e.email, lt.leave_type_name, lb.calendar_year, lb.total_days, lb.used_days
FROM leave_balances lb
JOIN employees e ON e.id = lb.employee_id
JOIN leave_types lt ON lt.id = lb.leave_type_id
ORDER BY e.email, lt.leave_type_name;

ALTER TABLE leave_balances MODIFY COLUMN used_days DECIMAL(4,1) DEFAULT 0.0;