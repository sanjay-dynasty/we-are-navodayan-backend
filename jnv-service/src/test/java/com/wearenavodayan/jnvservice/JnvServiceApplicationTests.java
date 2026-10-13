package com.wearenavodayan.jnvservice;

import com.wearenavodayan.jnvservice.entity.Jnv;
import com.wearenavodayan.jnvservice.repository.JnvRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("local")
@Transactional
class JnvServiceApplicationTests {

    @Autowired
    private JnvRepository jnvRepository;

    @Test
    void contextLoads() {
        assertThat(jnvRepository).isNotNull();
    }

    @Test
    void shouldSaveAndRetrieveJnv() {
        String schoolCode = "T-" + UUID.randomUUID().toString().substring(0, 12);

        Jnv jnv = createJnv(schoolCode);

        Jnv savedJnv = jnvRepository.saveAndFlush(jnv);

        assertThat(savedJnv.getId()).isNotNull();
        assertThat(savedJnv.getCreatedAt()).isNotNull();
        assertThat(savedJnv.getUpdatedAt()).isNotNull();

        Jnv retrievedJnv = jnvRepository.findById(savedJnv.getId())
                .orElseThrow();

        assertThat(retrievedJnv.getSchoolCode()).isEqualTo(schoolCode);
        assertThat(retrievedJnv.getSchoolName())
                .isEqualTo("Test Jawahar Navodaya Vidyalaya");
        assertThat(retrievedJnv.getState()).isEqualTo("Uttar Pradesh");
        assertThat(retrievedJnv.getDistrict()).isEqualTo("Ayodhya");
    }

    @Test
    void shouldFindJnvBySchoolCode() {
        String schoolCode = "T-" + UUID.randomUUID().toString().substring(0, 12);

        jnvRepository.saveAndFlush(createJnv(schoolCode));

        assertThat(jnvRepository.findBySchoolCode(schoolCode))
                .isPresent()
                .get()
                .extracting(Jnv::getSchoolCode)
                .isEqualTo(schoolCode);
    }

    @Test
    void shouldCheckWhetherSchoolCodeExists() {
        String schoolCode = "T-" + UUID.randomUUID().toString().substring(0, 12);

        jnvRepository.saveAndFlush(createJnv(schoolCode));

        assertThat(jnvRepository.existsBySchoolCode(schoolCode))
                .isTrue();

        assertThat(jnvRepository.existsBySchoolCode(
                "MISSING-" + UUID.randomUUID()))
                .isFalse();
    }

    @Test
    void shouldRejectDuplicateSchoolCode() {
        String schoolCode = "T-" + UUID.randomUUID().toString().substring(0, 12);

        jnvRepository.saveAndFlush(createJnv(schoolCode));

        Jnv duplicateJnv = createJnv(schoolCode);

        assertThatThrownBy(
                () -> jnvRepository.saveAndFlush(duplicateJnv))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private Jnv createJnv(String schoolCode) {
        Jnv jnv = new Jnv();

        jnv.setSchoolCode(schoolCode);
        jnv.setSchoolName("Test Jawahar Navodaya Vidyalaya");
        jnv.setState("Uttar Pradesh");
        jnv.setDistrict("Ayodhya");
        jnv.setAddress("Test Address");
        jnv.setPinCode("224001");
        jnv.setActive(true);

        return jnv;
    }
}