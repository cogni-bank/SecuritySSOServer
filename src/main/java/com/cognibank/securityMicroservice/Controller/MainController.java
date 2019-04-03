package com.cognibank.securityMicroservice.Controller;

import com.cognibank.securityMicroservice.Model.NotificationMessage;
import com.cognibank.securityMicroservice.Model.UserCodes;
import com.cognibank.securityMicroservice.Model.UserDetails;
import com.cognibank.securityMicroservice.Repository.UserDetailsRepository;
import com.cognibank.securityMicroservice.Repository.UserCodesRepository;
import com.cognibank.securityMicroservice.Service.RabbitSenderService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.swing.text.html.parser.Entity;
import java.util.*;

@RestController("/")
public class MainController {

    @Autowired
    private UserCodesRepository userCodesRepository;

    @Autowired
    private UserDetailsRepository userDetailsRepository;

    @Autowired
    private RabbitSenderService rabbitSenderService;

    @Autowired
    private Environment env;

    @GetMapping("helloWorld")
    public String HelloWorld(HttpSession session) {
        System.out.print("sadsdisand ");
        session.
                setAttribute("favoriteColors", "asdasdfasd");

        return "hello"+session.getAttribute("favoriteColors");
    }

    //Can
    @PostMapping (path = "userManagement" , consumes = "application/json", produces = "application/json")
    public UserDetails sendDataToUserManagement(@RequestBody String user) {
       System.out.print("sendDataToUserManagement " + user);

        UserDetails mailAndPhone = new UserDetails();
        mailAndPhone.setUserId(123456);
        mailAndPhone.setEmail("anilvarma@gmail.com");
        mailAndPhone.setPhone("+11234567890");

        return mailAndPhone;
    }

    //Amit
    @PostMapping(path = "notification" , consumes = "application/json", produces = "application/json")
    public void sendDataToNotification(@RequestBody String emailOrPhone) {
        System.out.print("sendDataToNotification " + emailOrPhone);
    }





    //Receive data from UI and forward it to UserManagement team and receive email address and phone number and forward email/phone to UI
    @PostMapping(path = "loginUser", consumes = "application/json", produces = "application/json")
    public ResponseEntity<UserDetails> loginUser (@RequestBody String user , HttpSession session) {

        System.out.println(user);
        final String uri = env.getProperty("userManagement.getUserDetails");
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> request = new HttpEntity<>(user, headers);
        UserDetails userObjFromUserManagement =  restTemplate.postForObject(uri, request, UserDetails.class);
        System.out.println(("userObjFromUserManagement sending to UM " ) + userObjFromUserManagement);
      userDetailsRepository.save(userObjFromUserManagement);
       //session.setAttribute("UserDetails",userObjFromUserManagement);

      System.out.println(session.getAttribute("UserDetails"));
        System.out.println("Data sent to user----> " + maskUserDetails(userObjFromUserManagement).toString());

        if(userObjFromUserManagement.getUserId()== 0){
            return new ResponseEntity<UserDetails>(HttpStatus.NOT_FOUND);

        }
        return new ResponseEntity<UserDetails>(maskUserDetails(userObjFromUserManagement), HttpStatus.OK);
    }

    //to mask the data of email and phone
    public UserDetails maskUserDetails(UserDetails toMaskUserDetails){

        String emailID = toMaskUserDetails.getEmail();
        String emailIDFormatted = emailID.replace(emailID.substring(3,emailID.indexOf('@')), "XXX");
        toMaskUserDetails.setEmail(emailIDFormatted);

        String phone = toMaskUserDetails.getPhone();
        String phoneFormatted = phone.replace(phone.substring(4,9), "XXXXX");
        toMaskUserDetails.setPhone(phoneFormatted);

        return toMaskUserDetails;
    }

