package com.sandhyyasofttech.attendsmart.Models;
import java.io.Serializable;
import java.util.Calendar;

public class MonthlyAttendanceSummary implements Serializable {  // ✅ implements Serializable
    public int presentDays = 0;
    public int halfDays = 0;
    public int absentDays = 0;
    public int lateCount = 0;
    public int paidLeavesUsed = 0;
    public int unpaidLeaves = 0;
    public int workingDaysInMonth = 0;  // Dynamic per month
    public int totalDaysInMonth = 0;    // ✅ नवीन: एकूण महिन्यातील दिवस (30/31/28/29)
    public int weeklyHolidayCount = 0;
    public int companyHolidayCount = 0;
}
