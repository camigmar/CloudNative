package cl.duoc.gestionguias.producer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import cl.duoc.gestionguias.dto.GuiaMensajeDTO;

/**
 * Productor para las colas de RabbitMQ.
 *
 * Regla de negocio: toda guia generada/actualizada se intenta enviar a la
 * Cola 1. Si la publicacion falla (ej. RabbitMQ caido, exchange no
 * disponible, error de serializacion, etc.), el mismo mensaje se reenvia
 * a la Cola 2, que concentra todos los mensajes con error.
 */
@Component
public class GuiaProducer {

    private static final Logger log = LoggerFactory.getLogger(GuiaProducer.class);

    private final RabbitTemplate rabbitTemplate;

    @Value("${app.rabbitmq.exchange}")
    private String exchangeName;

    @Value("${app.rabbitmq.routingkey1}")
    private String routingKey1;

    @Value("${app.rabbitmq.routingkey2}")
    private String routingKey2;

    public GuiaProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void enviarGuia(GuiaMensajeDTO mensaje) {
        try {
            // Mecanismo de prueba: si el numeroGuia empieza con "FORZAR_ERROR",
            // se simula una falla a proposito para poder probar la Cola 2.
            if (mensaje.getNumeroGuia() != null && mensaje.getNumeroGuia().startsWith("FORZAR_ERROR")) {
                throw new RuntimeException("Error simulado para pruebas de Cola 2");
            }

            rabbitTemplate.convertAndSend(exchangeName, routingKey1, mensaje);
            log.info("Guia {} publicada correctamente en Cola 1", mensaje.getNumeroGuia());
        } catch (Exception e) {
            log.error("Fallo al publicar la guia {} en Cola 1: {}. Se enviara a Cola 2 (errores)",
                    mensaje.getNumeroGuia(), e.getMessage());
            enviarAColaErrores(mensaje, e.getMessage());
        }
    }

    private void enviarAColaErrores(GuiaMensajeDTO mensaje, String motivoError) {
        try {
            mensaje.setMotivoError(motivoError);
            rabbitTemplate.convertAndSend(exchangeName, routingKey2, mensaje);
            log.warn("Guia {} enviada a Cola 2 (errores)", mensaje.getNumeroGuia());
        } catch (Exception ex) {
            log.error("No fue posible publicar ni siquiera en la Cola 2 para la guia {}: {}",
                    mensaje.getNumeroGuia(), ex.getMessage());
        }
    }
}
