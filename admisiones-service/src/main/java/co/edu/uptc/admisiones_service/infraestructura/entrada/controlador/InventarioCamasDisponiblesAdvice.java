package co.edu.uptc.admisiones_service.infraestructura.entrada.controlador;

import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestControllerAdvice
public class InventarioCamasDisponiblesAdvice implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(
            Object body,
            MethodParameter returnType,
            MediaType selectedContentType,
            Class<? extends HttpMessageConverter<?>> selectedConverterType,
            ServerHttpRequest request,
            ServerHttpResponse response) {
        if (body == null || !request.getURI().getPath().startsWith("/api/admisiones/inventario-camas")) {
            return body;
        }
        if (body instanceof Iterable<?> lista) {
            List<Object> resultado = new ArrayList<>();
            for (Object item : lista) {
                resultado.add(enriquecerElemento(item));
            }
            return resultado.toArray();
        }
        return enriquecerElemento(body);
    }

    private Object enriquecerElemento(Object item) {
        if (item == null) {
            return null;
        }
        Map<String, Object> mapa = new LinkedHashMap<>();
        if (item instanceof Map<?, ?> original) {
            original.forEach((k, v) -> mapa.put(String.valueOf(k), v));
        } else {
            Class<?> clazz = item.getClass();
            if (clazz.isRecord()) {
                for (RecordComponent rc : clazz.getRecordComponents()) {
                    try {
                        Method acc = rc.getAccessor();
                        acc.setAccessible(true);
                        mapa.put(rc.getName(), acc.invoke(item));
                    } catch (Exception ignored) {
                    }
                }
            } else {
                for (Method m : clazz.getDeclaredMethods()) {
                    if (m.getParameterCount() == 0) {
                        String nombre = m.getName();
                        if (nombre.startsWith("get") && nombre.length() > 3) {
                            String prop = Character.toLowerCase(nombre.charAt(3)) + nombre.substring(4);
                            try {
                                m.setAccessible(true);
                                mapa.put(prop, m.invoke(item));
                            } catch (Exception ignored) {
                            }
                        } else if (!nombre.startsWith("set") && !nombre.equals("toString")
                                && !nombre.equals("hashCode")) {
                            try {
                                m.setAccessible(true);
                                mapa.put(nombre, m.invoke(item));
                            } catch (Exception ignored) {
                            }
                        }
                    }
                }
            }
        }
        if (mapa.get("camasTotales") instanceof Number totales
                && mapa.get("camasOcupadas") instanceof Number ocupadas) {
            mapa.putIfAbsent("camasDisponibles", totales.intValue() - ocupadas.intValue());
            return mapa;
        }
        return item;
    }
}
