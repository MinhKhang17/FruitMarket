package com.example.fruitmarket.util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.qrcode.QRCodeWriter;

import java.io.ByteArrayOutputStream;
import java.util.Base64;

public class QrUtils {
    public static String generateQrBase64(String url) throws WriterException, java.io.IOException {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        var bitMatrix = qrCodeWriter.encode(url, BarcodeFormat.QR_CODE, 300, 300);
        ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);
        byte[] pngData = pngOutputStream.toByteArray();
        return "data:image/png;base64," + Base64.getEncoder().encodeToString(pngData);
    }
}
