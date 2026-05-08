package com.example.hotelbooking.payment.adapter.out.percistance.jpa;

import com.example.hotelbooking.payment.application.port.out.PaymentRepository;
import com.example.hotelbooking.payment.domain.BookingId;
import com.example.hotelbooking.payment.domain.Payment;
import com.example.hotelbooking.payment.domain.PaymentId;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile("payment-postgres")
@RequiredArgsConstructor
class JpaPaymentRepository implements PaymentRepository {

  private final SpringDataPaymentJpaRepository springDataRepository;

  @Override
  public Payment save(Payment payment) {
    return springDataRepository.save(PaymentJpaEntity.from(payment)).toDomain();
  }

  @Override
  public Optional<Payment> findById(PaymentId paymentId) {
    return springDataRepository.findById(paymentId.value()).map(PaymentJpaEntity::toDomain);
  }

  @Override
  public Optional<Payment> findByBookingId(BookingId bookingId) {
    return springDataRepository.findByBookingId(bookingId.value()).map(PaymentJpaEntity::toDomain);
  }
}
