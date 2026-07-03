package com.sandhyyasofttech.attendsmart.Models;

public class CompanyHolidayModel {

    public String holidayId;
    public String holidayName;
    public String holidayDate;
    public boolean paid;

    public CompanyHolidayModel() {
    }

    public CompanyHolidayModel(String holidayName,
                               String holidayDate,
                               boolean paid) {

        this.holidayName = holidayName;
        this.holidayDate = holidayDate;
        this.paid = paid;
    }
}