package pe.techsupport.common.resilience;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.sql.SQLNonTransientConnectionException;
import java.sql.SQLRecoverableException;
import java.sql.SQLTransientConnectionException;
import java.util.function.Predicate;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;

import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

/**
 * Distingue un fallo de infraestructura (BD caída, servicio inaccesible, timeout) de un error de negocio.
 * Solo los fallos de infraestructura abren el circuit breaker y activan la lectura desde la caché "stale".
 * Se referencia desde application.yml como record-failure-predicate de Resilience4j.
 */
public class InfraFailures implements Predicate<Throwable> {

    @Override
    public boolean test(Throwable throwable) {
        return isInfrastructureFailure(throwable);
    }

    public static boolean isInfrastructureFailure(Throwable throwable) {
        Throwable current = throwable;
        int depth = 0;
        while (current != null && depth++ < 15) {
            if (current instanceof ConcurrencyFailureException) {
                return false; // conflicto de concurrencia (optimistic lock, deadlock): no es una caída
            }
            if (current instanceof CannotCreateTransactionException
                    || current instanceof DataAccessResourceFailureException
                    || current instanceof TransientDataAccessException
                    || current instanceof CallNotPermittedException
                    || current instanceof ResourceAccessException
                    || current instanceof HttpServerErrorException
                    || current instanceof SQLTransientConnectionException
                    || current instanceof SQLNonTransientConnectionException
                    || current instanceof SQLRecoverableException
                    || current instanceof ConnectException
                    || current instanceof UnknownHostException
                    || current instanceof SocketTimeoutException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    public static String rootMessage(Throwable throwable) {
        Throwable root = throwable;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        return root.getClass().getSimpleName() + ": " + root.getMessage();
    }
}
