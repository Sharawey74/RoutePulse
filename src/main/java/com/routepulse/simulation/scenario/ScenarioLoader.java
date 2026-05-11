package com.routepulse.simulation.scenario;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;

/**
 * Loads scenario definitions from classpath JSON files.
 * Scenarios define the courier setup, order injection schedule, and disruption timeline
 * for a complete simulation run.
 *
 * <p><strong>Day 2:</strong> Structure defined. The {@link #load(String)} method is implemented
 * but JSON files are populated in Day 10 when final scenarios are authored.
 * The class is ready to call from Day 3 onward as the state layer becomes available.
 *
 * <p>Scenario files are stored at:
 * {@code src/main/resources/scenarios/{scenarioName}.json}
 */
@Component
public class ScenarioLoader {

    private static final Logger log = LoggerFactory.getLogger(ScenarioLoader.class);
    private static final String SCENARIO_BASE_PATH = "scenarios/";

    private final ObjectMapper objectMapper;

    public ScenarioLoader(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Loads and deserializes a scenario from the classpath resource.
     *
     * @param scenarioName the scenario file name without extension
     *                     (e.g., "rush_hour" → loads "scenarios/rush_hour.json")
     * @return the fully deserialized {@link ScenarioDefinition}
     * @throws IllegalArgumentException if the scenario file does not exist
     * @throws IOException              if the file cannot be read or parsed
     */
    public ScenarioDefinition load(String scenarioName) throws IOException {
        validateScenarioName(scenarioName);

        String resourcePath = SCENARIO_BASE_PATH + scenarioName + ".json";
        ClassPathResource resource = new ClassPathResource(resourcePath);

        if (!resource.exists()) {
            throw new IllegalArgumentException(
                    "Scenario file not found at classpath:" + resourcePath
                    + ". Scenario files are authored in Day 10.");
        }

        log.info("ScenarioLoader: loading scenario '{}'", scenarioName);
        try (InputStream inputStream = resource.getInputStream()) {
            ScenarioDefinition definition = objectMapper.readValue(inputStream, ScenarioDefinition.class);
            log.info("ScenarioLoader: loaded scenario '{}' — {} orders, {} disruptions",
                    scenarioName,
                    definition.orderSchedule().size(),
                    definition.disruptionSchedule().size());
            return definition;
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private void validateScenarioName(String scenarioName) {
        if (scenarioName == null || scenarioName.isBlank()) {
            throw new IllegalArgumentException("Scenario name must not be null or blank");
        }
        // Guard against path traversal
        if (scenarioName.contains("..") || scenarioName.contains("/")) {
            throw new IllegalArgumentException(
                    "Scenario name must not contain path separators, got: " + scenarioName);
        }
    }
}
