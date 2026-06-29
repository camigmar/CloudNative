package cl.duoc.gestionguias.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import cl.duoc.gestionguias.entity.GuiaDespacho;
import java.util.List;

public interface GuiaRepository extends JpaRepository<GuiaDespacho, Long> {

    List<GuiaDespacho> findByTransportista(String transportista);
    List<GuiaDespacho> findByFecha(String fecha);
    List<GuiaDespacho> findByTransportistaAndFecha(String transportista, String fecha);
}
