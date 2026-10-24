package com.estudo.cheques.recebivel;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BoletoRepository extends JpaRepository<Boleto, Long> {

    List<Boleto> findAllByOrderByDataVencimentoAsc();
}
