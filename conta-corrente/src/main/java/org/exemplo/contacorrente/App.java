package org.exemplo.contacorrente;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.PathHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;

import static io.micrometer.prometheus.PrometheusConfig.DEFAULT;
import static io.undertow.UndertowOptions.ALWAYS_SET_KEEP_ALIVE;
import static io.undertow.util.Headers.CONTENT_TYPE;

public class App {

    private static final Logger LOGGER = LoggerFactory.getLogger(App.class);
    private static final String HOST = "localhost";
    private static final String TEXT_PLAIN = "text/plain";

    public static void main(final String[] args) {
        var inicio = Instant.now();
        Config conf = ConfigFactory.load();
        int porta = conf.getInt("application.port");
        Undertow.builder()
                .addHttpListener(porta, HOST)
                .setHandler(new App().handler())
                .setServerOption(ALWAYS_SET_KEEP_ALIVE, false)
                .build()
                .start();
        var fim = Instant.now();
        LOGGER.info("Aplicação rodando na porta {}, iniciada em {} ms.", porta, Duration.between(inicio, fim).toMillis());
    }

    private HttpHandler handler() {
        var registry = new PrometheusMeterRegistry(DEFAULT);
        new ClassLoaderMetrics().bindTo(registry);
        new JvmMemoryMetrics().bindTo(registry);
        new JvmGcMetrics().bindTo(registry);
        new ProcessorMetrics().bindTo(registry);
        new JvmThreadMetrics().bindTo(registry);

        return new PathHandler()
                .addExactPath("/prometheus", exchange -> {
                    exchange.getResponseHeaders().put(CONTENT_TYPE, TEXT_PLAIN);
                    exchange.getResponseSender().send(registry.scrape());
                })
                .addExactPath("/hello", exchange -> {
                    exchange.getResponseHeaders().put(CONTENT_TYPE, TEXT_PLAIN);
                    exchange.getResponseSender().send("Hello World");
                });
    }
}
