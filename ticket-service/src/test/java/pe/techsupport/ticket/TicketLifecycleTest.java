package pe.techsupport.ticket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import pe.techsupport.common.error.ApiException;
import pe.techsupport.ticket.domain.Priority;
import pe.techsupport.ticket.domain.QueueName;
import pe.techsupport.ticket.domain.Specialty;
import pe.techsupport.ticket.domain.SupportLevel;
import pe.techsupport.ticket.domain.Ticket;
import pe.techsupport.ticket.domain.TicketStatus;

class TicketLifecycleTest {

    private Ticket newTicket(Priority priority) {
        return Ticket.register("USR-C0001", "Manufacturas del Norte", "Caída total del ERP", "desc",
                Specialty.BASES_DATOS, priority, SupportLevel.N1);
    }

    @Test
    void criticalTicketsGoToPriorityQueue() {
        assertThat(newTicket(Priority.CRITICA).getQueueName()).isEqualTo(QueueName.PRIORITY_QUEUE);
        assertThat(newTicket(Priority.BAJA).getQueueName()).isEqualTo(QueueName.STANDARD_QUEUE);
    }

    @Test
    void fullHappyPath() {
        Ticket t = newTicket(Priority.ALTA);
        assertThat(t.assignTechnician("USR-T0001", "Luis Ramírez", SupportLevel.N1)).isEqualTo(TicketStatus.ASIGNADO);
        assertThat(t.getQueueName()).isNull();
        t.startWork();
        t.resolve("Se reinició el servicio", "REINICIO");
        t.close();
        assertThat(t.getStatus()).isEqualTo(TicketStatus.CERRADO);
    }

    @Test
    void hierarchicalEscalationRaisesLevelAndKeepsHistory() {
        Ticket t = newTicket(Priority.ALTA);
        t.assignTechnician("USR-T0003", "Ana Quispe", SupportLevel.N1);
        String released = t.escalate("Requiere privilegios de DBA", null);

        assertThat(released).isEqualTo("USR-T0003");
        assertThat(t.getStatus()).isEqualTo(TicketStatus.ESCALADO);
        assertThat(t.getRequiredLevel()).isEqualTo(SupportLevel.N2);
        assertThat(t.getAssignedTechId()).isNull();
        assertThat(t.lastEscalatedFrom()).contains("USR-T0003");

        assertThat(t.assignTechnician("USR-T0001", "Luis Ramírez", SupportLevel.N2)).isEqualTo(TicketStatus.REASIGNADO);
        assertThat(t.getEscalations()).hasSize(1);
        assertThat(t.getEscalations().get(0).getNewTechId()).isEqualTo("USR-T0001");
    }

    @Test
    void functionalEscalationChangesSpecialtyKeepingLevel() {
        Ticket t = newTicket(Priority.ALTA);
        t.assignTechnician("USR-T0001", "Luis Ramírez", SupportLevel.N2);
        t.escalate("Es un problema de red", Specialty.REDES_CONECTIVIDAD);
        assertThat(t.getCategory()).isEqualTo(Specialty.REDES_CONECTIVIDAD);
        assertThat(t.getRequiredLevel()).isEqualTo(SupportLevel.N1);
    }

    @Test
    void cannotEscalateBeyondN3() {
        Ticket t = newTicket(Priority.CRITICA);
        t.assignTechnician("USR-T0002", "Mario Torres", SupportLevel.N3);
        assertThatThrownBy(() -> t.escalate("sin solución", null))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("N3");
    }

    @Test
    void invalidTransitionsAreRejected() {
        Ticket t = newTicket(Priority.MEDIA);
        assertThatThrownBy(t::close).isInstanceOf(ApiException.class);
        assertThatThrownBy(t::startWork).isInstanceOf(ApiException.class);
    }

    @Test
    void directReassignmentRecordsHistory() {
        Ticket t = newTicket(Priority.ALTA);
        t.assignTechnician("USR-T0003", "Ana Quispe", SupportLevel.N1);
        String released = t.reassign("Cambio de turno", "USR-T0001", "Luis Ramírez", SupportLevel.N2);
        assertThat(released).isEqualTo("USR-T0003");
        assertThat(t.getStatus()).isEqualTo(TicketStatus.REASIGNADO);
        assertThat(t.getEscalations()).singleElement()
                .satisfies(e -> assertThat(e.getNewTechId()).isEqualTo("USR-T0001"));
    }
}
