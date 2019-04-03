package com.cognibank.securityMicroservice.Model;

import javax.persistence.Entity;
import javax.persistence.Id;
import java.io.Serializable;

@Entity
public class UserCodes implements Serializable {

    private static final long serialversionUID =
            12L;

    @Id
    private long userId;
    private String type;
    private String code;


    public UserCodes withUserId(long userId){
        this.userId = userId;
        return this;
    }
    public UserCodes withType(String type){
        this.type = type;
        return this;
    }
    public UserCodes withCode(String code){
        this.code = code;
        return this;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    @Override
    public String toString() {
        return "UserCodes{" +
                "userId=" + userId +
                ", type='" + type + '\'' +
                ", code='" + code + '\'' +
                '}';
    }
}
