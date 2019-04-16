package com.cognibank.securityMicroservice.Controller;

import io.swagger.annotations.ApiOperation;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpSession;

@CrossOrigin("*")
@RestController
public class LogoutContoller {

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
    @PostMapping(path = "/logoutUser", consumes = "application/json", produces = "application/json")
    public ResponseEntity<String> logoutUser(@RequestBody String user, HttpSession session) throws UserSessionExpiredException {
        System.out.println("From the logoutuser");
        try{


            session.removeAttribute("LoggedInUser");
            System.out.println("after session attr removeds...");

            session.setMaxInactiveInterval(0);
            session.invalidate();
            System.out.println("after session invalidate...");


        } catch (Exception e) {
            throw new UserSessionExpiredException("Logout Failure");
        }
        //session.removeAttribute("logedInUser");
        return new ResponseEntity<String>("Logout Successful", HttpStatus.OK);
    }


}
