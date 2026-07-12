package cl.duoc.gestionguias.consumer;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import cl.duoc.gestionguias.dto.GuiaMensajeDTO;
import cl.duoc.gestionguias.entity.GuiaProcesada;
import cl.duoc.gestionguias.repository.GuiaProcesadaRepository;

/**
 * Consume los mensajes disponibles en la Cola 1 y los persiste en la
 * tabla "guias_procesadas", tabla distinta a "guias".
 */
@Service
public class GuiaConsumerService {

    private static final Logger log = LoggerFactory.getLogger(GuiaConsumerService.class);

    private final RabbitTemplate rabbitTemplate;
    private final GuiaProcesadaRepository guiaProcesadaRepository;

    @Value("${app.rabbitmq.queue1}")
    private String queue1Name;

    public GuiaConsumerService(RabbitTemplate rabbitTemplate,
                                GuiaProcesadaRepository guiaProcesadaRepository) {
        this.rabbitTemplate = rabbitTemplate;
        this.guiaProcesadaRepository = guiaProcesadaRepository;
    }

    /**
     * Drena todos los mensajes actualmente disponibles en la Cola 1
     * y los guarda en la base de datos. Devuelve la lista de registros guardados.
     */
    public List<GuiaProcesada> procesarMensajesColaUno() {
        List<GuiaProcesada> guardadas = new ArrayList<>();

        GuiaMensajeDTO mensaje;
        while ((mensaje = (GuiaMensajeDTO) rabbitTemplate.receiveAndConvert(queue1Name)) != null) {
            GuiaProcesada procesada = new GuiaProcesada();
            procesada.setGuiaOrigenId(mensaje.getGuiaId());
            procesada.setNumeroGuia(mensaje.getNumeroGuia());
            procesada.setTransportista(mensaje.getTransportista());
            procesada.setFecha(mensaje.getFecha());
            procesada.setEstado(mensaje.getEstado());
            procesada.setUrlS3(mensaje.getUrlS3());
            procesada.setFechaProcesado(LocalDateTime.now());

            guardadas.add(guiaProcesadaRepository.save(procesada));
            log.info("Mensaje de guia {} consumido de Cola 1 y guardado en la tabla guias_procesadas",
                    mensaje.getNumeroGuia());
        }

        return guardadas;
    }
}
