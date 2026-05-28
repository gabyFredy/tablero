package com.example.maquinawebmongo;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

@Component
public class MongoDBCheck {
    
    private final MongoTemplate mongoTemplate;
    
    public MongoDBCheck(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }
    
    @EventListener(ApplicationReadyEvent.class)
    public void verificarConexion() {
        try {
            mongoTemplate.executeCommand("{ buildInfo: 1 }");
            System.out.println("✅ ¡CONECTADO EXITOSAMENTE a MongoDB!");
            System.out.println("📊 Base de datos: " + mongoTemplate.getDb().getName());
        } catch (Exception e) {
            System.err.println("❌ ERROR: No se pudo conectar a MongoDB");
            System.err.println("🔧 Motivo: " + e.getMessage());
        }
    }
}
