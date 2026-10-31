package com.obraia.backend.planning;

import java.util.List;
import java.util.Map;

import com.obraia.backend.entity.catalog.SizeCategory;

/**
 * Phases of a commercial unit: the standard ones with the tasks of a
 * shop, plus a last phase to fit out the front for the public.
 */
public class CommercialUnitPhasePlanner extends StandardPhasePlanner {

    static final String FIT_OUT = "Adecuación comercial (fachada, vitrina y cortina metálica)";

    private static final Map<String, List<String>> TASKS = Map.of(
            "Preliminares y replanteo", List.of("Limpieza del lote", "Cerramiento provisional", "Replanteo de ejes"),
            "Excavación y movimiento de tierras", List.of("Excavación de zapatas y vigas de cimentación",
                    "Retiro de material sobrante"),
            "Cimentación", List.of("Solado de limpieza", "Armado de acero de zapatas", "Fundida de zapatas",
                    "Vigas de cimentación"),
            "Estructura (columnas, vigas y placas)", List.of("Columnas", "Vigas aéreas para luces amplias",
                    "Placa de entrepiso"),
            "Mampostería", List.of("Muros de cerramiento", "Muro de bodega"),
            "Cubierta", List.of("Estructura de la cubierta", "Instalación de tejas"),
            "Instalaciones hidrosanitarias y eléctricas", List.of("Red de agua y baño del local",
                    "Acometida eléctrica para comercio", "Tablero de circuitos"),
            "Pañetes, pisos y acabados", List.of("Pañetes", "Piso de alto tráfico", "Pintura"),
            FIT_OUT, List.of("Fachada y aviso", "Vitrina", "Cortina metálica"));

    @Override
    protected Map<String, List<String>> tasksByPhase() {
        return TASKS;
    }

    @Override
    protected List<ExtraPhase> extraPhases(SizeCategory size) {
        return List.of(new ExtraPhase(FIT_OUT, size == SizeCategory.SMALL ? 7 : 10));
    }
}
