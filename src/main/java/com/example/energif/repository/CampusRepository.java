package com.example.energif.repository;

import com.example.energif.model.Campus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CampusRepository extends JpaRepository<Campus, Long> {
    Campus findByNome(String nome);
}
