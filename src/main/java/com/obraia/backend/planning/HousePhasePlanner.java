package com.obraia.backend.planning;

import java.util.List;
import java.util.Map;

/** Phases of a house: the standard ones, with the tasks of a home. Task names are data, so they are in Spanish. */
public class HousePhasePlanner extends StandardPhasePlanner {

    private static final Map<String, List<String>> TASKS = Map.of(
            "Preliminares y replanteo", List.of("Limpieza del lote", "Cerramiento provisional", "Replanteo de ejes"),
            "Excavación y movimiento de tierras", List.of("Excavación de zapatas y vigas de cimentación",
                    "Retiro de material sobrante"),
            "Cimentación", List.of("Solado de limpieza", "Armado de acero de zapatas", "Fundida de zapatas",
                    "Vigas de cimentación"),
            "Estructura (columnas, vigas y placas)", List.of("Columnas", "Vigas aéreas", "Placa de entrepiso"),
            "Mampostería", List.of("Muros de fachada", "Muros interiores"),
            "Cubierta", List.of("Estructura de la cubierta", "Instalación de tejas"),
            "Instalaciones hidrosanitarias y eléctricas", List.of("Redes de agua potable", "Redes sanitarias",
                    "Cableado eléctrico de la vivienda"),
            "Pañetes, pisos y acabados", List.of("Pañetes", "Pisos y enchapes de baños y cocina", "Pintura"));

    @Override
    protected Map<String, List<String>> tasksByPhase() {
        return TASKS;
    }
}