    //Receive data from UI and forward it to UserManagement team and receive email address and phone number and forward email/phone to UI
    @PostMapping(path = "sendOtp", consumes = "application/json", produces = "application/json")
    public ResponseEntity<Map<String,String>> sendOtpToNotification (@RequestBody String notificationDetails, HttpSession session) {

        System.out.print(notificationDetails);

        ObjectMapper mapper = new ObjectMapper();
        String value;
        Map<String, String> map = new HashMap<>();

        try {
            map = mapper.readValue(notificationDetails, new TypeReference<Map<String, String>>() {
            });

        } catch (Exception e) {
            e.printStackTrace();
        }

        Optional<UserDetails> validateThisUser = userDetailsRepository.findById(Long.parseLong(map.get("userId")));

        if(!validateThisUser.isPresent()){
            return new ResponseEntity<Map<String, String>>(HttpStatus.NOT_FOUND);
        }
        //if user present, get email/phone
       if (validateThisUser.isPresent()) {
            String type = map.get("type");
            //generate OTP
            String otpCode = generateOTP();
            if (type.equalsIgnoreCase("email")) {
                value = validateThisUser.get().getEmail();
            } else {
                value = validateThisUser.get().getPhone();
            }

            // Started the session for OTP Code
            session.setAttribute("otpCode", new UserCodes().withUserId(Long.parseLong(map.get("userId")))
                                                                  .withCode(otpCode)
                                                                    .withType("otp"));



            session.setMaxInactiveInterval(30);

            map.put(type,value);
            map.remove("userId");
            map.put("code",otpCode);



            System.out.println("Map:" + map);

            //send to notifications --Rabbit MQ
            try {
                NotificationMessage message = new NotificationMessage().withEmail(map.get("email")).withType(map.get("type")).withCode(Long.parseLong(map.get("code")));
                rabbitSenderService.send(message);
            }catch(Exception e){
                e.printStackTrace();
            }
       }
        return new ResponseEntity<Map<String, String>>(map,HttpStatus.OK);
    }


        //Recieved OTP from User and returning authID if authenticated
    @PostMapping(path = "validateUserWithOTP", consumes = "application/json", produces = "application/json")
    public ResponseEntity<String> validateUser(@RequestBody UserCodes userCodes, HttpServletResponse response, HttpSession session){

        String message = "User not found";
        // Optional<UserCodes> validateThisUser = userCodesRepository.findById(userCodes.getUserId());

        UserCodes validateThisUser = (UserCodes) (session.getAttribute("otpCode"));


        System.out.println(validateThisUser);
        if(validateThisUser == null || !(validateThisUser.getType().equalsIgnoreCase("otp"))){
            return new ResponseEntity<>(message, HttpStatus.NOT_FOUND);
        }
       if(validateThisUser!=null && validateThisUser.getType().equalsIgnoreCase("otp")) {
            if ((userCodes.getCode()).equalsIgnoreCase(validateThisUser.getCode())) {
                String authCode = authCodeGenerator();
                response.addHeader("Authorization", authCode);
                validateThisUser.setType("authID");
                validateThisUser.setCode(authCode);
                userCodesRepository.save(validateThisUser);
                session.setAttribute("Auth",new UserCodes().withUserId(validateThisUser.getUserId())
                        .withCode(authCode)
                        .withType("Auth"));
                userDetailsRepository.deleteById(validateThisUser.getUserId());
                System.out.println("validateThisUser.toString() ----------------------------> " + validateThisUser.toString());
               message = "User found!!! Hurray!!";
            }
        }
       return new ResponseEntity<String>(message, HttpStatus.ACCEPTED);

    }

    public String authCodeGenerator() {
        String credentials = UUID.randomUUID().toString();
        return credentials;
    }

    public String generateOTP() {
        int otpNumber = 100000 + new Random().nextInt(900000);
        String otp = Integer.toString(otpNumber);
        System.out.println(otp);
        return otp;
    }

//    @GetMapping("auth/{userId}")
//    public ResponseEntity<String> authenticatingTheRequest(@PathVariable Long id, HttpServletResponse httpServletResponse, HttpSession httpSession ) {
//
//        String authToCompare = httpServletResponse.getHeader("Authorization");
//
//        UserCodes authFromSession =(UserCodes)httpSession.getAttribute("Auth");
//
//       // if(authFromSession.)
//
//
//
//
//        return null;
//    }

//    @ExceptionHandler
//    @ResponseStatus(HttpStatus.NOT_FOUND)
//    private void carNotFoundHandler(CarNotFoundException ex){
//
//    }

}
