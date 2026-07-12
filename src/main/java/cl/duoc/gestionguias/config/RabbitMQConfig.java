package cl.duoc.gestionguias.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuracion de RabbitMQ.
 *
 * Cola 1 (app.rabbitmq.queue1): recibe las guias generadas correctamente.
 * Cola 2 (app.rabbitmq.queue2): almacena los mensajes que fallaron al
 * intentar publicarse en la Cola 1 (cola de errores).
 *
 * Estos @Bean se crean automaticamente cuando la app arranca: no hay que
 * crear las colas a mano desde el panel web de RabbitMQ.
 */
@Configuration
public class RabbitMQConfig {

    @Value("${app.rabbitmq.exchange}")
    private String exchangeName;

    @Value("${app.rabbitmq.queue1}")
    private String queue1Name;

    @Value("${app.rabbitmq.queue2}")
    private String queue2Name;

    @Value("${app.rabbitmq.routingkey1}")
    private String routingKey1;

    @Value("${app.rabbitmq.routingkey2}")
    private String routingKey2;

    @Bean
    public DirectExchange guiasExchange() {
        return new DirectExchange(exchangeName);
    }

    @Bean
    public Queue queueGuias() {
        return new Queue(queue1Name, true); // true = durable, sobrevive a un reinicio de RabbitMQ
    }

    @Bean
    public Queue queueGuiasErrores() {
        return new Queue(queue2Name, true);
    }

    @Bean
    public Binding bindingQueue1(Queue queueGuias, DirectExchange guiasExchange) {
        return BindingBuilder.bind(queueGuias).to(guiasExchange).with(routingKey1);
    }

    @Bean
    public Binding bindingQueue2(Queue queueGuiasErrores, DirectExchange guiasExchange) {
        return BindingBuilder.bind(queueGuiasErrores).to(guiasExchange).with(routingKey2);
    }

    // Convertidor JSON: los mensajes se serializan/deserializan como JSON
    // en vez de usar la serializacion binaria de Java por defecto.
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                          MessageConverter jsonMessageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter);
        return template;
    }
}
