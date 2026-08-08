package com.optimizesolux.notificationhub.infrastructure.template;

import io.pebbletemplates.pebble.PebbleEngine;
import io.pebbletemplates.pebble.template.PebbleTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;

@Component
public class PebbleTemplateRenderer {

    private final PebbleEngine engine =
            new PebbleEngine.Builder().autoEscaping(false).newLineTrimming(false).build();

    public String render(String templateSource, Map<String, Object> data) {
        try {
            PebbleTemplate template = engine.getLiteralTemplate(templateSource);
            StringWriter writer = new StringWriter();
            template.evaluate(writer, data != null ? data : Map.of());
            return writer.toString();
        } catch (IOException e) {
            throw new IllegalStateException("Template rendering failed: " + e.getMessage(), e);
        }
    }
}
