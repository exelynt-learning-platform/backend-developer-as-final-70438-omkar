package com.omkar.resourcebooking.service;

import com.omkar.resourcebooking.dto.ReservationRequest;
import com.omkar.resourcebooking.dto.ReservationResponse;
import com.omkar.resourcebooking.entity.Reservation;
import com.omkar.resourcebooking.entity.ReservationStatus;
import com.omkar.resourcebooking.entity.Resource;
import com.omkar.resourcebooking.entity.Role;
import com.omkar.resourcebooking.entity.User;
import com.omkar.resourcebooking.exception.BadRequestException;
import com.omkar.resourcebooking.exception.ReservationNotFoundException;
import com.omkar.resourcebooking.exception.ResourceNotFoundException;
import com.omkar.resourcebooking.repository.ReservationRepository;
import com.omkar.resourcebooking.repository.ReservationSpecification;
import com.omkar.resourcebooking.repository.ResourceRepository;
import com.omkar.resourcebooking.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final ResourceRepository resourceRepository;
    private final UserRepository userRepository;

    public ReservationService(
            ReservationRepository reservationRepository,
            ResourceRepository resourceRepository,
            UserRepository userRepository) {

        this.reservationRepository = reservationRepository;
        this.resourceRepository = resourceRepository;
        this.userRepository = userRepository;
    }

    public ReservationResponse createReservation(ReservationRequest request) {
        User authenticatedUser = getAuthenticatedUser();

        if (request.getStartTime() == null || request.getEndTime() == null) {
            throw new BadRequestException("Start time and End time are required");
        }

        if (!request.getEndTime().isAfter(request.getStartTime())) {
            throw new BadRequestException("End time must be after start time");
        }

        Resource resource = resourceRepository.findById(request.getResourceId())
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found with id: " + request.getResourceId()));

        BigDecimal price = request.getPrice();

        Reservation reservation = new Reservation(
                authenticatedUser,
                resource,
                request.getStartTime(),
                request.getEndTime(),
                price,
                ReservationStatus.PENDING
        );

        Reservation saved = reservationRepository.save(reservation);
        return mapToResponse(saved);
    }

    public Page<ReservationResponse> getAllReservations(
            ReservationStatus status,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            int page,
            int size,
            String sortParam) {

        User currentUser = getAuthenticatedUser();
        String usernameFilter = null;

        if (currentUser.getRole() == Role.USER) {
            usernameFilter = currentUser.getUsername();
        }

        Specification<Reservation> spec = ReservationSpecification.filterReservations(
                status, minPrice, maxPrice, usernameFilter
        );

        Pageable pageable = createPageable(page, size, sortParam);
        Page<Reservation> reservationPage = reservationRepository.findAll(spec, pageable);

        return reservationPage.map(this::mapToResponse);
    }

    public ReservationResponse getReservationById(Long id) {
        User currentUser = getAuthenticatedUser();
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ReservationNotFoundException("Reservation not found with id: " + id));

        if (currentUser.getRole() == Role.USER && !reservation.getUser().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Access denied: You do not own this reservation");
        }

        return mapToResponse(reservation);
    }

    public ReservationResponse updateReservation(Long id, ReservationRequest request) {
        User currentUser = getAuthenticatedUser();
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ReservationNotFoundException("Reservation not found with id: " + id));

        if (currentUser.getRole() == Role.USER && !reservation.getUser().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Access denied: You cannot modify another user's reservation");
        }

        if (request.getStartTime() != null && request.getEndTime() != null) {
            if (!request.getEndTime().isAfter(request.getStartTime())) {
                throw new BadRequestException("End time must be after start time");
            }
            reservation.setStartTime(request.getStartTime());
            reservation.setEndTime(request.getEndTime());
        }

        if (request.getResourceId() != null) {
            Resource resource = resourceRepository.findById(request.getResourceId())
                    .orElseThrow(() -> new ResourceNotFoundException("Resource not found with id: " + request.getResourceId()));
            reservation.setResource(resource);
        }

        if (request.getPrice() != null) {
            reservation.setPrice(request.getPrice());
        }

        Reservation updated = reservationRepository.save(reservation);
        return mapToResponse(updated);
    }

    public void deleteReservation(Long id) {
        User currentUser = getAuthenticatedUser();
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ReservationNotFoundException("Reservation not found with id: " + id));

        if (currentUser.getRole() == Role.USER && !reservation.getUser().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Access denied: You cannot delete another user's reservation");
        }

        reservationRepository.delete(reservation);
    }

    private User getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("User is not authenticated");
        }
        String username = authentication.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new AccessDeniedException("Authenticated user not found in database: " + username));
    }

    private static final java.util.Set<String> ALLOWED_SORT_FIELDS = java.util.Set.of(
            "id", "price", "startTime", "endTime", "status"
    );

    private Pageable createPageable(int page, int size, String sortParam) {
        if (page < 0) page = 0;
        if (size <= 0) size = 10;

        if (sortParam == null || sortParam.trim().isEmpty()) {
            return PageRequest.of(page, size, Sort.by("id").descending());
        }

        String[] sortParts = sortParam.split(",");
        String property = sortParts[0].trim();

        if (!ALLOWED_SORT_FIELDS.contains(property)) {
            throw new BadRequestException("Invalid sort field: " + property + ". Allowed fields: " + ALLOWED_SORT_FIELDS);
        }

        Sort.Direction direction = Sort.Direction.ASC;

        if (sortParts.length > 1 && "desc".equalsIgnoreCase(sortParts[1].trim())) {
            direction = Sort.Direction.DESC;
        }

        return PageRequest.of(page, size, Sort.by(direction, property));
    }

    private ReservationResponse mapToResponse(Reservation reservation) {
        return new ReservationResponse(
                reservation.getId(),
                reservation.getUser().getId(),
                reservation.getUser().getUsername(),
                reservation.getResource().getId(),
                reservation.getResource().getName(),
                reservation.getStartTime(),
                reservation.getEndTime(),
                reservation.getPrice(),
                reservation.getStatus()
        );
    }
}
