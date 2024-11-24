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

        List<TimeSlot> inComingTimeSlots = listOfTimeSlotPractitionerById
                .stream()
                .filter(timeSlot -> timeSlot.getStartDate().isAfter(LocalDateTime.now()))
                .sorted((Comparator.comparing(TimeSlot::getStartDate)))
                .collect(Collectors.toList());

        List<Appointment> appointmentList = appointmentRepository.findByPractitionerId(practitionerId);

        List<Appointment> inComingAppointments = appointmentList
                .stream()
                .filter(appointment -> appointment.getStartDate().isAfter(LocalDateTime.now()))
                .sorted(Comparator.comparing(Appointment::getStartDate).reversed())
                .collect(Collectors.toList());

        List<LocalDateTime> appointmentDates = inComingAppointments.stream().map(Appointment::getStartDate).collect(Collectors.toList());

        return generateAvailabilities(practitionerId, inComingTimeSlots, inComingAppointments, appointmentDates);
    }


    public List<Availability> generateAvailabilities(Integer practitionerId, List<TimeSlot> timeSlots, List<Appointment> appointmentList, List<LocalDateTime> appointmentsDates) {

        List<Availability> availabilityList = new ArrayList<>(Collections.emptyList());
        int meetingDuration = 15;

        if (!appointmentList.isEmpty()) {

            for (TimeSlot timeSlot : timeSlots) {
                int timeSlotDiff = timeSlot.getEndDate().getHour() - timeSlot.getStartDate().getHour();
                LocalDateTime startDateTimeSlotWithAppointment = timeSlot.getStartDate();
                for (int m = 0; m < timeSlotDiff * 4; m++) {
                    for (Appointment appointment : appointmentList) {

                        if (startDateTimeSlotWithAppointment.isBefore(appointment.getStartDate())) {

                            if (!startDateTimeSlotWithAppointment.equals(appointment.getStartDate())) {
                                List<LocalDateTime> collected = appointmentsDates.stream().filter(localDateTime -> localDateTime.equals(appointment.getStartDate())).collect(Collectors.toList());
                                if (!collected.isEmpty() && !appointment.getStartDate().equals(startDateTimeSlotWithAppointment.plusMinutes(meetingDuration))) {
                                    Availability.AvailabilityBuilder availability = Availability.builder().practitionerId(practitionerId).startDate(appointment.getEndDate()).endDate(appointment.getEndDate().plusMinutes(15));
                                    availabilityList.add(availability.build());
                                    startDateTimeSlotWithAppointment = appointment.getStartDate().plusMinutes(15);
                                }

                                List<LocalDateTime> firstDatePossible = appointmentsDates.stream().filter(localDateTime -> !localDateTime.equals(timeSlot.getStartDate())).collect(Collectors.toList());
                                if (firstDatePossible.isEmpty()) {
                                    Availability.AvailabilityBuilder availability = Availability.builder().practitionerId(practitionerId).startDate(timeSlot.getStartDate()).endDate(timeSlot.getEndDate().plusMinutes(15));
                                    availabilityList.add(availability.build());
                                }


                            }


                        }


                    }
                }
            }

        } else {
            for (TimeSlot timesSlot : timeSlots) {

                LocalDateTime startDateTimeSlotWithEmptyAppointment = timesSlot.getStartDate();


                int slotTime = timesSlot.getEndDate().getHour() - timesSlot.getStartDate().getHour();

                for (int m = 0; m < slotTime * 4; m++) {
                    Availability.AvailabilityBuilder availability = Availability.builder().practitionerId(practitionerId).startDate(startDateTimeSlotWithEmptyAppointment).endDate(startDateTimeSlotWithEmptyAppointment.plusMinutes(15));
                    availabilityList.add(availability.build());
                    startDateTimeSlotWithEmptyAppointment = startDateTimeSlotWithEmptyAppointment.plusMinutes(15);
                }

            }
        }


        return availabilityList;


    }
}
