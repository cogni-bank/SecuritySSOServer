package com.cognibank.securityMicroservice.Service;

import com.cognibank.securityMicroservice.Model.NotificationMessage;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.core.env.Environment;


@Service
public class RabbitSenderService {

    @Autowired
    private Environment env;

    @Autowired
    private AmqpTemplate rabbitTemplate;


    public void send(NotificationMessage notificationDetails) throws Exception{


        rabbitTemplate.convertAndSend(env.getProperty("spring.rabbitmq.api.directExchangeName"), env.getProperty("spring.rabbitmq.api.routingKey.otp"),notificationDetails);
        System.out.println("Send msg = " + notificationDetails);
    }
}