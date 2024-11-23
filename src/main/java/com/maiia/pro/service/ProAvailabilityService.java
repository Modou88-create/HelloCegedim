package com.maiia.pro.service;

import com.maiia.pro.entity.Appointment;
import com.maiia.pro.entity.Availability;
import com.maiia.pro.entity.TimeSlot;
import com.maiia.pro.repository.AppointmentRepository;
import com.maiia.pro.repository.AvailabilityRepository;
import com.maiia.pro.repository.TimeSlotRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProAvailabilityService {

    @Autowired
    private AvailabilityRepository availabilityRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private TimeSlotRepository timeSlotRepository;

    public List<Availability> findByPractitionerId(Integer practitionerId) {
        return availabilityRepository.findByPractitionerId(practitionerId);
    }

    public List<Availability> generateAvailabilities(Integer practitionerId) {
        // TODO : implement this
        List<TimeSlot> listOfTimeSlotPractitionerById = timeSlotRepository.findByPractitionerId(practitionerId);

        List<TimeSlot> inCommingTimeSlots = listOfTimeSlotPractitionerById
                .stream()
                .filter(timeSlot -> timeSlot.getStartDate().isAfter(LocalDateTime.now()))
                .sorted((Comparator.comparing(TimeSlot::getStartDate)))
                .collect(Collectors.toList());

        List<Appointment> appointmentList = appointmentRepository.findByPractitionerId(practitionerId);

        List<Appointment> inCommingAppointments = appointmentList.stream()
                .filter(appointment -> appointment.getStartDate().isAfter(LocalDateTime.now()))
                .sorted((o1, o2) -> o2.getStartDate().compareTo(o1.getStartDate()))
                .collect(Collectors.toList());


        return generateAvailabilities(practitionerId, inCommingTimeSlots, inCommingAppointments);
    }


    public List<Availability> generateAvailabilities(Integer practitionerId, List<TimeSlot> timeSlots, List<Appointment> appointmentList) {

        List<Availability> availabilityList = new ArrayList<>(Collections.emptyList());
        int meetingDuration = 15;

        if (appointmentList.isEmpty()) {

            for (TimeSlot timesSlot : timeSlots) {

                LocalDateTime startDateTimeSlotWithEmptyAppointment = timesSlot.getStartDate();


                int slotTime = timesSlot.getEndDate().getHour() - timesSlot.getStartDate().getHour();

                for (int m = 0; m < slotTime * 4; m++) {
                    availabilityList.add(new Availability(null, practitionerId, startDateTimeSlotWithEmptyAppointment, startDateTimeSlotWithEmptyAppointment.plusMinutes(meetingDuration)));
                    startDateTimeSlotWithEmptyAppointment = startDateTimeSlotWithEmptyAppointment.plusMinutes(15);
                }

            }
        } else {

            for (TimeSlot timeSlot : timeSlots) {
                int timeSlotDiff = timeSlot.getEndDate().getHour() - timeSlot.getStartDate().getHour();
                LocalDateTime startDateTimeSlotWithAppointment = timeSlot.getStartDate();
                for (int d = 0; d < timeSlotDiff * 4; d++) {

                    for (Appointment appointment : appointmentList) {

                        if (!timeSlot.getStartDate().equals(appointment.getStartDate())) {
                            availabilityList.add(new Availability(null, practitionerId, startDateTimeSlotWithAppointment, startDateTimeSlotWithAppointment.plusMinutes(15)));
                            startDateTimeSlotWithAppointment = startDateTimeSlotWithAppointment.plusMinutes(15);
                        }
                    }

                }
            }

        }
        return availabilityList;


    }
}
