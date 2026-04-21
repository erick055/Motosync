package com.example.motosync;

public class Appointment {
    public String appointmentId;
    public String customerName;
    public String customerEmail;
    public String serviceType;
    public String vehicleDetails;
    public String date;
    public String status; // "Pending", "Approved", "Declined", "Completed"

    // Required empty constructor for Firebase
    public Appointment() {
    }

    public Appointment(String appointmentId, String customerName, String customerEmail, String serviceType, String vehicleDetails, String date, String status) {
        this.appointmentId = appointmentId;
        this.customerName = customerName;
        this.customerEmail = customerEmail;
        this.serviceType = serviceType;
        this.vehicleDetails = vehicleDetails;
        this.date = date;
        this.status = status;
    }
}