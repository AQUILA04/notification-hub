package com.optimizesolux.notificationhub.infrastructure.provider;

import com.optimizesolux.notificationhub.application.SmsTestRecipientService;
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
    @Mock private SmsTestRecipientService smsTestRecipientService;

    @Test
    void resolveRecipientsPrefersDatabaseList() {
        assertEquals(
                List.of("a@example.com", "b@example.com"),
                MailpitSmsBridge.resolveRecipients(
                        List.of("a@example.com", "b@example.com"), "sms@optimizesolux.com"));
    }

    @Test
    void resolveRecipientsFallsBackToConfiguredWhenDbEmpty() {
        assertEquals(
                List.of("devs@example.com"),
                MailpitSmsBridge.resolveRecipients(List.of(), "devs@example.com"));
        assertEquals(
                List.of("devs@example.com"),
                MailpitSmsBridge.resolveRecipients(null, "devs@example.com"));
    }

    @Test
    void resolveRecipientsDefaultsToSmsInboxWhenEmpty() {
        assertEquals(
                List.of("sms@optimizesolux.com"),
                MailpitSmsBridge.resolveRecipients(List.of(), null));
        assertEquals(
                List.of("sms@optimizesolux.com"),
                MailpitSmsBridge.resolveRecipients(List.of(), "  "));
    }

    @Test
    void sendDelegatesToSmtpWithDistributionList() throws Exception {
        when(emailProviderRegistry.active()).thenReturn(emailProvider);
        when(emailProvider.send(anyString(), anyList(), anyString(), anyString()))
                .thenReturn("smtp-1");
        when(smsTestRecipientService.activeEmails())
                .thenReturn(List.of("ahonsueric01@gmail.com", "sms@optimizesolux.com"));

        NotificationHubProperties properties =
                new NotificationHubProperties(
                        null, null, null, null, null, null, null, null, null, null, null, null, null);
        MailpitSmsBridge bridge =
                new MailpitSmsBridge(emailProviderRegistry, properties, smsTestRecipientService);

        String id = bridge.send("OptimizeSLX", List.of("+22890909090"), "OTP 4242");

        assertEquals("smtp-1", id);
        verify(emailProvider)
                .send(
                        eq("noreply@optimizesolux.com"),
                        eq(List.of("ahonsueric01@gmail.com", "sms@optimizesolux.com")),
                        eq("[SMS TEST] +22890909090"),
                        org.mockito.ArgumentMatchers.argThat(
                                html ->
                                        html.contains("OTP 4242")
                                                && html.contains("OptimizeSLX")
                                                && html.contains("ahonsueric01@gmail.com")
                                                && html.contains("sms@optimizesolux.com")));
        assertTrue(
                MailpitSmsBridge.renderHtml("A", List.of("+1"), "<script>", List.of("x@y.z"))
                        .contains("&lt;script&gt;"));
    }

    @Test
    void sendFallsBackToConfiguredWhenListEmpty() throws Exception {
        when(emailProviderRegistry.active()).thenReturn(emailProvider);
        when(emailProvider.send(anyString(), anyList(), anyString(), anyString()))
                .thenReturn("smtp-2");
        when(smsTestRecipientService.activeEmails()).thenReturn(List.of());

        NotificationHubProperties properties =
                new NotificationHubProperties(
                        null, null, null, null, null, null, null, null, null, null, null, null, null);
        MailpitSmsBridge bridge =
                new MailpitSmsBridge(emailProviderRegistry, properties, smsTestRecipientService);

        bridge.send("From", List.of("+1"), "body");

        verify(emailProvider)
                .send(
                        eq("noreply@optimizesolux.com"),
                        eq(List.of("sms@optimizesolux.com")),
                        eq("[SMS TEST] +1"),
                        org.mockito.ArgumentMatchers.anyString());
    }
}
