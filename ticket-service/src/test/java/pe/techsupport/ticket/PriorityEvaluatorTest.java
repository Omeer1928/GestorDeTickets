package pe.techsupport.ticket;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import pe.techsupport.ticket.domain.Priority;
import pe.techsupport.ticket.domain.Specialty;
import pe.techsupport.ticket.domain.SupportLevel;
import pe.techsupport.ticket.service.PriorityEvaluator;

class PriorityEvaluatorTest {

    private final PriorityEvaluator evaluator = new PriorityEvaluator();

    @ParameterizedTest(name = "{0} + \"{1}\" → {2}")
    @CsvSource({
            "BASES_DATOS, Caída total del ERP, CRITICA",
            "BASES_DATOS, Consulta lenta en reportes, ALTA",
            "SEGURIDAD_INFORMATICA, Posible ACCESO NO AUTORIZADO al servidor, CRITICA",
            "SEGURIDAD_INFORMATICA, Renovar certificado SSL, ALTA",
            "REDES_CONECTIVIDAD, Oficina sin internet desde las 8am, ALTA",
            "REDES_CONECTIVIDAD, Configurar nueva VLAN, MEDIA",
            "SISTEMAS_SOFTWARE, El sistema contable no responde, ALTA",
            "SISTEMAS_SOFTWARE, Instalar Office, MEDIA",
            "HARDWARE_MANTENIMIENTO, La PC no enciende, MEDIA",
            "HARDWARE_MANTENIMIENTO, Impresora no imprime, BAJA"
    })
    void appliesCriticalityTable(Specialty category, String title, Priority expected) {
        assertThat(evaluator.evaluate(category, title, "")).isEqualTo(expected);
    }

    @Test
    void keywordsAreMatchedInDescriptionToo() {
        assertThat(evaluator.evaluate(Specialty.BASES_DATOS, "ERP", "El sistema no responde en producción"))
                .isEqualTo(Priority.CRITICA);
    }

    @Test
    void criticalIncidentsRequireAtLeastN2() {
        assertThat(evaluator.requiredLevel(Priority.CRITICA)).isEqualTo(SupportLevel.N2);
        assertThat(evaluator.requiredLevel(Priority.ALTA)).isEqualTo(SupportLevel.N1);
    }
}
