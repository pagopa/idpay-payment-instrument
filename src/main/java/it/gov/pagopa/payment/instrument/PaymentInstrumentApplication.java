package it.gov.pagopa.payment.instrument;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication(scanBasePackages = "it.gov.pagopa")
public class PaymentInstrumentApplication {

  public static void main(String[] args) {
    SpringApplication.run(PaymentInstrumentApplication.class, args);
  }

}
