package com.cognibank.securityMicroservice.Service;

import com.cognibank.securityMicroservice.Model.UserDetails;
import org.springframework.stereotype.Service;

import java.util.Base64;
import java.util.Random;
import java.util.UUID;

@Service
public class MainService {


    /**
     * To mask the data of email and phone
     *
     * @param newUserDetails
     * @return Masked user details
     */
    public UserDetails maskUserDetails(UserDetails newUserDetails) {

        String email = "";
        String phone = "";

        if (newUserDetails.isHasNumber()) {
            phone = newUserDetails.getPhone();
            phone = phone.replace(phone.substring(4, 9), "XXXXX");
            newUserDetails.withPhone(phone);
        }
        if (newUserDetails.isHasEmail()) {
            email = newUserDetails.getEmail();
            email = email.replace(email.substring(1, email.indexOf('@')), "XXX");
            newUserDetails.withEmail(email);
        }
        return newUserDetails;
    }

    public String authCodeGenerator(String userId) {
        String credentials = UUID.randomUUID().toString();
        return Base64.getEncoder().encodeToString((userId + ":" + credentials).getBytes());
    }

    public String generateOTP() {
        int otpNumber = 100000 + new Random().nextInt(900000);
        String otp = Integer.toString(otpNumber);
        return otp;
    }
}
