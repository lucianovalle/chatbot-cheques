package com.estudo.cheques.recebivel;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChequeRepository extends JpaRepository<Cheque, Long> {

    List<Cheque> findAllByOrderByDataVencimentoAsc();
}
