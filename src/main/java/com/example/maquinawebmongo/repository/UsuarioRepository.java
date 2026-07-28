package com.example.maquinawebmongo.repository;

import com.example.maquinawebmongo.model.Usuario;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends MongoRepository<Usuario, String> {
    Optional<Usuario> findByUsername(String username);
    Optional<Usuario> findByEmail(String email);
    Optional<Usuario> findByTokenVerificacion(String token);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    long countByRol(String rol);
}