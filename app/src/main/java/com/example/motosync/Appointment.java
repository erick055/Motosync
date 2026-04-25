package com.example.motosync;

public class Appointment {
    public String appointmentId;
    public String customerName;
    public String customerEmail;
    public String userId; // <-- NEW SECURE IDENTIFIER
    public String serviceType;
    public String vehicleDetails;
    public String date;
    public String status;

    public Appointment() {
        // Required empty constructor
    }

    public Appointment(String appointmentId, String customerName, String customerEmail, String userId, String serviceType, String vehicleDetails, String date, String status) {
        this.appointmentId = appointmentId;
        this.customerName = customerName;
        this.customerEmail = customerEmail;
        this.userId = userId;
        this.serviceType = serviceType;
        this.vehicleDetails = vehicleDetails;
        this.date = date;
        this.status = status;
    }
}