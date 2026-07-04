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
        int workingDays = config.workingDays;

        if (workingDays <= 0) {
            workingDays = 30;
        }

        double perDay = config.monthlySalary / workingDays;
        result.perDaySalary = perDay;

        // Payable days = present + (half * 0.5) + paid leaves
        double payableDays =
                summary.presentDays
                        + (summary.halfDays * 0.5)
                        + summary.paidLeavesUsed
                        + summary.weeklyHolidayCount
                        + summary.companyHolidayCount;


// ================= LATE RULE =================

        double lateDeductionDays = 0.0;

        String lateRule = config.lateRule == null
                ? ""
                : config.lateRule.trim();

        if ("3 Late marks = 0.5 Day deduction".equalsIgnoreCase(lateRule)) {

            lateDeductionDays =
                    (summary.lateCount / 3) * 0.5;

        } else if ("5 Late marks = 1 Day deduction".equalsIgnoreCase(lateRule)) {

            lateDeductionDays =
                    (summary.lateCount / 5) * 1.0;
        }


// Late deduction apply
        payableDays = payableDays - lateDeductionDays;

        result.lateDeductionDays = lateDeductionDays;
        result.lateDeductionAmount = lateDeductionDays * perDay;


// Negative payable days nako
        if (payableDays < 0) {
            payableDays = 0;
        }


// Maximum configured working days
        if (payableDays > workingDays) {
            payableDays = workingDays;
        }

        result.payableDays = payableDays;


        android.util.Log.d(
                "SALARY_LATE_DEBUG",
                "LateCount=" + summary.lateCount +
                        ", LateRule=" + lateRule +
                        ", LateDeductionDays=" + lateDeductionDays +
                        ", FinalPayableDays=" + payableDays
        );

// Never pay more than configured working days
//        if (payableDays > config.workingDays) {
//            payableDays = config.workingDays;
//        }

        // Never pay more than configured working days
        if (payableDays > workingDays) {
            payableDays = workingDays;
        }
        result.payableDays = payableDays;

        android.util.Log.d(
                "SALARY_DEBUG",
                "Present=" + summary.presentDays +
                        ", Half=" + summary.halfDays +
                        ", PaidLeave=" + summary.paidLeavesUsed +
                        ", WeeklyHoliday=" + summary.weeklyHolidayCount +
                        ", CompanyHoliday=" + summary.companyHolidayCount +
                        ", PayableDays=" + payableDays +
                        ", PerDay=" + perDay +
                        ", Gross=" + (payableDays * perDay)
        );

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