package co.edu.uptc.auth_service.infraestructura.seguridad;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class NormalizadorEmailAuthFilter extends OncePerRequestFilter {

    private static final Pattern PATRON_EMAIL = Pattern.compile("(\"email\"\\s*:\\s*\")([^\"]*)(\")");

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String ruta = request.getRequestURI();
        boolean esRutaAuth = "POST".equalsIgnoreCase(request.getMethod())
                && ("/api/auth/login".equals(ruta) || "/api/auth/usuarios".equals(ruta));

        if (!esRutaAuth) {
            filterChain.doFilter(request, response);
            return;
        }

        byte[] bytesOriginales = request.getInputStream().readAllBytes();
        byte[] bytesProcesados = normalizarEmailSiEsJsonValido(bytesOriginales);

        HttpServletRequest requestEnvuelto = new HttpServletRequestWrapper(request) {
            @Override
            public ServletInputStream getInputStream() {
                ByteArrayInputStream bais = new ByteArrayInputStream(bytesProcesados);
                return new ServletInputStream() {
                    @Override
                    public int read() {
                        return bais.read();
                    }

                    @Override
                    public boolean isFinished() {
                        return bais.available() == 0;
                    }

                    @Override
                    public boolean isReady() {
                        return true;
                    }

                    @Override
                    public void setReadListener(ReadListener readListener) {
                    }
                };
            }

            @Override
            public BufferedReader getReader() {
                return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
            }

            @Override
            public int getContentLength() {
                return bytesProcesados.length;
            }

            @Override
            public long getContentLengthLong() {
                return bytesProcesados.length;
            }
        };

        filterChain.doFilter(requestEnvuelto, response);
    }

    private byte[] normalizarEmailSiEsJsonValido(byte[] raw) {
        if (raw.length == 0) {
            return raw;
        }
        String texto = new String(raw, StandardCharsets.UTF_8);
        String recorte = texto.trim();
        if (!recorte.startsWith("{") || !recorte.endsWith("}")) {
            return raw;
        }
        Matcher matcher = PATRON_EMAIL.matcher(texto);
        if (matcher.find()) {
            String emailNormalizado = matcher.group(2).trim().toLowerCase(Locale.ROOT);
            String reemplazado = matcher.replaceFirst(
                    Matcher.quoteReplacement(matcher.group(1) + emailNormalizado + matcher.group(3)));
            return reemplazado.getBytes(StandardCharsets.UTF_8);
        }
        return raw;
    }
}