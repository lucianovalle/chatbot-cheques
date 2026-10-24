package com.estudo.cheques.recebivel;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaxaRepository extends JpaRepository<Taxa, Long> {

    Optional<Taxa> findByModalidade(String modalidade);
}
