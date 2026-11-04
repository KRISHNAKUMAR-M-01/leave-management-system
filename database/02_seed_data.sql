USE leave_management;

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