package com.omkar.resourcebooking.repository;

import com.omkar.resourcebooking.entity.Reservation;
import com.omkar.resourcebooking.entity.ReservationStatus;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;

public class ReservationSpecification {

    public static Specification<Reservation> hasStatus(ReservationStatus status) {
        return (root, query, criteriaBuilder) ->
                status == null ? null : criteriaBuilder.equal(root.get("status"), status);
    }

    public static Specification<Reservation> hasMinPrice(BigDecimal minPrice) {
        return (root, query, criteriaBuilder) ->
                minPrice == null ? null : criteriaBuilder.greaterThanOrEqualTo(root.get("price"), minPrice);
    }

    public static Specification<Reservation> hasMaxPrice(BigDecimal maxPrice) {
        return (root, query, criteriaBuilder) ->
                maxPrice == null ? null : criteriaBuilder.lessThanOrEqualTo(root.get("price"), maxPrice);
    }

    public static Specification<Reservation> hasUsername(String username) {
        return (root, query, criteriaBuilder) ->
                username == null ? null : criteriaBuilder.equal(root.get("user").get("username"), username);
    }

    public static Specification<Reservation> filterReservations(
            ReservationStatus status,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            String username) {

        return Specification
                .where(hasStatus(status))
                .and(hasMinPrice(minPrice))
                .and(hasMaxPrice(maxPrice))
                .and(hasUsername(username));
    }
}
