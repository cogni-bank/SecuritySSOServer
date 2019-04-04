package com.cognibank.securityMicroservice.Model;

import javax.persistence.Entity;
import javax.persistence.Id;
import java.io.Serializable;
import java.util.Map;

//@Entity
public class UserCodes implements Serializable {

    private static final long serialversionUID =
            12L;

  //  @Id
    private long userId;
    private String type;
    private String code;
    private Map<String, String> codeTypes;


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

    public UserCodes withCodeTypes(Map<String,String> codes){
        this.codeTypes = codes;
        return this;
    }

    public Map<String, String> getCodeTypes() {
        return codeTypes;
    }

    public long getUserId() {
        return userId;
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
                ", codeTypes=" + codeTypes +
                '}';
    }
}