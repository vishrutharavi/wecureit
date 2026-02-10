package com.wecureit.controller.doctor;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.wecureit.dto.response.AvailabilityResponse;
import com.wecureit.repository.DoctorAvailabilityRepository;
import com.wecureit.repository.DoctorFacilityLockRepository;
import com.wecureit.service.DoctorAvailabilityService;

/**
 * Controller to expose doctor facility lock related helpers.
 *
 * New endpoint: GET /api/doctors/{doctorId}/locked-availabilities?workDate=YYYY-MM-DD
 * - If a DoctorFacilityLock exists for doctorId+workDate, this endpoint will
 *   deactivate other availabilities for that doctor/day (set is_active=false)
 *   and then return the active availabilities for the date.
 */
@RestController
@RequestMapping("/api/doctors")
public class DoctorFacilityLockController {

	@Autowired
	private DoctorFacilityLockRepository lockRepository;

	@Autowired
	private DoctorAvailabilityRepository availabilityRepository;

	@Autowired
	private DoctorAvailabilityService availabilityService;

	@GetMapping("/{doctorId}/locked-availabilities")
	public ResponseEntity<List<AvailabilityResponse>> getLockedAvailabilities(
			@PathVariable("doctorId") UUID doctorId,
			@RequestParam(value = "workDate") String workDateStr
	) {
		if (workDateStr == null || workDateStr.isBlank()) {
			return ResponseEntity.badRequest().build();
		}
		LocalDate workDate = LocalDate.parse(workDateStr);

		// If a lock exists, deactivate other availabilities for that doctor/day.
		var lockOpt = lockRepository.findByDoctorIdAndWorkDate(doctorId, workDate);
		if (lockOpt.isPresent()) {
			var lock = lockOpt.get();
			// bulk-update to mark other availabilities inactive
			availabilityRepository.deactivateOtherAvailabilities(doctorId, workDate, lock.getFacilityId());
		}

		// Return availabilities for the single date (from == to == workDate)
		List<AvailabilityResponse> out = availabilityService.listAvailabilities(doctorId, workDate, workDate);
		return ResponseEntity.ok(out);
	}

}

