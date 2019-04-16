package com.cognibank.securityMicroservice.Controller;

import com.cognibank.securityMicroservice.Exceptions.UserManagementServiceUnavailabeException;
import com.cognibank.securityMicroservice.Exceptions.UserNameOrPasswordWrongException;
import com.cognibank.securityMicroservice.Model.NotificationMessage;
import com.cognibank.securityMicroservice.Model.UserCodes;
import com.cognibank.securityMicroservice.Model.UserDetails;
import com.cognibank.securityMicroservice.Repository.NotificationMessageRepository;
import com.cognibank.securityMicroservice.Service.MainService;
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
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;


import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


@CrossOrigin("*")
@RestController
@Api(description = "Set of endpoints for authentication, generating otp for email and phone userDetails.")
public class MainController {

    @Autowired
    private NotificationMessageRepository notificationMessageRepository;

    @Autowired
    private RabbitSenderService rabbitSenderService;

    @Autowired
    private Environment env;

    @Autowired
    private MainService mainService;

    private Map<String, String> codesWithTypes = new HashMap<>();
    private ConcurrentHashMap<String, UserDetails> userDetailsConcurrentHashMap = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, HttpSession> userOTPSessionConcurrentHashMap = new ConcurrentHashMap<>();


    /**
     * Security send notification to user by email or phone
     *
     * @param emailOrPhone
     */
    @PostMapping(path = "/notification", consumes = "application/json", produces = "application/json")
    public void sendDataToNotification(@RequestBody String emailOrPhone) {

        System.out.print("sendDataToNotification " + emailOrPhone);
    }


    /**
     * Receive data from UI and forward it to UserManagement team and
     * receive email address and phone number from UserManagement and
     * forward email/phone to UI
     *
     * @param user
     * @return user details if status os ok, if not it returns not found status
     */
    @ApiOperation("Returns user details from user management")
    @PostMapping(path = "/loginUser", consumes = "application/json", produces = "application/json")
    public UserDetails loginUser(@RequestBody String user) throws UserManagementServiceUnavailabeException {

        final String uri = env.getProperty("userManagement.getUserDetails");

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> request = new HttpEntity<>(user, headers);

        try {
            ResponseEntity<UserDetails> newUserDetailsEntity = restTemplate.postForEntity(uri, request, UserDetails.class);
            if (newUserDetailsEntity.getStatusCode().equals(HttpStatus.OK)) {

                UserDetails newUserDetails = newUserDetailsEntity.getBody();

                userDetailsConcurrentHashMap.put(newUserDetails.getUserId(), newUserDetails);

                return mainService.maskUserDetails(newUserDetails);
            }

        } catch (HttpServerErrorException e) {
            throw new UserManagementServiceUnavailabeException();
        } catch (HttpClientErrorException e) {
            throw new UserNameOrPasswordWrongException();
        }

        throw new UserNameOrPasswordWrongException();
    }


    /**
     * Receive data from UI and forward it to UserManagement team and
     * receive email address and phone number and forward email/phone to UI
     *
     * @param notificationDetails
     * @param session
     * @return status of OTP
     */
    @ApiOperation("Sending otp to RabbitMQ")
    @PostMapping(path = "/sendOtp", consumes = "application/json", produces = "application/json")
    public ResponseEntity<Map<String, String>> sendOtpToNotification(
            @ApiParam(name = "userId", value = "email", required = true)
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

        UserDetails validateThisUser = userDetailsConcurrentHashMap.get(map.get("userId"));
        if (validateThisUser == null) {
            return new ResponseEntity<Map<String, String>>(HttpStatus.NOT_FOUND);

        }
        //if user present, get email/phone
        if (validateThisUser != null) {
            String type = map.get("type");
            //generate OTP
            String otpCode = mainService.generateOTP();
            if (type.equalsIgnoreCase("email")) {
                value = validateThisUser.getEmail();
            } else {
                value = validateThisUser.getPhone();
            }

            // Started the session for OTP Code
            codesWithTypes.put("otp", otpCode);
            session.setAttribute("otpCode", new UserCodes().withUserId(map.get("userId"))
                    .withCodeTypes(codesWithTypes));
            session.setMaxInactiveInterval(500);

            userOTPSessionConcurrentHashMap.put(validateThisUser.getUserId(), session);
            map.put(type, value);
            //map.remove("userId");
            map.put("code", otpCode);

            //send to notifications --Rabbit MQ
            try {

                NotificationMessage message = new NotificationMessage()
                        .withEmail(map.get("email"))
                        .withType(map.get("type"))
                        .withCode(Long.parseLong(map.get("code")));
                rabbitSenderService.send(message);
                message.withUserId(map.get("userId"));
                notificationMessageRepository.save(message);

            } catch (Exception e) {

                e.printStackTrace();
            }
        }
        return new ResponseEntity<Map<String, String>>(map, HttpStatus.OK);
    }


