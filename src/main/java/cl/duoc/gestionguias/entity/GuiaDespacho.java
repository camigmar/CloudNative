package cl.duoc.gestionguias.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "guias")
public class GuiaDespacho {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String numeroGuia;
    private String transportista;
    private String fecha;
    private String rutaEfs;
    private String urlS3;
    private String estado;
}
