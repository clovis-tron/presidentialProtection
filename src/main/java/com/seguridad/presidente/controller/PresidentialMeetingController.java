package com.seguridad.presidente.controller;

import com.seguridad.presidente.model.PresidentialMeeting;
import com.seguridad.presidente.model.User;
import com.seguridad.presidente.repository.PresidentialMeetingRepository;
import com.seguridad.presidente.service.*;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Controller
public class PresidentialMeetingController {

    @Autowired
    PresidentialMeetingService presidentialMeetingService;

    @Autowired
    private JavaMailSender emailSender;


    @Autowired
    PresidentialMeetingRepository presidentialMeetingRepository;

    @Autowired
    private UserService userService;





//    @RequestMapping(value = {"/view"}, method = RequestMethod.GET)
//    public String showMeetingList(Model model) {
//        List<PresidentialMeeting> meeting = presidentialMeetingService.listAll();
//        model.addAttribute("meeting", meeting);
//        System.out.println("records: " + meeting);
//        return "/view-meeting";
//    }

    
    @GetMapping("/view")
    public String showMeetingList(Model model) {
    	 List<PresidentialMeeting> meeting = presidentialMeetingService.listAll();
        model.addAttribute("meeting", meeting);
        return "view-meeting";
    }
 

    @GetMapping("/meetings")
    public String showMeetings(Model model) {
        List<PresidentialMeeting> meetings = presidentialMeetingRepository.findAll();
        model.addAttribute("meetings", meetings);
        return "admin/meeting-request";
    }

    @PostMapping("/sendEmail")
    public String sendEmail(@RequestParam String email, @RequestParam String message) {
        SimpleMailMessage mailMessage = new SimpleMailMessage();
        mailMessage.setTo(email);
        mailMessage.setSubject("Meeting Request");
        mailMessage.setText(message);
        emailSender.send(mailMessage);
        return "admin/dashboard";
    }

    @GetMapping("/showNewMeetingForm")
    public String showNewMeetingForm(Model model) {
        PresidentialMeeting presidentialMeeting = new PresidentialMeeting();
        model.addAttribute("presidentialMeeting", presidentialMeeting);
        return "user/schedule-form";
    }

    @PostMapping("/saveMeeting")
    public String saveMeeting(@ModelAttribute("presidentialMeeting") @Valid PresidentialMeeting presidentialMeeting,
                              BindingResult bindingResult,
                              @RequestParam("pdfFile") MultipartFile pdfFile,
                              @RequestParam(value = "file", required = false) MultipartFile profileImage) throws IOException {
        if (bindingResult.hasErrors()) {
            return "user/schedule-form";
        }
        if (!profileImage.isEmpty()) {
            String contentType = profileImage.getContentType();
            if (contentType.equals("image/jpeg") || contentType.equals("image/png")) {
                byte[] imageBytes = profileImage.getBytes();
                presidentialMeeting.setProfileImage(imageBytes);
            } else {
                bindingResult.rejectValue("profileImage", "error.profileImage", "Invalid file type");
                return "user/schedule-form";
            }
        }

        if (!pdfFile.isEmpty()) {
            String contentType = pdfFile.getContentType();
            if (contentType.equals("application/pdf")) {
                if (pdfFile.getSize() <= 1_000_000) {
                    byte[] pdfBytes = pdfFile.getBytes();
                    presidentialMeeting.setFileName(pdfFile.getOriginalFilename());
                    presidentialMeeting.setPdf(pdfBytes);
                } else {
                    bindingResult.rejectValue("pdfFile", "error.pdfFile", "File size should be less than or equal to 1MB");
                    return "user/schedule-form";
                }
            } else {
                bindingResult.rejectValue("pdfFile", "error.pdfFile", "Invalid file type");
                return "user/schedule-form";
            }
        }




        presidentialMeetingService.saveMeeting(presidentialMeeting);

        // send an email to the admin with the meeting details
        String recipientAddress = "gmail.com";
        String subject = "New meeting request";
        String message = String.format("Date: %s\nStart time: %s %s\nEnd time: %s %s\nAgenda: %s\nName: %s",
                presidentialMeeting.getDate(),
                presidentialMeeting.getStartTime(), presidentialMeeting.getStartTime().getHour() >= 12 ? "pm" : "am",
                presidentialMeeting.getEndTime(), presidentialMeeting.getEndTime().getHour() >= 12 ? "pm" : "am",
                presidentialMeeting.getAgenda(), presidentialMeeting.getReporterName());

        SimpleMailMessage email = new SimpleMailMessage();
        email.setTo(recipientAddress);
        email.setSubject(subject);
        email.setText(message);

        try {
            emailSender.send(email);
            System.out.println("Message sent");
        } catch (MailException e) {
            System.out.println("Error sending message: " + e.getMessage());
        }

        return "redirect:/view";
    }


