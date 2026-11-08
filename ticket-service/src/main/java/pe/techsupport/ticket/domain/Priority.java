package pe.techsupport.ticket.domain;

public enum Priority {
    BAJA(1), MEDIA(2), ALTA(3), CRITICA(4);

    private final int rank;

    Priority(int rank) {
        this.rank = rank;
    }

    public int rank() {
        return rank;
    }

    /** Cola de espera: CRITICA y ALTA van a la cola prioritaria. */
    public QueueName queue() {
        return rank >= ALTA.rank ? QueueName.PRIORITY_QUEUE : QueueName.STANDARD_QUEUE;
    }
}
