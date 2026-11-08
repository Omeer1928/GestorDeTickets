package pe.techsupport.auth.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.auth")
public class AuthProperties {

    private int maxFailedAttempts = 5;
    private Duration lockDuration = Duration.ofMinutes(15);
    private Duration credentialCacheTtl = Duration.ofMinutes(10);

    public int getMaxFailedAttempts() { return maxFailedAttempts; }
    public void setMaxFailedAttempts(int maxFailedAttempts) { this.maxFailedAttempts = maxFailedAttempts; }
    public Duration getLockDuration() { return lockDuration; }
    public void setLockDuration(Duration lockDuration) { this.lockDuration = lockDuration; }
    public Duration getCredentialCacheTtl() { return credentialCacheTtl; }
    public void setCredentialCacheTtl(Duration credentialCacheTtl) { this.credentialCacheTtl = credentialCacheTtl; }
}