    @GetMapping("/download/{id}")
    public ResponseEntity<byte[]> downloadPDF(@PathVariable Long id) {
        Optional<PresidentialMeeting> optionalMeeting = presidentialMeetingService.findMeetingById(id);
        if (optionalMeeting.isPresent()) {
            PresidentialMeeting meeting = optionalMeeting.get();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", meeting.getFileName());
            return new ResponseEntity<>(meeting.getPdf(), headers, HttpStatus.OK);
        }
        return ResponseEntity.notFound().build();
    }



    @RequestMapping(value = "/image/{id}", produces = MediaType.IMAGE_JPEG_VALUE)
    @ResponseBody
    public ResponseEntity<byte[]> getImage(@PathVariable("id") Long id) {
        Optional<PresidentialMeeting> meeting = presidentialMeetingService.findMeetingById(id);
        byte[] imageBytes = meeting.get().getProfileImage();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_JPEG);
        headers.setContentLength(imageBytes.length);
        return new ResponseEntity<>(imageBytes, headers, HttpStatus.OK);
    }


    @GetMapping("/updateMeeting/{id}")
    public String showUpdateForm(@PathVariable(value = "id") Long id, Model model) {
        Optional<PresidentialMeeting> optionalMeeting = presidentialMeetingService.findMeetingById(id);
        if (optionalMeeting.isPresent()) {
            PresidentialMeeting meeting = optionalMeeting.get();
            model.addAttribute("meeting", meeting);
            return "update_meeting";
        } else {
            return "redirect:/view";
        }
    }


    @PostMapping("/updateMeeting/{id}")
    public String updateMeeting(@PathVariable(value = "id") Long id, @ModelAttribute("meeting") @Valid PresidentialMeeting updatedMeeting, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return "update_meeting";
        } else {
            Optional<PresidentialMeeting> optionalMeeting = presidentialMeetingService.findMeetingById(id);
            if (optionalMeeting.isPresent()) {
                PresidentialMeeting meeting = optionalMeeting.get();
                meeting.setReporterName(updatedMeeting.getReporterName());
                meeting.setDate(updatedMeeting.getDate());
                meeting.setStartTime(updatedMeeting.getStartTime());
                meeting.setEndTime(updatedMeeting.getEndTime());
                meeting.setAgenda(updatedMeeting.getAgenda());

                presidentialMeetingService.saveMeeting(meeting);
            }



            return "redirect:/view";
        }
    }



    @GetMapping("/deleteMeeting/{id}")
    public String deleteMeeting(@PathVariable(value = "id") Long id) {
        presidentialMeetingService.deleteMeetingById(id);
        return "redirect:/view";
    }


    @GetMapping("/search")
    public String searchMeetings(@RequestParam("searchTerm") String searchTerm, Model model) {
        List<PresidentialMeeting> meetings = presidentialMeetingService.searchAgenda(searchTerm);
        model.addAttribute("meetings", meetings);
        System.out.println("I am inside the search controller");
        return "user/searched";
    }



}
