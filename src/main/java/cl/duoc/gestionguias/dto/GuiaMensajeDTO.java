package cl.duoc.gestionguias.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Representa el mensaje que viaja por RabbitMQ cuando se crea o actualiza
 * una guia de despacho. Se serializa/deserializa automaticamente a JSON
 * gracias al Jackson2JsonMessageConverter configurado en RabbitMQConfig.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GuiaMensajeDTO {

    private Long guiaId;
    private String numeroGuia;
    private String transportista;
    private String fecha;
    private String estado;
    private String urlS3;
    private String motivoError; // solo se llena si el mensaje termina en la cola de errores

    public GuiaMensajeDTO(Long guiaId, String numeroGuia, String transportista,
                           String fecha, String estado, String urlS3) {
        this.guiaId = guiaId;
        this.numeroGuia = numeroGuia;
        this.transportista = transportista;
        this.fecha = fecha;
        this.estado = estado;
        this.urlS3 = urlS3;
    }
}
