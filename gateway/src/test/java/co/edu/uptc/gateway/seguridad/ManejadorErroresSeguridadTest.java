package co.edu.uptc.gateway.seguridad;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpResponse;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ManejadorErroresSeguridadTest {

    private final ManejadorErroresSeguridad manejador = new ManejadorErroresSeguridad();

    private MockServerWebExchange nuevoIntercambio() {
        return MockServerWebExchange.from(MockServerHttpRequest.get("/api/admisiones").build());
    }

    @Test
    void sinTokenResponde401EnJsonConCabeceraBearer() {
        MockServerWebExchange exchange = nuevoIntercambio();

        manejador.commence(exchange, new BadCredentialsException("sin token")).block();

        MockServerHttpResponse respuesta = exchange.getResponse();
        assertEquals(HttpStatus.UNAUTHORIZED, respuesta.getStatusCode());
        assertEquals("Bearer", respuesta.getHeaders().getFirst("WWW-Authenticate"));
        assertEquals(MediaType.APPLICATION_JSON, respuesta.getHeaders().getContentType());
        String cuerpo = respuesta.getBodyAsString().block();
        assertTrue(cuerpo.contains("\"status\":401"));
        assertTrue(cuerpo.contains("\"mensaje\""));
    }

    @Test
    void rolSinPermisoResponde403EnJson() {
        MockServerWebExchange exchange = nuevoIntercambio();

        manejador.handle(exchange, new AccessDeniedException("denegado")).block();

        MockServerHttpResponse respuesta = exchange.getResponse();
        assertEquals(HttpStatus.FORBIDDEN, respuesta.getStatusCode());
        String cuerpo = respuesta.getBodyAsString().block();
        assertTrue(cuerpo.contains("\"status\":403"));
        assertTrue(cuerpo.contains("Tu rol no tiene permiso"));
    }
}
