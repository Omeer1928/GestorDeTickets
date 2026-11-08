package pe.techsupport.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

import pe.techsupport.gateway.config.AccessRules;
import pe.techsupport.gateway.config.AccessRules.Decision;

class AccessRulesTest {

    private final AccessRules rules = new AccessRules();

    @Test
    void loginAndRegisterArePublic() {
        assertThat(rules.decide(HttpMethod.POST, "/auth/login")).isEqualTo(Decision.PUBLIC);
        assertThat(rules.decide(HttpMethod.POST, "/auth/register")).isEqualTo(Decision.PUBLIC);
    }

    @Test
    void internalEndpointsAreNeverExposed() {
        assertThat(rules.decide(HttpMethod.PATCH, "/users/technicians/USR-T0001/workload")).isEqualTo(Decision.DENY);
        assertThat(rules.decide(HttpMethod.DELETE, "/users/clients/USR-C0001")).isEqualTo(Decision.DENY);
    }

    @Test
    void rbacByRoleAndMethod() {
        assertThat(rules.isAllowed(HttpMethod.GET, "/users/clients", "CLIENTE")).isFalse();
        assertThat(rules.isAllowed(HttpMethod.GET, "/users/clients", "ADMIN")).isTrue();
        assertThat(rules.isAllowed(HttpMethod.GET, "/users/technicians/available", "CLIENTE")).isFalse();
        assertThat(rules.isAllowed(HttpMethod.PATCH, "/tickets/TKT_102001/assign", "TECNICO")).isFalse();
        assertThat(rules.isAllowed(HttpMethod.PATCH, "/tickets/TKT_102001/escalate", "TECNICO")).isTrue();
        assertThat(rules.isAllowed(HttpMethod.POST, "/tickets", "CLIENTE")).isTrue();
        assertThat(rules.isAllowed(HttpMethod.GET, "/tickets", "CLIENTE")).isFalse();
    }
}
