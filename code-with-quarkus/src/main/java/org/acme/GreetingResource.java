package org.acme;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Path("/hello")
public class GreetingResource {

    @Inject
    OpenTelemetry otel;

    private Tracer tracer;

    GreetingResource() {
        tracer = otel.getTracer("instrumentation-scope-name", "instrumentation-scope-version");
    }

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() {
        //Tracer tracer = otel.getTracer("instrumentation-scope-name", "instrumentation-scope-version");

        Span span = tracer.spanBuilder("rollTheDice").startSpan();

        // Make the span the current span
        try (Scope scope = span.makeCurrent()) {
            return "Hello you";
        } catch(Throwable t) {
            span.recordException(t);
            throw t;
        } finally {
            span.end();
        }
    }

    public List<Integer> rollTheDice(int rolls) {
        Span parentSpan = tracer.spanBuilder("parent").startSpan();
        List<Integer> results = new ArrayList<Integer>();
        try {
            for (int i = 0; i < rolls; i++) {
                results.add(this.rollOnce(parentSpan));
            }
            return results;
        } finally {
            parentSpan.end();
        }
    }

    private int rollOnce(Span parentSpan) {
        Span childSpan = tracer.spanBuilder("child")
                .setParent(Context.current().with(parentSpan))
                .startSpan();
        try {
            return ThreadLocalRandom.current().nextInt(this.min, this.max + 1);
        } finally {
            childSpan.end();
        }
    }
}