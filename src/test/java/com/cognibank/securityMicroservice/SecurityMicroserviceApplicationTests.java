package com.cognibank.securityMicroservice;

import com.cognibank.securityMicroservice.Model.User;
import com.cognibank.securityMicroservice.Repository.UserRepository;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

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

	@Autowired
	private UserRepository userRepository;

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
	public void userNameAndPasswordInfo2() throws Exception {
		this.mockMvc.perform(post("/loginUser2").contentType("application/json").content("{\n" +
				"  \"userName\" : \"anil\",\n" +
				"  \"password\" : \"12345\"\n" +
				"}")).andExpect(status().isOk()).andDo(print ());
	}

	@Test
	public void validateUserWithOTPWhenUserIsNotPresent() throws Exception {

		this.mockMvc.perform(post("/validateUserWithOTP").contentType("application/json").content("{\n" +
				"  \"userId\" : \"ABC\",\n" +
				"  \"otpCode\" : \"1234\"\n" +
				"}")).andDo(print ()).andExpect(status().isOk()).andExpect(content ().string(Matchers.containsString ("User not found")));
	}

	@Test
	public void validateUserWithOTPWhenUserIsPresent() throws Exception {

		String userID = "1234";
		User newUser = new User();
		newUser.setUserId(userID);
		newUser.setOtpCode("1234");
		userRepository.save(newUser);
		this.mockMvc.perform(post("/validateUserWithOTP").contentType("application/json").content("{\n" +
				"  \"userId\" : \"" + userID + "\",\n" +
				"  \"otpCode\" : \"1234\"\n" +
				"}")).andDo(print ()).andExpect(status().isOk()).andExpect(content ().string(Matchers.containsString ("User found!!! Hurray!!")));
	}

}
