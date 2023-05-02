package com.seguridad.presidente.service;

import com.seguridad.presidente.model.PresidentialMeeting;
import com.seguridad.presidente.repository.PresidentialMeetingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class PresidentialMeetingService {

    @Autowired
    PresidentialMeetingRepository presidentialMeetingRepository;

    public List<PresidentialMeeting> getAllMeetings() {
        return presidentialMeetingRepository.findAll();
    }

    public void saveMeeting(PresidentialMeeting presidentialMeeting) {
        presidentialMeetingRepository.save(presidentialMeeting);
    }


    public void deleteMeetingById(Long id) {
        presidentialMeetingRepository.deleteById(id);
    }

    public List<PresidentialMeeting> listAll()
    {

        return presidentialMeetingRepository.findAll();
    }

    public Optional<PresidentialMeeting> findMeetingById(Long id) {
        return presidentialMeetingRepository.findById(id);
    }

    public void updateMeeting(Long id, PresidentialMeeting updatedMeeting) {
        Optional<PresidentialMeeting> optionalMeeting = findMeetingById(id);
        if (optionalMeeting.isPresent()) {
            PresidentialMeeting meeting = optionalMeeting.get();
            meeting.setDate(updatedMeeting.getDate());
            meeting.setStartTime(updatedMeeting.getStartTime());
            meeting.setEndTime(updatedMeeting.getEndTime());

            saveMeeting(meeting);
        }
    }

    public List<PresidentialMeeting> searchAgenda(String keyword) {
        List<PresidentialMeeting> conferences = presidentialMeetingRepository.findByAgendaContainingIgnoreCase(keyword);
        return conferences;
    }
}