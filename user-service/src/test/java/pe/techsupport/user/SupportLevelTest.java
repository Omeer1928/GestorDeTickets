package pe.techsupport.user;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import pe.techsupport.user.domain.SupportLevel;

class SupportLevelTest {

    @Test
    void higherLevelsCoverLowerRequirements() {
        assertThat(SupportLevel.N3.covers(SupportLevel.N2)).isTrue();
        assertThat(SupportLevel.N2.covers(SupportLevel.N2)).isTrue();
        assertThat(SupportLevel.N1.covers(SupportLevel.N2)).isFalse();
    }
}
