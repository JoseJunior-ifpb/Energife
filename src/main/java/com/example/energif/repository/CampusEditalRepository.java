package com.example.energif.repository;

import com.example.energif.model.CampusEdital;
import com.example.energif.model.Campus;
import com.example.energif.model.Edital;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CampusEditalRepository extends JpaRepository<CampusEdital, Long> {
    CampusEdital findByCampusAndEdital(Campus campus, Edital edital);
    java.util.List<CampusEdital> findAllByEditalId(Long editalId);
    java.util.List<CampusEdital> findAllByCampusId(Long campusId);
    java.util.List<CampusEdital> findAllByEditalIdAndCampusId(Long editalId, Long campusId);
}
