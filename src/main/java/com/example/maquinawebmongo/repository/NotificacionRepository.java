package com.example.maquinawebmongo.repository;

import com.example.maquinawebmongo.model.Notificacion;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;

public interface NotificacionRepository extends MongoRepository<Notificacion, String> {
    
    List<Notificacion> findByUsuarioIdOrderByFechaCreacionDesc(String usuarioId);
    
    List<Notificacion> findByUsuarioIdAndLeidaFalseOrderByFechaCreacionDesc(String usuarioId);
    
    long countByUsuarioIdAndLeidaFalse(String usuarioId);
    
    @Query("{ 'registroId': ?0, 'usuarioId': ?1 }")
    List<Notificacion> findByRegistroIdAndUsuarioId(String registroId, String usuarioId);

    List<Notificacion> findByRegistroIdAndTipo(String registroId, String tipoNotificacion);
    
    @Query("{ 'registroId': ?0, 'usuarioId': ?1, 'tipo': ?2 }")
    List<Notificacion> findByRegistroIdAndUsuarioIdAndTipo(String registroId, String usuarioId, String tipo);
}