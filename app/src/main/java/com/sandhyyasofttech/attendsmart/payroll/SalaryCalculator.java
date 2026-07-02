package com.sandhyyasofttech.attendsmart.payroll;

import com.sandhyyasofttech.attendsmart.Models.MonthlyAttendanceSummary;
import com.sandhyyasofttech.attendsmart.Models.SalaryCalculationResult;
import com.sandhyyasofttech.attendsmart.Models.SalaryConfig;

public class SalaryCalculator {

    public static SalaryCalculationResult calculateSalary(
            MonthlyAttendanceSummary summary,
            SalaryConfig config
    ) {
        SalaryCalculationResult result = new SalaryCalculationResult();

        // ✅ महिन्यातील एकूण दिवस वापर (30/31/28/29)
        int totalDaysInMonth = summary.totalDaysInMonth;

        // ✅ जर total days 0 असेल तर default 30 वापर
        if (totalDaysInMonth <= 0) {
            totalDaysInMonth = 30;
        }

        if (config.monthlySalary <= 0) {
            return result;
        }

// ✅ Per day = Monthly Salary ÷ Configured Working Days
        int workingDays = config.workingDays > 0
                ? config.workingDays
                : summary.workingDaysInMonth;

        double perDay = config.monthlySalary / workingDays;
        result.perDaySalary = perDay;

        // Payable days = present + (half * 0.5) + paid leaves
        double payableDays = summary.presentDays + (summary.halfDays * 0.5) + summary.paidLeavesUsed;
        result.payableDays = payableDays;

        // ✅ Gross = Payable days × Per day
        result.grossSalary = payableDays * perDay;

        double pf = 0, esi = 0, other = 0;

        if (config.deductionEnabled) {
            pf = result.grossSalary * (config.pfPercent / 100.0);
            esi = result.grossSalary * (config.esiPercent / 100.0);
            other = config.otherDeduction;
        }

        result.pfAmount = pf;
        result.esiAmount = esi;
        result.otherDeduction = other;
        result.totalDeduction = pf + esi + other;
        result.netSalary =
                Math.max(
                        0,
                        result.grossSalary - result.totalDeduction
                );
        return result;
    }

}