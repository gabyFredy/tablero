package com.example.maquinawebmongo.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${SENDGRID_FROM_EMAIL:fernandogglucena@gmail.com}")
    private String fromEmail;

    @Value("${spring.mail.password:}")
    private String password;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public boolean enviarCorreo(String destino, String asunto, String mensaje) {
        try {
            System.out.println("📧 INTENTANDO ENVIAR CORREO A: " + destino);
            System.out.println("📧 FROM: " + fromEmail);

            SimpleMailMessage email = new SimpleMailMessage();
            email.setFrom(fromEmail);
            email.setTo(destino);
            email.setSubject(asunto);
            email.setText(mensaje);
            mailSender.send(email);
            System.out.println("✅ Correo enviado exitosamente a: " + destino);
            return true;
        } catch (Exception e) {
            System.err.println("❌ Error al enviar correo a " + destino + ": " + e.getMessage());
            return false;
        }
    }
}