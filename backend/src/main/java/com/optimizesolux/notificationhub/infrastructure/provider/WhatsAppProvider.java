package com.optimizesolux.notificationhub.infrastructure.provider;

import java.util.List;
import java.util.Map;

/** SPI WhatsApp — une classe par fournisseur (Twilio, Meta, …). */
public interface WhatsAppProvider {

    String id();

    /**
     * @param contentSidOrBody Content SID Twilio (HX…) ou texte session 24h
     * @param templateData variables Content Template (optionnel)
     */
    String send(
            String from,
            List<String> to,
            String contentSidOrBody,
            Map<String, Object> templateData)
            throws Exception;
}
