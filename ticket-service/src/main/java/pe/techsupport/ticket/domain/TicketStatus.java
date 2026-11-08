package pe.techsupport.ticket.domain;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Catálogo de estados del ticket (Fig. 17) con su máquina de transiciones permitidas.
 */
public enum TicketStatus {
    REGISTRADO, ASIGNADO, EN_PROCESO, ESCALADO, REASIGNADO, RESUELTO, CERRADO;

    private static final Map<TicketStatus, Set<TicketStatus>> TRANSITIONS = Map.of(
            REGISTRADO, EnumSet.of(ASIGNADO),
            ASIGNADO, EnumSet.of(EN_PROCESO, ESCALADO, REASIGNADO, RESUELTO),
            REASIGNADO, EnumSet.of(EN_PROCESO, ESCALADO, REASIGNADO, RESUELTO),
            EN_PROCESO, EnumSet.of(ESCALADO, REASIGNADO, RESUELTO),
            ESCALADO, EnumSet.of(REASIGNADO),
            RESUELTO, EnumSet.of(CERRADO),
            CERRADO, EnumSet.noneOf(TicketStatus.class));

    public boolean canTransitionTo(TicketStatus target) {
        return TRANSITIONS.get(this).contains(target);
    }

    /** Estados en los que el ticket espera técnico (cola). */
    public boolean isWaiting() {
        return this == REGISTRADO || this == ESCALADO;
    }

    /** Estados con un técnico trabajando el caso. */
    public boolean hasActiveTechnician() {
        return this == ASIGNADO || this == REASIGNADO || this == EN_PROCESO;
    }
}
