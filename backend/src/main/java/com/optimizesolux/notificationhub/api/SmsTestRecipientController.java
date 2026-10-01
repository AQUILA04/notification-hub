package com.optimizesolux.notificationhub.api;

import com.optimizesolux.notificationhub.api.dto.SmsTestRecipientRequest;
import com.optimizesolux.notificationhub.api.dto.SmsTestRecipientResponse;
import com.optimizesolux.notificationhub.application.SmsTestRecipientService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/admin/sms-test-recipients")
public class SmsTestRecipientController {

    private final SmsTestRecipientService smsTestRecipientService;

    public SmsTestRecipientController(SmsTestRecipientService smsTestRecipientService) {
        this.smsTestRecipientService = smsTestRecipientService;
    }

    @GetMapping
    public List<SmsTestRecipientResponse> list() {
        return smsTestRecipientService.list();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SmsTestRecipientResponse add(@Valid @RequestBody SmsTestRecipientRequest request) {
        return smsTestRecipientService.add(request.email());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@PathVariable UUID id) {
        smsTestRecipientService.remove(id);
    }
}
