package com.cognibank.securityMicroservice.Model;

import javax.persistence.*;
import java.io.Serializable;


public class UserDetails {

    private String userId;
    private String email;
    private String phone;
    private boolean hasNumber;
    private boolean hasEmail;

    public boolean isHasNumber() {
        return hasNumber;
    }

    public void setHasNumber(boolean hasNumber) {
        this.hasNumber = hasNumber;
    }

    public boolean isHasEmail() {
        return hasEmail;
    }

    public void setHasEmail(boolean hasEmail) {
        this.hasEmail = hasEmail;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
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

    public UserDetails withUserId(String userId) {

        this.userId = userId;
        return this;
    }

    public UserDetails withEmail(String email) {
        this.email = email;
        return this;
    }
    public UserDetails withPhone(String phone) {

        this.phone = phone;
        return this;
    }



    @Override
    public String toString() {
        return "UserDetails{" +
                "userId=" + userId +
                ", email='" + email + '\'' +
                ", phone='" + phone + '\'' +
                '}';
    }
}