    /**
     * Received OTP from User and returning authID if authenticated
     *
     * @param userCodes
     * @param request
     * @param response
     * @return authID
     */
    @ApiOperation("Validating the user with otp")
    @PostMapping(path = "/validateUserWithOTP", consumes = "application/json", produces = "application/json")
    public ResponseEntity<String> validateUser(@RequestBody UserCodes userCodes, HttpServletRequest request
            ,HttpServletResponse response) {

        String message = "User not found";
        Long typeValid;

        HttpSession httpSession = userOTPSessionConcurrentHashMap.get(userCodes.getUserId());
        UserCodes validateThisUser = (UserCodes) httpSession.getAttribute("otpCode");
        if (validateThisUser != null) {

            typeValid = validateThisUser.getCodeTypes().keySet().stream().filter(s -> s.equalsIgnoreCase("otp")).count();
            if (typeValid == 0) {
                return new ResponseEntity<String>("Session Expired", HttpStatus.NOT_FOUND);
            }

            if ((userCodes.getCode()).equalsIgnoreCase(validateThisUser.getCodeTypes().get("otp"))) {
                String authCode = mainService.authCodeGenerator(validateThisUser.getUserId());

                //auth code added to response header
                response.addHeader("Authorization", authCode);

                codesWithTypes.put("authID", authCode);
                validateThisUser
                        .withCodeTypes(codesWithTypes);

                httpSession.setAttribute("otpCode", new UserCodes().withUserId(validateThisUser.getUserId())
                        .withCodeTypes(codesWithTypes));
                message = "User found!!";

                HttpSession userSession = request.getSession();
                userSession.setAttribute("LoggedInUser", userCodes.getUserId());

                // clear information about users OTP and temporary details if verified
                userOTPSessionConcurrentHashMap.remove(userCodes.getUserId());
                userDetailsConcurrentHashMap.remove(userCodes.getUserId());

                return new ResponseEntity<String>(message, HttpStatus.OK);
            }

        }
        return new ResponseEntity<String>(message, HttpStatus.NOT_FOUND);
    }





    /**
     * This will be called before every UI call to authenticate the user
     * @param httpServletRequest
     * @param httpSession
     * @return
     */
    @ApiOperation("Checking for user authentication")
    @GetMapping(path = "/auth")
    public ResponseEntity<String> authenticatingTheRequest(HttpServletRequest httpServletRequest, HttpSession httpSession) {
        String message = " user not found";

        String authToCompare = httpServletRequest.getHeader("Authorization");
        UserCodes authFromSession = (UserCodes) httpSession.getAttribute("otpCode");

        if (codesWithTypes.get("authID").equalsIgnoreCase(authToCompare)) {
            message = "User is authenticated";
            return new ResponseEntity<String>(message, HttpStatus.ACCEPTED);
        }

        return new ResponseEntity<String>(message,HttpStatus.UNAUTHORIZED);
    }



//    private boolean isPresent(String authorization) {
//        if (authorization == null || authorization.isEmpty()) {
//            return false;
//        }
//        return true;
//    }





}