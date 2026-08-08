package br.com.naheroback.modules.payment.repositories;

import br.com.naheroback.modules.payment.entities.ProcessedPaymentEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedPaymentEventRepository extends JpaRepository<ProcessedPaymentEvent, String> {
}
