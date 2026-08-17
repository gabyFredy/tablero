package com.example.maquinawebmongo.repository;

import com.example.maquinawebmongo.model.Main;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;

public interface MainRepository extends MongoRepository<Main, String> {
    
    // 🔥 NUEVO: Buscar todos los elementos de una sección específica
    List<Main> findBySeccion(String seccion);

    //List<Main> findBySeccionStartingWith(String prefijo);
    
    // Opcional: Buscar por sección y título
    List<Main> findBySeccionAndTitulo(String seccion, String titulo);
    
    // Opcional: Buscar por sección y programa
    List<Main> findBySeccionAndPrograma(String seccion, String programa);
    
    // Opcional: Buscar por porcentaje mayor a...
    List<Main> findBySeccionAndPorcentajeGreaterThan(String seccion, int porcentaje);

    // 🔥 NUEVO: Buscar por sección y orden
    List<Main> findBySeccionOrderByOrdenAsc(String seccion);

    // NUEVO: Buscar el máximo orden de una sección
    @Query(value = "{ 'seccion': ?0 }", fields = "{ 'orden': 1 }", sort = "{ 'orden': -1 }")
    List<Main> findTopBySeccionOrderByOrdenDesc(String seccion, Pageable pageable);
}