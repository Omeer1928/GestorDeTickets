package pe.techsupport.user.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.cache-ttl")
public class CacheTtlProperties {

    private Duration profile = Duration.ofMinutes(5);
    private Duration list = Duration.ofSeconds(60);
    /** Corto: la disponibilidad cambia con cada asignación. */
    private Duration available = Duration.ofSeconds(10);

    public Duration getProfile() { return profile; }
    public void setProfile(Duration profile) { this.profile = profile; }
    public Duration getList() { return list; }
    public void setList(Duration list) { this.list = list; }
    public Duration getAvailable() { return available; }
    public void setAvailable(Duration available) { this.available = available; }
}
