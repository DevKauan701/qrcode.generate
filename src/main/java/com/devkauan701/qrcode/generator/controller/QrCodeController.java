package com.devkauan701.qrcode.generator.controller;

import com.devkauan701.qrcode.generator.dto.QrCodeGenerateRequest;
import com.devkauan701.qrcode.generator.dto.QrCodeGenerateResponse;
import com.devkauan701.qrcode.generator.service.QrCodeGeneratorService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/qrcode")
public class QrCodeController {

    private final QrCodeGeneratorService service;

    public QrCodeController(QrCodeGeneratorService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<QrCodeGenerateResponse> generate(@RequestBody QrCodeGenerateRequest request) {
        if (request.text() == null || request.text().isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        try {
            return ResponseEntity.ok(service.generateAndUpload(request.text()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}