package com.example.energif.repository;

import com.example.energif.model.Campus;
import com.example.energif.model.CampusEdital;
import com.example.energif.model.CampusEditalTurno;
import com.example.energif.model.Edital;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CampusEditalTurnoRepository extends JpaRepository<CampusEditalTurno, Long> {

    CampusEditalTurno findByCampusEditalAndTurno(CampusEdital ce, String turno);

    @Query("select t from CampusEditalTurno t where t.campusEdital.campus = :campus and t.campusEdital.edital = :edital and t.turno = :turno")
    CampusEditalTurno findByCampusAndEditalAndTurno(@Param("campus") Campus campus, @Param("edital") Edital edital, @Param("turno") String turno);

    List<CampusEditalTurno> findByCampusEditalId(Long campusEditalId);
}
