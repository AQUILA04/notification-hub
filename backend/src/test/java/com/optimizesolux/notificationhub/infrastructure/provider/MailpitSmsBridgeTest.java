package com.optimizesolux.notificationhub.infrastructure.provider;

import com.optimizesolux.notificationhub.config.NotificationHubProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MailpitSmsBridgeTest {

    @Mock private EmailProviderRegistry emailProviderRegistry;
    @Mock private EmailProvider emailProvider;

    @Test
    void resolveMailToUsesConfiguredInbox() {
        assertEquals("devs@example.com", MailpitSmsBridge.resolveMailTo("devs@example.com"));
    }

    @Test
    void resolveMailToDefaultsToSmsInbox() {
        assertEquals("sms@optimizesolux.com", MailpitSmsBridge.resolveMailTo(null));
        assertEquals("sms@optimizesolux.com", MailpitSmsBridge.resolveMailTo("  "));
    }

    @Test
    void sendDelegatesToSmtpWithInterceptSubject() throws Exception {
        when(emailProviderRegistry.active()).thenReturn(emailProvider);
        when(emailProvider.send(anyString(), anyList(), anyString(), anyString()))
                .thenReturn("smtp-1");

        NotificationHubProperties properties =
                new NotificationHubProperties(
                        null, null, null, null, null, null, null, null, null, null, null, null, null);
        MailpitSmsBridge bridge = new MailpitSmsBridge(emailProviderRegistry, properties);

        String id = bridge.send("OptimizeSLX", List.of("+22890909090"), "OTP 4242");

        assertEquals("smtp-1", id);
        verify(emailProvider)
                .send(
                        eq("noreply@optimizesolux.com"),
                        eq(List.of("sms@optimizesolux.com")),
                        eq("[SMS TEST] +22890909090"),
                        org.mockito.ArgumentMatchers.argThat(
                                html -> html.contains("OTP 4242") && html.contains("OptimizeSLX")));
        assertTrue(MailpitSmsBridge.renderHtml("A", List.of("+1"), "<script>").contains("&lt;script&gt;"));
    }
}
