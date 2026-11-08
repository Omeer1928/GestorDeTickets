package pe.techsupport.common.http;

import java.net.http.HttpClient;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import pe.techsupport.common.config.CommonProperties;
import pe.techsupport.common.security.Roles;

/**
 * Construye clientes REST para llamadas servicio-a-servicio (RestClient de Spring 6, la alternativa
 * síncrona recomendada a WebClient en aplicaciones Spring MVC). Se parte del RestClient.Builder
 * autoconfigurado para heredar métricas y propagación de traceId, y se fijan timeouts cortos para no
 * bloquear hilos si el servicio destino o su BD están caídos.
 */
@Component
public class InternalRestClientFactory {

    private final RestClient.Builder builder;
    private final CommonProperties props;
    private final String appName;

    public InternalRestClientFactory(RestClient.Builder builder, CommonProperties props,
                                     @Value("${spring.application.name}") String appName) {
        this.builder = builder;
        this.props = props;
        this.appName = appName;
    }

    public RestClient create(String baseUrl, Duration connectTimeout, Duration readTimeout) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(connectTimeout)
                .version(HttpClient.Version.HTTP_1_1)
                .build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(readTimeout);
        return builder.clone()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .defaultHeader(Roles.HEADER_USER_ID, "system:" + appName)
                .defaultHeader(Roles.HEADER_USER_ROLE, Roles.SYSTEM)
                .defaultHeader(Roles.HEADER_INTERNAL_KEY, props.getInternalKey())
                .build();
    }
}
