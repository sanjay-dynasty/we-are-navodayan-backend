package com.wearenavodayan.jnvservice;

import com.wearenavodayan.jnvservice.entity.Jnv;
import com.wearenavodayan.jnvservice.repository.JnvRepository;
import com.wearenavodayan.jnvservice.service.JnvService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionAttribute;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("local")
class JnvServiceTransactionTests {

    @Autowired
    private JnvService jnvService;

    @Autowired
    private JnvRepository jnvRepository;

    private final List<String> createdSchoolCodes = new ArrayList<>();

    @AfterEach
    void cleanUp() {
        createdSchoolCodes.forEach(code ->
                jnvRepository.findBySchoolCode(code)
                        .ifPresent(jnvRepository::delete));
    }

    @Test
    void shouldCreateAndRetrieveJnvThroughService() {
        String schoolCode = createSchoolCode();
        Jnv saved = jnvService.create(createJnv(schoolCode));

        assertThat(saved.getId()).isNotNull();
        assertThat(jnvService.getById(saved.getId()).getSchoolCode())
                .isEqualTo(schoolCode);
        assertThat(jnvService.getBySchoolCode(schoolCode).getSchoolName())
                .isEqualTo("Test Jawahar Navodaya Vidyalaya");
    }

    @Test
    void shouldUpdateAndDeleteJnvThroughService() {
        String schoolCode = createSchoolCode();
        Jnv saved = jnvService.create(createJnv(schoolCode));

        Jnv changes = createJnv(schoolCode);
        changes.setSchoolName("Updated JNV");

        Jnv updated = jnvService.update(saved.getId(), changes);

        assertThat(updated.getSchoolName()).isEqualTo("Updated JNV");

        jnvService.delete(saved.getId());

        assertThatThrownBy(() -> jnvService.getById(saved.getId()))
                .isInstanceOf(jakarta.persistence.EntityNotFoundException.class);
    }

    @Test
    void shouldDeclareReadOnlyTransactionsForReadMethods()
            throws NoSuchMethodException {
        AnnotationTransactionAttributeSource source =
                new AnnotationTransactionAttributeSource();

        Method readMethod = JnvService.class.getMethod("getById", UUID.class);
        TransactionAttribute readAttribute =
                source.getTransactionAttribute(readMethod, JnvService.class);

        Method writeMethod =
                JnvService.class.getMethod("create", Jnv.class);
        TransactionAttribute writeAttribute =
                source.getTransactionAttribute(writeMethod, JnvService.class);

        assertThat(readAttribute).isNotNull();
        assertThat(readAttribute.isReadOnly()).isTrue();

        assertThat(writeAttribute).isNotNull();
        assertThat(writeAttribute.isReadOnly()).isFalse();
    }

    private String createSchoolCode() {
        String code = "T-" + UUID.randomUUID().toString().substring(0, 12);
        createdSchoolCodes.add(code);
        return code;
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