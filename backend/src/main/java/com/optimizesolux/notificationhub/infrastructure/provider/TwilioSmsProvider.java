package com.optimizesolux.notificationhub.infrastructure.provider;

import com.optimizesolux.notificationhub.config.NotificationHubProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@ConditionalOnProperty(prefix = "notification-hub.sms", name = "provider", havingValue = "twilio")
public class TwilioSmsProvider implements SmsProvider {

    private static final Logger log = LoggerFactory.getLogger(TwilioSmsProvider.class);

    private final NotificationHubProperties properties;
    private final RestTemplate restTemplate;

    public TwilioSmsProvider(NotificationHubProperties properties, RestTemplateBuilder builder) {
        this.properties = properties;
        this.restTemplate =
                builder
                        .setConnectTimeout(Duration.ofSeconds(5))
                        .setReadTimeout(Duration.ofSeconds(15))
                        .build();
    }

    @Override
    public String id() {
        return "twilio";
    }

    @Override
    public String send(String from, List<String> to, String body) throws Exception {
        NotificationHubProperties.Sms sms = properties.sms();
        if (sms.accountSid() == null
                || sms.authToken() == null
                || sms.accountSid().isBlank()
                || sms.authToken().isBlank()) {
            throw new IllegalStateException("Twilio account-sid and auth-token are required");
        }
        String url =
                "https://api.twilio.com/2010-04-01/Accounts/" + sms.accountSid() + "/Messages.json";
        String auth =
                Base64.getEncoder()
                        .encodeToString(
                                (sms.accountSid() + ":" + sms.authToken())
                                        .getBytes(StandardCharsets.UTF_8));

        String lastSid = null;
        for (String destination : to) {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.set(HttpHeaders.AUTHORIZATION, "Basic " + auth);
            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("From", from);
            form.add("To", destination);
            form.add("Body", body);
            @SuppressWarnings("rawtypes")
            ResponseEntity<Map> response =
                    restTemplate.postForEntity(url, new HttpEntity<>(form, headers), Map.class);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new IllegalStateException("Twilio returned " + response.getStatusCode());
            }
            Object sid = response.getBody().get("sid");
            lastSid = sid != null ? sid.toString() : "twilio-" + UUID.randomUUID();
            log.info("SMS Twilio sent to={} sid={}", destination, lastSid);
        }
        return lastSid != null ? lastSid : "twilio-" + UUID.randomUUID();
    }
}
