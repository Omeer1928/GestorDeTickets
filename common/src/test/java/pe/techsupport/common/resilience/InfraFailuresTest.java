package pe.techsupport.common.resilience;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.ConnectException;
import java.sql.SQLTransientConnectionException;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.transaction.CannotCreateTransactionException;

import pe.techsupport.common.error.ApiException;

class InfraFailuresTest {

    @Test
    void detectsDatabaseDownWrappedInTransactionException() {
        var ex = new CannotCreateTransactionException("no tx",
                new SQLTransientConnectionException("HikariPool - Connection is not available"));
        assertThat(InfraFailures.isInfrastructureFailure(ex)).isTrue();
    }

    @Test
    void detectsConnectExceptionDeepInCauseChain() {
        var ex = new RuntimeException("wrapper", new IllegalStateException("x", new ConnectException("refused")));
        assertThat(InfraFailures.isInfrastructureFailure(ex)).isTrue();
    }

    @Test
    void businessErrorsDoNotCountAsInfrastructure() {
        assertThat(InfraFailures.isInfrastructureFailure(ApiException.notFound("x", "y"))).isFalse();
        assertThat(InfraFailures.isInfrastructureFailure(new DataIntegrityViolationException("dup"))).isFalse();
        assertThat(InfraFailures.isInfrastructureFailure(new OptimisticLockingFailureException("version"))).isFalse();
    }
}
