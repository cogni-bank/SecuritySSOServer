package com.cognibank.securityMicroservice;

import com.cognibank.securityMicroservice.Controller.MainController;
import com.cognibank.securityMicroservice.Model.UserDetails;
import static org.assertj.core.api.Assertions.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest()
public class IntegrationTests {

    @Autowired
    TestRestTemplate testRestTemplate;

    @Test
    public void loginUserTest(){
        ResponseEntity<UserDetails> userDetails = testRestTemplate.getForEntity("/security/loginUser",UserDetails.class);
        assertThat(userDetails.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

}
