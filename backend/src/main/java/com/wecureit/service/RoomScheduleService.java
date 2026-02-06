package com.wecureit.service;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.wecureit.entity.RoomSchedule;
import com.wecureit.repository.RoomScheduleRepository;

@Service
public class RoomScheduleService {

    private final RoomScheduleRepository roomScheduleRepository;

    public RoomScheduleService(RoomScheduleRepository roomScheduleRepository) {
        this.roomScheduleRepository = roomScheduleRepository;
    }

    /**
     * Try to save the room schedule in its own transaction. If the save fails due to
     * a constraint (overlap), the exception/rollback will be isolated to this method
     * and will not mark the caller's transaction rollback-only.
     *
     * Returns true if saved, false if save failed due to DataIntegrityViolationException.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public RoomSchedule tryReserve(RoomSchedule rs) {
        try {
            return roomScheduleRepository.save(rs);
        } catch (DataIntegrityViolationException ex) {
            // conflict (overlap) or other integrity issue - swallow and return null to caller
            return null;
        }
    }
}
