package com.cognibank.securityMicroservice.Controller;

import com.cognibank.securityMicroservice.Model.NotificationMessage;
import com.cognibank.securityMicroservice.Model.User;
import com.cognibank.securityMicroservice.Model.UserCodes;
import com.cognibank.securityMicroservice.Model.UserDetails;
import com.cognibank.securityMicroservice.Repository.NotificationMessageRepository;
import com.cognibank.securityMicroservice.Repository.UserDetailsRepository;
import com.cognibank.securityMicroservice.Service.RabbitSenderService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import net.minidev.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


@RestController("/")
@Api(description = "Set of endpoints for authentication, generating otp for email and phone userDetails.")
public class MainController {

    @Autowired
    private UserDetailsRepository userDetailsRepository;

    @Autowired
    private NotificationMessageRepository notificationMessageRepository;

    @Autowired
    private RabbitSenderService rabbitSenderService;

    @Autowired
    private Environment env;

    private Map<String, String> codesWithTypes = new HashMap<>();
    private ConcurrentHashMap<String, UserDetails> userDetailsConcurrentHashMap = new ConcurrentHashMap<>();


    /**
     * Security send notification to user by email or phone
     *
     * @param emailOrPhone
     */
    @PostMapping(path = "notification", consumes = "application/json", produces = "application/json")
    public void sendDataToNotification(@RequestBody String emailOrPhone) {

        System.out.print("sendDataToNotification " + emailOrPhone);
    }


    /**
     * Receive data from UI and forward it to UserManagement team and
     * receive email address and phone number from UserManagement and
     * forward email/phone to UI
     *
     * @param user
     * @param session
     * @return user details if status os ok, if not it returns not found status
     */
    @ApiOperation("Returns user details from user management")
    @PostMapping(path = "loginUser", consumes = "application/json", produces = "application/json")
    public ResponseEntity<UserDetails> loginUser(@RequestBody String user, HttpSession session) {


        System.out.println("This is a session" + session);
        final String uri = env.getProperty("userManagement.getUserDetails");

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> request = new HttpEntity<>(user, headers);

        UserDetails newUserDetails = restTemplate.postForObject(uri, request, UserDetails.class);

        userDetailsConcurrentHashMap.put(newUserDetails.getUserId(), newUserDetails);

        if (newUserDetails.getUserId() != null) {
            session.setMaxInactiveInterval(1800);
            return new ResponseEntity<UserDetails>(maskUserDetails(newUserDetails), HttpStatus.OK);
        } else {
            return new ResponseEntity<UserDetails>(HttpStatus.NOT_FOUND);
        }
    }

    /**
     * Receive data from UI and forward it to UserManagement team and
     * receive email address and phone number from UserManagement and
     * forward email/phone to UI
     *
     * @param user
     * @param session
     * @return user details if status os ok, if not it returns not found status
     */
    @ApiOperation("Returns user details from user management")
    @PostMapping(path = "logoutUser", consumes = "application/json", produces = "application/json")
    public void logoutUser(@RequestBody String user, HttpSession session) {
        session.invalidate();
    }

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


    /**
     * Receive data from UI and forward it to UserManagement team and
     * receive email address and phone number and forward email/phone to UI
     *
     * @param notificationDetails
     * @param session
     * @return status of OTP
     */
    @ApiOperation("Sending otp to RabbitMQ")
    @PostMapping(path = "sendOtp", consumes = "application/json", produces = "application/json")
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
            String otpCode = generateOTP();
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
            map.put(type, value);
            map.remove("userId");
            map.put("code", otpCode);


            //send to notifications --Rabbit MQ
            try {
                NotificationMessage message = new NotificationMessage()
                        .withEmail(map.get("email"))
                        .withType(map.get("type"))
                        .withCode(Long.parseLong(map.get("code")));
                notificationMessageRepository.save(message);
                rabbitSenderService.send(message);
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
    @PostMapping(path = "validateUserWithOTP", consumes = "application/json", produces = "application/json")
    public ResponseEntity<String> validateUser(@RequestBody UserCodes userCodes, HttpServletRequest request, HttpServletResponse response) {

        HttpSession httpSession = request.getSession();

        String message = "User not found";
        Long typeValid;

        System.out.println(userCodes.getUserId());
        System.out.println(userCodes.getCode());

        UserCodes validateThisUser = (UserCodes) httpSession.getAttribute("otpCode");
        if (validateThisUser != null) {
            typeValid = validateThisUser.getCodeTypes().keySet().stream().filter(s -> s.equalsIgnoreCase("otp")).count();
            if (typeValid == 0) {
                return new ResponseEntity<String>("Session Expired", HttpStatus.NOT_FOUND);
            }

            if ((userCodes.getCode()).equalsIgnoreCase(validateThisUser.getCodeTypes().get("otp"))) {
                String authCode = authCodeGenerator(validateThisUser.getUserId());
                response.addHeader("Authorization", authCode);

                codesWithTypes.put("authID", authCode);
                validateThisUser.withCodeTypes(codesWithTypes);
                httpSession.setAttribute("otpCode", new UserCodes().withUserId(validateThisUser.getUserId())
                        .withCodeTypes(codesWithTypes));
                message = "User found!!";
                return new ResponseEntity<String>(message, HttpStatus.OK);
            }

        }
        return new ResponseEntity<String>(message, HttpStatus.NOT_FOUND);
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


    @ApiOperation("Checking for user authentication")
    @GetMapping(path = "auth")
    public ResponseEntity<String> authenticatingTheRequest(HttpServletRequest httpServletRequest, HttpSession httpSession) {
        String message = " user not found";

        String authToCompare = httpServletRequest.getHeader("Authorization");

        Enumeration<String> attributeNames = httpSession.getAttributeNames();
        UserCodes authFromSession = (UserCodes) httpSession.getAttribute("otpCode");


        if (authFromSession.getCodeTypes().get("authID").equalsIgnoreCase(authToCompare)) {
            message = "User is authenticated";
            return new ResponseEntity<String>(HttpStatus.OK);
        }

        return new ResponseEntity<String>(HttpStatus.NOT_FOUND);

    }

    @GetMapping(path = "forward")
    public ResponseEntity<String> forwardToMe(HttpServletRequest httpServletRequest, HttpSession httpSession) {

        UserCodes authFromSession = (UserCodes) httpSession.getAttribute("otpCode");

        if (authFromSession != null) {
            if (authFromSession.getCodeTypes().get("authID").equalsIgnoreCase(httpServletRequest.getHeader("Authorization"))) {
                System.out.println("in accpeted");
                return new ResponseEntity<String>(HttpStatus.ACCEPTED);
            }
        }
        return new ResponseEntity<String>("Is not Authorized User", HttpStatus.UNAUTHORIZED);
    }

    private boolean isPresent(String authorization) {
        if (authorization == null || authorization.isEmpty()) {
            return false;
        }
        return true;
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.NOT_FOUND)
    private void userNotFoundHandler(UserNotFoundException ex) {

    }

}