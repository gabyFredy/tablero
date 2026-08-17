package com.example.maquinawebmongo.service;

import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class SendGridEmailService {

    private final SendGrid sendGrid;
    private final String fromEmail;

    public SendGridEmailService(@Value("${SENDGRID_API_KEY}") String apiKey,
                                @Value("${SENDGRID_FROM_EMAIL:fernandogglucena@gmail.com}") String fromEmail) {
        this.sendGrid = new SendGrid(apiKey);
        this.fromEmail = fromEmail;
    }

    public boolean enviarCorreo(String para, String asunto, String mensaje) {
        try {
            Email from = new Email(fromEmail);
            Email to = new Email(para);
            Content content = new Content("text/plain", mensaje);
            Mail mail = new Mail(from, asunto, to, content);

            Request request = new Request();
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());

            Response response = sendGrid.api(request);
            
            if (response.getStatusCode() >= 200 && response.getStatusCode() < 300) {
                System.out.println("✅ Correo enviado a: " + para + " - Status: " + response.getStatusCode());
                return true;
            } else {
                System.err.println("❌ Error al enviar: " + response.getStatusCode() + " - " + response.getBody());
                return false;
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error al enviar correo: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}