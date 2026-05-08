package com.example.hotelbooking.payment.application.port.out;

import com.example.hotelbooking.payment.domain.BookingId;
import com.example.hotelbooking.payment.domain.Payment;
import com.example.hotelbooking.payment.domain.PaymentId;
import java.util.Optional;

public interface PaymentRepository {

  Payment save(Payment payment);

  Optional<Payment> findById(PaymentId paymentId);

  Optional<Payment> findByBookingId(BookingId bookingId);
}
