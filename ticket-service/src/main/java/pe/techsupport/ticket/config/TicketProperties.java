package pe.techsupport.ticket.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.tickets")
public class TicketProperties {

    private long dispatchIntervalMs = 20000;
    private int avgAttentionMinutes = 15;
    private Duration viewCacheTtl = Duration.ofSeconds(30);
    private Duration listCacheTtl = Duration.ofSeconds(15);

    public long getDispatchIntervalMs() { return dispatchIntervalMs; }
    public void setDispatchIntervalMs(long dispatchIntervalMs) { this.dispatchIntervalMs = dispatchIntervalMs; }
    public int getAvgAttentionMinutes() { return avgAttentionMinutes; }
    public void setAvgAttentionMinutes(int avgAttentionMinutes) { this.avgAttentionMinutes = avgAttentionMinutes; }
    public Duration getViewCacheTtl() { return viewCacheTtl; }
    public void setViewCacheTtl(Duration viewCacheTtl) { this.viewCacheTtl = viewCacheTtl; }
    public Duration getListCacheTtl() { return listCacheTtl; }
    public void setListCacheTtl(Duration listCacheTtl) { this.listCacheTtl = listCacheTtl; }
}
