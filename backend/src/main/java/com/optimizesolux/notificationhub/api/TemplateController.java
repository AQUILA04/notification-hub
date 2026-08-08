package com.optimizesolux.notificationhub.api;

import com.optimizesolux.notificationhub.api.dto.CreateTemplateRequest;
import com.optimizesolux.notificationhub.api.dto.PreviewTemplateRequest;
import com.optimizesolux.notificationhub.api.dto.TemplateResponse;
import com.optimizesolux.notificationhub.application.TemplateService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1/templates")
public class TemplateController {

    private final TemplateService templateService;

    public TemplateController(TemplateService templateService) {
        this.templateService = templateService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TemplateResponse create(@Valid @RequestBody CreateTemplateRequest request) {
        return templateService.create(request);
    }

    @GetMapping
    public List<TemplateResponse> list() {
        return templateService.list();
    }

    @PostMapping("/{name}/preview")
    public Map<String, String> preview(
            @PathVariable String name, @Valid @RequestBody PreviewTemplateRequest request) {
        return templateService.preview(name, request.data());
    }
}
