package pe.techsupport.ticket.service;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Component;

import pe.techsupport.ticket.domain.Priority;
import pe.techsupport.ticket.domain.Specialty;
import pe.techsupport.ticket.domain.SupportLevel;

/**
 * Reglas de criticidad (tabla de la sección 10 del documento): cada categoría tiene una criticidad
 * base y palabras clave que la elevan. El texto se normaliza (minúsculas, sin tildes) antes de comparar.
 */
@Component
public class PriorityEvaluator {

    record Rule(Priority base, List<String> keywords, Priority elevated) {
    }

    private static final Map<Specialty, Rule> RULES = Map.of(
            Specialty.BASES_DATOS, new Rule(Priority.ALTA,
                    List.of("caida", "produccion", "no conecta"), Priority.CRITICA),
            Specialty.SEGURIDAD_INFORMATICA, new Rule(Priority.ALTA,
                    List.of("brecha", "ataque", "acceso no autorizado"), Priority.CRITICA),
            Specialty.REDES_CONECTIVIDAD, new Rule(Priority.MEDIA,
                    List.of("caida total", "sin internet"), Priority.ALTA),
            Specialty.SISTEMAS_SOFTWARE, new Rule(Priority.MEDIA,
                    List.of("no responde", "error critico"), Priority.ALTA),
            Specialty.HARDWARE_MANTENIMIENTO, new Rule(Priority.BAJA,
                    List.of("no enciende", "pantalla azul"), Priority.MEDIA));

    public Priority evaluate(Specialty category, String title, String description) {
        Rule rule = RULES.get(category);
        String text = normalize(title + " " + description);
        return rule.keywords().stream().anyMatch(text::contains) ? rule.elevated() : rule.base();
    }

    /** Un incidente crítico exige como mínimo un especialista N2. */
    public SupportLevel requiredLevel(Priority priority) {
        return priority == Priority.CRITICA ? SupportLevel.N2 : SupportLevel.N1;
    }

    static String normalize(String text) {
        String noAccents = Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return noAccents.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    }
}
