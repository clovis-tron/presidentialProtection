package com.seguridad.presidente.repository;

import com.seguridad.presidente.model.PresidentialMeeting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PresidentialMeetingRepository extends JpaRepository<PresidentialMeeting, Long> {
    Optional<PresidentialMeeting> findById(Long id);

    List<PresidentialMeeting> findByAgendaContainingIgnoreCase(String keyword);
}
