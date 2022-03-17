package com.pai.customer;

import com.pai.amqp.RabbitMQMessageProducer;
import com.pai.clients.fraud.FraudCheckResponse;
import com.pai.clients.fraud.FraudClient;
import com.pai.clients.notification.NotificationClient;
import com.pai.clients.notification.NotificationRequest;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@AllArgsConstructor
public class CustomerService {
    private final CustomerRepository customerRepository;
    private final FraudClient fraudClient;
    private final RabbitMQMessageProducer rabbitMQMessageProducer;
    public void registerCustomer(CustomerRegistrationRequest request) {
        Customer customer = Customer.builder().firstName(request.firstName())
                .lastName(request.lastName())
                .email(request.email())
                .build();
        customerRepository.saveAndFlush(customer);
        // todo: check if valid email
        // todo: check if email is not taken
        FraudCheckResponse fraudCheckResponse = fraudClient.isFraudster(customer.getId());
        assert fraudCheckResponse != null;
        if (fraudCheckResponse.isFraudster()){
            throw new IllegalStateException("fraudster");
        }
        NotificationRequest notificationRequest = new NotificationRequest(
                customer.getId(), customer.getEmail(),
                String.format("Hi %s, welcome to paiservices...", customer.getFirstName()));

        rabbitMQMessageProducer.publish(
                notificationRequest,"internal.exchange","internal.notification.routing-key"
        );
    }
}
