package pe.techsupport.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "gateway")
public record GatewayProperties(Services services, RateLimit rateLimit) {

    public record Services(String authUrl, String userUrl, String ticketUrl) {
    }

    public record RateLimit(int replenishRate, int burstCapacity) {
    }
}
