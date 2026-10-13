package com.wearenavodayan.jnvservice.service;

import com.wearenavodayan.jnvservice.entity.Jnv;
import com.wearenavodayan.jnvservice.repository.JnvRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class JnvService {

    private final JnvRepository jnvRepository;

    public JnvService(JnvRepository jnvRepository) {
        this.jnvRepository = jnvRepository;
    }

    @Transactional
    public Jnv create(Jnv jnv) {
        return jnvRepository.save(jnv);
    }

    public Jnv getById(UUID id) {
        return jnvRepository.findById(id)
                .orElseThrow(() ->
                        new EntityNotFoundException("JNV not found: " + id));
    }

    public Jnv getBySchoolCode(String schoolCode) {
        return jnvRepository.findBySchoolCode(schoolCode)
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "JNV not found with school code: " + schoolCode));
    }

    public List<Jnv> getAll() {
        return jnvRepository.findAll();
    }

    @Transactional
    public Jnv update(UUID id, Jnv updatedJnv) {
        Jnv existingJnv = getById(id);

        existingJnv.setSchoolCode(updatedJnv.getSchoolCode());
        existingJnv.setSchoolName(updatedJnv.getSchoolName());
        existingJnv.setState(updatedJnv.getState());
        existingJnv.setDistrict(updatedJnv.getDistrict());
        existingJnv.setAddress(updatedJnv.getAddress());
        existingJnv.setPinCode(updatedJnv.getPinCode());
        existingJnv.setActive(updatedJnv.isActive());

        return jnvRepository.save(existingJnv);
    }

    @Transactional
    public void delete(UUID id) {
        Jnv jnv = getById(id);
        jnvRepository.delete(jnv);
    }

    public boolean existsBySchoolCode(String schoolCode) {
        return jnvRepository.existsBySchoolCode(schoolCode);
    }
}
