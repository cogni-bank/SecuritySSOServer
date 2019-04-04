package com.cognibank.securityMicroservice.Controller;

import com.cognibank.securityMicroservice.Model.NotificationMessage;
import com.cognibank.securityMicroservice.Model.UserCodes;
import com.cognibank.securityMicroservice.Model.UserDetails;
import com.cognibank.securityMicroservice.Repository.UserDetailsRepository;
import com.cognibank.securityMicroservice.Service.RabbitSenderService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.*;

@RestController("/")
@Api(description = "Set of endpoints for authentication, generating otp for email and phone userDetails.")
public class MainController {


    @Autowired
    private UserDetailsRepository userDetailsRepository;

    @Autowired
    private RabbitSenderService rabbitSenderService;

    @Autowired
    private Environment env;

    private Map<String, String> codesWithTypes = new HashMap<>();

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
    @ApiOperation("Returns user details from user management")
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
    @ApiOperation("Sending otp to RabbitMQ")
    @PostMapping(path = "sendOtp", consumes = "application/json", produces = "application/json")
    public ResponseEntity<Map<String,String>> sendOtpToNotification (
            @ApiParam(name="userId", value = "email", required = true)
            @RequestBody String notificationDetails, HttpSession session) {

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
            codesWithTypes.put("otp",otpCode);
            session.setAttribute("otpCode", new UserCodes().withUserId(Long.parseLong(map.get("userId")))
                    .withCodeTypes(codesWithTypes));
            session.setMaxInactiveInterval(500);
            map.put(type,value);
            map.remove("userId");
            map.put("code",otpCode);


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
    @ApiOperation("Validating the user with otp")
    @PostMapping(path = "validateUserWithOTP", consumes = "application/json", produces = "application/json")
    public ResponseEntity<String> validateUser(@RequestBody UserCodes userCodes, HttpServletResponse response, HttpSession session) {

        String message = "User not found";
        Long typeValid;

        UserCodes validateThisUser = (UserCodes) (session.getAttribute("otpCode"));
        if(validateThisUser!= null) {
             typeValid = validateThisUser.getCodeTypes().keySet().stream().filter(s -> s.equalsIgnoreCase("otp")).count();
        if (typeValid == 0) {
            return new ResponseEntity<String>("Session Expired", HttpStatus.NOT_FOUND);
        }
                if ((userCodes.getCode()).equalsIgnoreCase(validateThisUser.getCodeTypes().get("otp"))) {
            String authCode = authCodeGenerator(validateThisUser.getUserId());
            response.addHeader("Authorization", authCode);

            codesWithTypes.put("authID", authCode);
            validateThisUser.withCodeTypes(codesWithTypes);
            session.setAttribute("otpCode", new UserCodes().withUserId(validateThisUser.getUserId())
                    .withCodeTypes(codesWithTypes));
            // userDetailsRepository.deleteById(validateThisUser.getUserId());
            message = "User found!!! Hurray!!";
            return new ResponseEntity<String>(message, HttpStatus.OK);
        }
}
        return new ResponseEntity<String>(message, HttpStatus.NOT_FOUND);

    }



    public String authCodeGenerator(Long userId) {
        String credentials = UUID.randomUUID().toString();

       return  Base64.getEncoder().encodeToString((userId+":"+credentials).getBytes());
    }

    public String generateOTP() {
        int otpNumber = 100000 + new Random().nextInt(900000);
        String otp = Integer.toString(otpNumber);
        return otp;
    }


    @ApiOperation("Checking for user authentication")
    @GetMapping("auth")
    public ResponseEntity<String> authenticatingTheRequest(HttpServletRequest httpServletResponse, HttpSession session) {
        String message=" user not found";
    System.out.println("Hi");
        String authToCompare = httpServletResponse.getHeader("Authorization");

        UserCodes authFromSession =(UserCodes)session.getAttribute("otpCode");


        if(authFromSession.getCodeTypes().get("authID").equalsIgnoreCase(authToCompare)){
            message ="User is authenticated";
            return new ResponseEntity<String>(HttpStatus.OK);
        }

        return new ResponseEntity<String>(HttpStatus.NOT_FOUND);

    }

    @GetMapping(value = "forward")
    public ResponseEntity<String> forwardToMe(HttpServletRequest httpServletRequest, HttpSession httpSession){
        System.out.print("asdadfasd");

        UserCodes authFromSession =(UserCodes)httpSession.getAttribute("otpCode");

            if(authFromSession!=null) {
                if (authFromSession.getCodeTypes().get("authID").equalsIgnoreCase(httpServletRequest.getHeader("Authorization"))) {
                    System.out.println("in accpeted");
                    return new ResponseEntity<String>(HttpStatus.ACCEPTED);
                }
            }
        System.out.println("in unautherized" + new ResponseEntity<String>(HttpStatus.UNAUTHORIZED));
        return new ResponseEntity<String>(HttpStatus.UNAUTHORIZED);
    }

    private boolean isPresent(String authorization) {
        if(authorization==null || authorization.isEmpty())
        {
            return false;
        }
        return true;
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.NOT_FOUND)
    private void userNotFoundHandler(UserNotFoundException ex){

    }

}