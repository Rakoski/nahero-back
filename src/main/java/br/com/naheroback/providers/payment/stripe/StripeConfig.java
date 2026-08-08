package br.com.naheroback.providers.payment.stripe;

import com.stripe.Stripe;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "payment.stripe")
public class StripeConfig {

    private String apiKey;
    private String webhookSecret;
    private String priceMonthly;
    private String priceYearly;
    private String successUrl;
    private String cancelUrl;

    @PostConstruct
    public void init() {
        Stripe.apiKey = this.apiKey;
    }
}
