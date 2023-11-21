package br.com.matheushramos.pubsubgcpconsumer;

import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import com.google.cloud.spring.pubsub.integration.AckMode;
import com.google.cloud.spring.pubsub.integration.inbound.PubSubInboundChannelAdapter;
import com.google.cloud.spring.pubsub.support.BasicAcknowledgeablePubsubMessage;
import com.google.cloud.spring.pubsub.support.GcpPubSubHeaders;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class PubSubConfig {

    @Value("${spring.cloud.gcp.project-id}")
    private String projectId;

    @Value("${pubsub.pull-endpoint}")
    private String subscriptionEndpoint;

//    @Bean
//    public Subscriber inboundSubscriberMessages() {
//        log.info("Subscriber inboundSubscriberMessages...");
//        Subscriber subscriber = Subscriber.newBuilder(
//                        ProjectSubscriptionName.of(projectId, subscriptionEndpoint),
//                        (MessageReceiver) (pubsubMessage, ackReplyConsumer) -> {
//                            final var messageDataReceived = pubsubMessage.getData().toStringUtf8();
//                            log.info("Message arrived via subscriber {}! Payload: {}", subscriptionEndpoint, messageDataReceived);
//
//                            final var messageConverted = new Gson().fromJson(messageDataReceived, MessageSubscriberDTO.class);
//                            MDC.put("trackingId", messageConverted.getTrackingId());
//
//                            ackReplyConsumer.ack();
//                            log.info("Message readed successfully. Origin: {}", messageConverted.getOrigin());
//                            paymentOrderSubscriberService.messageProcess(messageConverted);
//                        })
//                .build();
//
//        subscriber.startAsync();
//
//        return subscriber;
//    }

    @Bean
    public MessageChannel inputMessageChannel() {
        return new DirectChannel();
    }

    @Bean
    public PubSubInboundChannelAdapter inboundChannelAdapter(@Qualifier("inputMessageChannel") MessageChannel messageChannel,
                                                             PubSubTemplate pubSubTemplate) {
        PubSubInboundChannelAdapter adapter = new PubSubInboundChannelAdapter(pubSubTemplate, this.subscriptionEndpoint);
        adapter.setOutputChannel(messageChannel);
        adapter.setAckMode(AckMode.MANUAL);
        adapter.setPayloadType(String.class);
        return adapter;
    }

    @Bean
    @ServiceActivator(inputChannel = "inputMessageChannel")
    public MessageHandler messageReceiver() {
        return messageReceived -> {
            BasicAcknowledgeablePubsubMessage originalMessage = messageReceived.getHeaders().get(GcpPubSubHeaders.ORIGINAL_MESSAGE, BasicAcknowledgeablePubsubMessage.class);
            log.info("Message arrived via an inbound channel adapter from {}! Payload: {}", subscriptionEndpoint, messageReceived);
            log.info("Original message: {}", originalMessage);
            originalMessage.ack();
        };
    }
}
