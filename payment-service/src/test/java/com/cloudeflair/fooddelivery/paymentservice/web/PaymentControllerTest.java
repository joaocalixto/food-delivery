package com.cloudeflair.fooddelivery.paymentservice.web;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentControllerTest {

    private final PaymentController controller = new PaymentController(1);

    @Test
    void keepsTheExistingRequestCompatibleByDefaultingToNormal() {
        var response = controller.pay(new PaymentController.PaymentRequest(42L, 9000, null));

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo("APPROVED");
        assertThat(response.getBody().scenario()).isEqualTo(PaymentController.Scenario.NORMAL);
    }

    @Test
    void returnsAnObservableServerErrorForTheErrorScenario() {
        var response = controller.pay(new PaymentController.PaymentRequest(
                42L, 9000, PaymentController.Scenario.ERROR));

        assertThat(response.getStatusCode().value()).isEqualTo(503);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo("DECLINED");
    }

    @Test
    void identifiesTheSlowScenarioInTheResponse() {
        var response = controller.pay(new PaymentController.PaymentRequest(
                42L, 9000, PaymentController.Scenario.SLOW));

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().scenario()).isEqualTo(PaymentController.Scenario.SLOW);
    }
}
