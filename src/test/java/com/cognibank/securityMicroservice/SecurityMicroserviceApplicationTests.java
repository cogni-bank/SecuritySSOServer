package com.cognibank.securityMicroservice;

import com.cognibank.securityMicroservice.Controller.MainController;
import com.cognibank.securityMicroservice.Model.UserCodes;
import com.cognibank.securityMicroservice.Model.UserDetails;
//import com.cognibank.securityMicroservice.Repository.UserCodesRepository;
//import com.cognibank.securityMicroservice.Repository.UserDetailsRepository;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;


import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
public class SecurityMicroserviceApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @InjectMocks
    private MainController mainController;

    @Before
    public void setUp(){
        MockitoAnnotations.initMocks(this);
        this.mockMvc = MockMvcBuilders.standaloneSetup(MainController.class).build();
    }
    @Test
    public void contextLoads() {
    }


    @Test
    public void userNameAndPasswordInfo() throws Exception {
        this.mockMvc.perform(post("/loginUser").contentType("application/json").content("{\n" +
                "  \"userName\" : \"anil\",\n" +
                "  \"password\" : \"12345\"\n" +
                "}")).andExpect(status().isOk())
                .andDo(print())
                .andExpect(content().string(containsString("aniXXX@gmail.com")));
    }


    @Test
    public void validateUserWithOTPWhenUserIsNotPresent() throws Exception {

        this.mockMvc.perform(post("/validateUserWithOTP").contentType("application/json").content("{\n" +
                "  \"userId\" : 123450988,\n" +
                "  \"code\" : \"1234\"\n" +
                "}")).andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("404 NOT_FOUND")));
    }

    @Test
    public void validateUserWithOTPWhenUserIsPresent() throws Exception {
		Map<String, String> userCodesMap = new HashMap<>();
		userCodesMap.put("otp","123456");
//        MockHttpSession mockSession = new MockHttpSession();
//
//        mockSession.setAttribute("otpCode", new UserCodes()
//                .withUserId(1234L)
//                .withCodeTypes(userCodesMap));

        this.mockMvc.perform(post("/validateUserWithOTP")
               // .session(mockSession)
                .contentType("application/json")
                .sessionAttr("otpCode", new UserCodes()
                        .withUserId("1234")
                        .withCodeTypes(userCodesMap))
                .content("{\n" +
                        "  \"userId\" : " + 1234L + ",\n" +
                        "  \"type\" : \"email\", \n" +
                        "  \"code\" : \"anilvarma@gmail.com\", \n" +
                        "  \"codeTypes\" : {\"otp\":\"123456\"} \n" +
                        "}")).andDo(print()).andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("User found!!")));
    }

    @Test
    public void sendOtpNotifications() throws Exception {
        String userID = "1234";
        UserDetails userDetails = new UserDetails();
        userDetails.setUserId(userID);
        userDetails.setEmail("anilvarma0093@gmail.com");
        userDetails.setPhone("1234567890");
        this.mockMvc.perform(post("/sendOtp").contentType("application/json").content("{\n" +
                "  \"userId\" : " + userID + ",\n" +
                "  \"type\" : \"email\"\n" +
                "}")).andDo(print()).andExpect(status().isOk()).andExpect(content().string(Matchers.containsString("anilvarma0093@gmail.com")));
    }

    @Test
    public void userAuthentication() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/auth")
                .header("Authorization", "MTIzNDU2"))
                .andDo(print()).andExpect(status().isOk());

    }

    @Test
    public void userAuthenticationFailed() throws Exception {

        mockMvc.perform(MockMvcRequestBuilders.get("/auth")
                .header("Authorization", "MTIzND"))
                .andDo(print()).andExpect(status().isNotFound());

    }

}