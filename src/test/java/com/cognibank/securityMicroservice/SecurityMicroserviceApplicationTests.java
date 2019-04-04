package com.cognibank.securityMicroservice;

import com.cognibank.securityMicroservice.Model.UserCodes;
import com.cognibank.securityMicroservice.Model.UserDetails;
//import com.cognibank.securityMicroservice.Repository.UserCodesRepository;
import com.cognibank.securityMicroservice.Repository.UserDetailsRepository;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.hamcrest.CoreMatchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

//	@Autowired
//	private UserCodesRepository userCodesRepository;

	@Autowired
	private UserDetailsRepository userDetailsRepository;

	@Test
	public void contextLoads() {
	}

	@Test
	public void helloWorldTesting() throws Exception {
		this.mockMvc.perform(get("/helloWorld")).andExpect(status().isOk ())
				.andExpect(content ().string(containsString ("hello")));
	}

	@Test
	public void userNameAndPasswordInfo() throws Exception {
		this.mockMvc.perform(post("/loginUser").contentType("application/json").content("{\n" +
				"  \"userName\" : \"anil\",\n" +
				"  \"password\" : \"12345\"\n" +
				"}")).andExpect(status().isOk()).andDo(print ()).andExpect(content ().string(containsString ("aniXXX@gmail.com")));
	}


	@Test
	public void validateUserWithOTPWhenUserIsNotPresent() throws Exception {

		this.mockMvc.perform(post("/validateUserWithOTP").contentType("application/json").content("{\n" +
				"  \"userId\" : 123450988,\n" +
				"  \"code\" : \"1234\"\n" +
				"}")).andDo(print ()).andExpect(status().isOk()).andExpect(content ().string(Matchers.containsString ("User not found")));
	}

//	@Test
//	public void validateUserWithOTPWhenUserIsPresent() throws Exception {
//
//		Long userID = 1234L;
//		UserCodes newUser = new UserCodes();
//		newUser.setUserId(userID);
//		newUser.setCode("1234");
//		newUser.setType("otp");
//		userCodesRepository.save(newUser);
//		this.mockMvc.perform(post("/validateUserWithOTP").contentType("application/json").content("{\n" +
//				"  \"userId\" : " + userID + ",\n" +
//				"  \"code\" : \"1234\"\n" +
//				"}")).andDo(print ()).andExpect(status().isOk()).andExpect(content ().string(Matchers.containsString ("User found!!! Hurray!!")));
//	}

	@Test
	public void sendOtpNotifications() throws Exception{
		Long userID = 1234L;
		UserDetails userDetails = new UserDetails();
		userDetails.setUserId(userID);
		userDetails.setEmail("anilvarma0093@gmail.com");
		userDetails.setPhone("1234567890");
		userDetailsRepository.save(userDetails);
		this.mockMvc.perform(post("/sendOtp").contentType("application/json").content("{\n" +
				"  \"userId\" : " + userID + ",\n" +
				"  \"type\" : \"email\"\n" +
				"}")).andDo(print ()).andExpect(status().isOk()).andExpect(content ().string(Matchers.containsString ("anilvarma0093@gmail.com")));
	}

	@Test
	public void userAuthentication() throws Exception{
		mockMvc.perform(MockMvcRequestBuilders.get("/auth").header("Authorization", "MTIzNDU2"))
				.andDo(print ()).andExpect(status().isOk());

	}

	@Test
	public void userAuthenticationFailed() throws Exception{

		mockMvc.perform(MockMvcRequestBuilders.get("/auth").header("Authorization", "MTIzND"))
				.andDo(print ()).andExpect(status().isNotFound());

	}

}