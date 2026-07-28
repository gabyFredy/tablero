package com.example.maquinawebmongo.repository;

import com.example.maquinawebmongo.model.Notificacion;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface NotificacionRepository extends MongoRepository<Notificacion, String> {
    
    List<Notificacion> findByUsuarioIdOrderByFechaCreacionDesc(String usuarioId);
    
    List<Notificacion> findByUsuarioIdAndLeidaFalseOrderByFechaCreacionDesc(String usuarioId);
    
    long countByUsuarioIdAndLeidaFalse(String usuarioId);
    
    List<Notificacion> findByRegistroIdAndUsuarioId(String registroId, String usuarioId);

    List<Notificacion> findByRegistroIdAndTipo(String registroId, String tipoNotificacion);
}