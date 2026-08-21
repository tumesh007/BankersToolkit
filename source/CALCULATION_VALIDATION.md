# Calculation Validation Model & Reference Notes

This document defines the mathematical models, validation rules, rounding handling, and zero-interest boundary conditions for the **Banker's Toolkit** financial calculation engine (`:core:domain` and `:feature:emi-calculator` / `:feature:amortization`).

---

## 1. EMI Calculation Formula

The standard reducing-balance Equated Monthly Installment (EMI) is computed using:

$$\text{EMI} = P \times r \times \frac{(1+r)^n}{(1+r)^n - 1}$$

where:
- **P**: Principal loan amount
- **r**: Monthly interest rate = $\frac{\text{Annual Interest Rate}}{12 \times 100}$
- **n**: Tenure in months

### Edge Case: Zero Interest ($r = 0$)
When interest rate is 0%:

$$\text{EMI} = \frac{P}{n}$$

- **Total Interest** = 0.00
- **Total Repayment** = $P$

---

## 2. Amortization Schedule & Convergence Rules

For each installment month $k \in \{1, \dots, n\}$:

1. **Opening Balance ($B_{k-1}$)**: Outstanding principal at start of month $k$. ($B_0 = P$)
2. **Interest Component ($I_k$)**: $I_k = B_{k-1} \times r$
3. **Principal Component ($P_k$)**: $P_k = \text{EMI} - I_k$
4. **Closing Balance ($B_k$)**: $B_k = B_{k-1} - P_k$

### Floating Point Rounding & Final Installment Adjustment
Due to floating-point representation, the raw sum of principal components across $n$ months may differ by fractions of a cent from $P$.
To ensure exact convergence ($B_n = 0.00$):
- The final installment $n$ sets $P_n = B_{n-1}$, and the closing balance $B_n$ is strictly set to `0.00`.

---

## 3. Comparison with Excel Loan Models

Standard Excel PMT formula equivalent:
`=PMT(rate/12, nper, -pv)`

### Rounding & Formatting Standard:
- UI displays monetary amounts rounded to 2 decimal places (`%.2f`).
- Core math uses double-precision floating point.
