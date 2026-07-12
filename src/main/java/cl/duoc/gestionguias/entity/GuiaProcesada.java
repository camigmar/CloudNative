package cl.duoc.gestionguias.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * Tabla nueva (distinta a "guias") donde se guardan los mensajes que ya
 * fueron consumidos desde la Cola 1 de RabbitMQ.
 */
@Data
@Entity
@Table(name = "guias_procesadas")
public class GuiaProcesada {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long guiaOrigenId;
    private String numeroGuia;
    private String transportista;
    private String fecha;
    private String estado;
    private String urlS3;
    private LocalDateTime fechaProcesado;
}
