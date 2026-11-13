import { Component, computed, input } from '@angular/core';

const RADIUS = 36;
const CIRCUMFERENCE = 2 * Math.PI * RADIUS;

@Component({
  selector: 'app-balance-ring',
  templateUrl: './balance-ring.html',
  styleUrl: './balance-ring.scss',
})
export class BalanceRing {
  readonly label = input.required<string>();
  readonly used = input.required<number>();
  readonly total = input.required<number>();

  protected readonly radius = RADIUS;
  protected readonly circumference = CIRCUMFERENCE;

  protected readonly remainingPercent = computed(() => {
    const total = this.total();
    return total > 0 ? (total - this.used()) / total : 1;
  });

  // dash length representing the remaining (filled) portion
  protected readonly dashArray = computed(() => {
    const filled = this.remainingPercent() * this.circumference;
    return `${filled} ${this.circumference}`;
  });

  protected readonly remaining = computed(() => this.total() - this.used());

  //true when ≤ 25% of balance remains - ring turns amber
  protected readonly lowBalance = computed(() => this.remainingPercent() <= 0.25);
}
