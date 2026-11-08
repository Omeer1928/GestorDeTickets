package pe.techsupport.ticket.service;

import pe.techsupport.common.error.ApiException;
import pe.techsupport.common.security.AuthUser;
import pe.techsupport.common.security.SecurityUtils;
import pe.techsupport.ticket.web.dto.TicketDtos.TicketResponse;

/**
 * Restricciones por propiedad del recurso (sección 11): un CLIENTE solo ve sus tickets y un TECNICO
 * solo los que tiene asignados (o que atendió antes de un escalamiento).
 */
final class TicketAccess {

    private TicketAccess() {
    }

    static void requireCanView(TicketResponse t) {
        AuthUser u = SecurityUtils.currentUser();
        if (u.isAdmin() || u.isSystem()) {
            return;
        }
        if (u.isClient() && u.userId().equals(t.userId())) {
            return;
        }
        if (u.isTechnician()) {
            boolean current = t.assignedTechnician() != null && u.userId().equals(t.assignedTechnician().userId());
            boolean previous = t.escalationHistory().stream().anyMatch(e -> u.userId().equals(e.previousTechId()));
            if (current || previous) {
                return;
            }
        }
        throw ApiException.forbidden("No tiene acceso al ticket " + t.ticketId());
    }

    static void requireSelf(String ownerId) {
        SecurityUtils.requireSelfOrPrivileged(ownerId);
    }
}
