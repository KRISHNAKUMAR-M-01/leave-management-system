import { Component, inject, OnInit, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { Employee, EmployeeFilter } from '../../../core/models/employee.model';
import { EmployeeService } from '../../../core/services/employee.service';
import { NotificationService } from '../../../core/services/notification.service';
import { getErrorMessage } from '../../../core/utils/error-message';
import { MatDialog } from '@angular/material/dialog';
import { EmployeeFormDialog } from '../../../shared/employee-form-dialog/employee-form-dialog';
import { ConfirmDialog } from '../../../shared/confirm-dialog/confirm-dialog';
import { RouterLink } from '@angular/router';

const PAGE_SIZE = 10;

@Component({
  selector: 'app-employee-list',
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatPaginatorModule,
    MatProgressSpinnerModule,
    MatSelectModule,
    MatTableModule,
    MatTooltipModule,
    RouterLink,
  ],
  templateUrl: './employee-list.html',
  styleUrl: './employee-list.scss',
})
export class EmployeeList implements OnInit {
  private readonly employeeService = inject(EmployeeService);
  private readonly notification = inject(NotificationService);
  private readonly formBuilder = inject(NonNullableFormBuilder);
  private readonly dialog = inject(MatDialog);

  protected readonly employees = signal<Employee[]>([]);
  protected readonly totalElements = signal(0);
  protected readonly currentPage = signal(0);
  protected readonly loading = signal(true);
  protected readonly displayedColumns = [
    'fullName',
    'email',
    'departmentName',
    'roleName',
    'active',
    'actions',
  ];

  protected readonly searchControl = this.formBuilder.control('');

  protected readonly sortBy = signal('fullName');
  protected readonly sortDirection = signal<'asc' | 'desc'>('asc');

  // Load the initial employee list when the component initializes.
  ngOnInit(): void {
    this.loadPage(0);
  }

  // Search employees using the entered search value.
  protected onSearch(): void {
    this.loadPage(0);
  }

  // Change the sorting column or direction.
  protected toggleSort(column: string): void {
    if (this.sortBy() === column) {
      this.sortDirection.set(this.sortDirection() === 'asc' ? 'desc' : 'asc');
    } else {
      this.sortBy.set(column);
      this.sortDirection.set('asc');
    }
    this.loadPage(0);
  }

  // Handle pagination changes and load the selected page
  protected onPageChange(event: PageEvent): void {
    this.loadPage(event.pageIndex);
  }

  // Load employees based on the current search, sorting and pagination settings.
  protected loadPage(page: number): void {
    this.loading.set(true);
    const filter: EmployeeFilter = {
      search: this.searchControl.value,
      page,
      size: PAGE_SIZE,
      sortBy: this.sortBy(),
      direction: this.sortDirection(),
    };

    this.employeeService.search(filter).subscribe({
      next: (result) => {
        this.employees.set(result.content);
        this.totalElements.set(result.totalElements);
        this.currentPage.set(result.page);
        this.loading.set(false);
      },
      error: (error: unknown) => {
        this.notification.error(getErrorMessage(error));
        this.loading.set(false);
      },
    });
  }

  // Open the dialog to add a new employee.
  protected openAddDialog(): void {
    this.dialog
      .open(EmployeeFormDialog, { data: { employee: null }, width: '640px', maxWidth: '95vw' })
      .afterClosed()
      .subscribe((result) => {
        if (result) {
          this.notification.success('Employee created');
          this.loadPage(this.currentPage());
        }
      });
  }

  // Open the dialog to edit the selected employee.
  protected openEditDialog(employee: Employee): void {
    this.dialog
      .open(EmployeeFormDialog, { data: { employee }, width: '640px', maxWidth: '95vw' })
      .afterClosed()
      .subscribe((result) => {
        if (result) {
          this.notification.success('Employee updated');
          this.loadPage(this.currentPage());
        }
      });
  }

  // Activate or deactivate an employee after confirmation
  protected toggleActive(row: Employee): void {
    const action = row.active ? 'Deactivate' : 'Activate';
    this.dialog
      .open(ConfirmDialog, {
        data: {
          title: `${action} employee`,
          message: `${action} ${row.fullName}'s account?`,
          confirmLabel: action,
          destructive: row.active,
        },
      })
      .afterClosed()
      .subscribe((confirmed: boolean) => {
        if (!confirmed) return;
        const request$ = row.active
          ? this.employeeService.deactivate(row.id)
          : this.employeeService.activate(row.id);

        request$.subscribe({
          next: () => {
            this.notification.success(`Employee ${action.toLowerCase()}d`);
            this.loadPage(this.currentPage());
          },
          error: (error: unknown) => this.notification.error(getErrorMessage(error)),
        });
      });
  }

  // Delete the selected employee after confirmation.
  protected deleteEmployee(row: Employee): void {
    this.dialog
      .open(ConfirmDialog, {
        data: {
          title: 'Delete employee',
          message: `Permanently delete ${row.fullName}? This cannot be undone.`,
          confirmLabel: 'Delete',
          destructive: true,
        },
      })
      .afterClosed()
      .subscribe((confirmed: boolean) => {
        if (!confirmed) return;
        this.employeeService.delete(row.id).subscribe({
          next: () => {
            this.notification.success('Employee deleted');
            this.loadPage(this.currentPage());
          },
          error: (error: unknown) => this.notification.error(getErrorMessage(error)),
        });
      });
  }
}
