package com.cognibank.securityMicroservice.Model;

import java.io.Serializable;
import java.util.Map;

public class UserCodes implements Serializable {

    private String userId;
    private String type;
    private String code;

    private Map<String, String> codeTypes;


    public UserCodes withUserId(String userId){
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

    public String getUserId() {
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