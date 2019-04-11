package com.cognibank.securityMicroservice.Model;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class NotificationMessage {

    @Id
    private long userId;

    private String email;
    private String phone;
    private long code;
    private String type;

    public NotificationMessage() {

    }

    public NotificationMessage withUserId (final long userId) {
        setUserId(userId);
        return this;
    }

    public NotificationMessage withEmail (final String emailId) {
        setEmail(emailId);
        return this;
    }

    public NotificationMessage withPhone (final String phone) {
        setPhone(phone);
        return this;
    }

    public NotificationMessage withCode (final long code) {
        setCode(code);
        return this;
    }

    public NotificationMessage withType (final String type) {
        setType(type);
        return this;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }


    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public long getCode() {
        return code;
    }

    public void setCode(long code) {
        this.code = code;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return "{" +
                "email:'" + email + '\'' +
                ", phone:'" + phone + '\'' +
                ", code:" + code +
                ", type:'" + type + '\'' +
                '}';
    }
}