package com.devkauan701.qrcode.generator.service;

import com.devkauan701.qrcode.generator.dto.QrCodeGenerateResponse;
import com.devkauan701.qrcode.generator.ports.StoragePort;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.UUID;

@Service
public class QrCodeGeneratorService {

    private final StoragePort storage;

    public QrCodeGeneratorService(StoragePort storage) {
        this.storage = storage;
    }

    public QrCodeGenerateResponse generateAndUpload(String text) throws WriterException, IOException {
        BitMatrix matrix = new QRCodeWriter()
                .encode(text, BarcodeFormat.QR_CODE, 300, 300);

        var out = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(matrix, "PNG", out);

        String fileName = UUID.randomUUID() + ".png";
        String url = storage.uploadFile(out.toByteArray(), fileName, "image/png");

        return new QrCodeGenerateResponse(url);
    }
}
