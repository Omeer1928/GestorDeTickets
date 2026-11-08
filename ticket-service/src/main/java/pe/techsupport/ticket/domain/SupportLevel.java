package pe.techsupport.ticket.domain;

public enum SupportLevel {
    N1, N2, N3;

    public boolean covers(SupportLevel required) {
        return ordinal() >= required.ordinal();
    }

    /** Siguiente nivel jerárquico (N1→N2→N3) o vacío si ya es N3. */
    public java.util.Optional<SupportLevel> next() {
        return this == N3 ? java.util.Optional.empty() : java.util.Optional.of(values()[ordinal() + 1]);
    }

    public static SupportLevel max(SupportLevel a, SupportLevel b) {
        if (a == null) return b;
        if (b == null) return a;
        return a.ordinal() >= b.ordinal() ? a : b;
    }
}
