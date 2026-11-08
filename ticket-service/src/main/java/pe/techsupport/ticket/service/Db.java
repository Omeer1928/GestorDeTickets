package pe.techsupport.ticket.service;

import java.util.function.Supplier;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;

import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Ejecuta trabajo transaccional detrás del circuit breaker "db". Con la BD caída, tras unos fallos
 * el circuito se abre y las escrituras responden 503 de inmediato en vez de esperar timeouts.
 * Las transacciones son cortas y nunca envuelven llamadas HTTP a otros servicios.
 */
@Component
public class Db {

    private final TransactionTemplate tx;
    private final TransactionTemplate readOnlyTx;
    private final CircuitBreaker circuit;

    public Db(PlatformTransactionManager txManager, CircuitBreakerRegistry registry) {
        this.tx = new TransactionTemplate(txManager);
        this.readOnlyTx = new TransactionTemplate(txManager);
        this.readOnlyTx.setReadOnly(true);
        this.circuit = registry.circuitBreaker("db");
    }

    public <T> T write(Supplier<T> work) {
        return circuit.executeSupplier(() -> tx.execute(status -> work.get()));
    }

    public void writeVoid(Runnable work) {
        circuit.executeRunnable(() -> tx.executeWithoutResult(status -> work.run()));
    }

    /** Lectura transaccional SIN circuit breaker: la usa ResilientCache, que ya aplica el suyo. */
    public <T> T read(Supplier<T> work) {
        return readOnlyTx.execute(status -> work.get());
    }
}
