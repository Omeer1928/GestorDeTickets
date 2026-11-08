package pe.techsupport.user.domain;

/** Nivel jerárquico de soporte: N1 primer contacto, N2 especialista, N3 experto/infraestructura. */
public enum SupportLevel {
    N1, N2, N3;

    public boolean covers(SupportLevel required) {
        return this.ordinal() >= required.ordinal();
    }
}
