package org.pm.hamburgaiassistant.tools;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
public class GoogleDirectionsTool {

    @Value("${google.places.api.key}")
    private String apiKey;

    private final OkHttpClient client = new OkHttpClient();

    @Tool("""
        Get detailed directions and route information between two locations.
        
        **When to use this tool:**
        - User asks how to get from one place to another
        - Questions about travel time or distance
        - Planning routes or transportation
        - Asking about public transit, walking, or driving directions
        
        **Examples:**
        - "How do I get to the Elbphilharmonie from the train station?"
        - "What's the fastest way to the airport?"
        - "How long does it take to walk to Miniatur Wunderland?"
        - "Show me the route from my hotel to Reeperbahn"
        
        **Parameters:**
        - origin: Starting location (address, landmark, or place name)
        - destination: End location (address, landmark, or place name)
        - mode: Transportation mode (transit/walking/driving/bicycling) - defaults to "transit"
        
        **Returns:** Step-by-step directions with distance, duration, and transit details.
        For Hamburg, always recommend public transit (U-Bahn/S-Bahn) as it's excellent!
        """)

    public String getDirections(String origin, String destination, String mode) {
        try {
            // Default to transit if not specified
            if (mode == null || mode.isEmpty()) {
                mode = "transit";
            }

            String encodedOrigin = URLEncoder.encode(origin, StandardCharsets.UTF_8);
            String encodedDestination = URLEncoder.encode(destination, StandardCharsets.UTF_8);

            String url = String.format(
                    "https://maps.googleapis.com/maps/api/directions/json?origin=%s&destination=%s&mode=%s&key=%s",
                    encodedOrigin, encodedDestination, mode.toLowerCase(), apiKey
            );

            Request request = new Request.Builder()
                    .url(url)
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    log.error("Google Directions API call failed: {}", response.code());
                    return "Sorry, I couldn't fetch directions.";
                }

                String responseBody = response.body().string();
                JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();

                if (!json.get("status").getAsString().equals("OK")) {
                    return "No route found between " + origin + " and " + destination;
                }

                // Parse first route
                JsonArray routes = json.getAsJsonArray("routes");
                if (routes.isEmpty()) {
                    return "No routes available.";
                }

                JsonObject route = routes.get(0).getAsJsonObject();
                JsonArray legs = route.getAsJsonArray("legs");
                JsonObject leg = legs.get(0).getAsJsonObject();

                // Extract information
                String distance = leg.getAsJsonObject("distance").get("text").getAsString();
                String duration = leg.getAsJsonObject("duration").get("text").getAsString();
                String startAddress = leg.get("start_address").getAsString();
                String endAddress = leg.get("end_address").getAsString();

                StringBuilder result = new StringBuilder();
                result.append("**Directions from ").append(origin)
                        .append(" to ").append(destination).append("**\n\n");
                result.append("📍 **Start:** ").append(startAddress).append("\n");
                result.append("📍 **End:** ").append(endAddress).append("\n");
                result.append("📏 **Distance:** ").append(distance).append("\n");
                result.append("⏱️ **Duration:** ").append(duration).append("\n");
                result.append("🚌 **Mode:** ").append(mode.substring(0, 1).toUpperCase())
                        .append(mode.substring(1)).append("\n\n");

                // Parse steps
                JsonArray steps = leg.getAsJsonArray("steps");
                result.append("**Route Steps:**\n");

                int stepCount = Math.min(steps.size(), 8); // Limit to 8 steps for readability
                for (int i = 0; i < stepCount; i++) {
                    JsonObject step = steps.get(i).getAsJsonObject();
                    String instruction = step.get("html_instructions").getAsString()
                            .replaceAll("<[^>]*>", ""); // Remove HTML tags
                    String stepDistance = step.getAsJsonObject("distance").get("text").getAsString();
                    String stepDuration = step.getAsJsonObject("duration").get("text").getAsString();

                    result.append(i + 1).append(". ").append(instruction)
                            .append(" (").append(stepDistance).append(", ")
                            .append(stepDuration).append(")\n");

                    // Add transit details if available
                    if (step.has("transit_details")) {
                        JsonObject transit = step.getAsJsonObject("transit_details");
                        JsonObject line = transit.getAsJsonObject("line");
                        String lineName = line.get("short_name").getAsString();
                        String vehicle = line.getAsJsonObject("vehicle").get("name").getAsString();
                        String departure = transit.getAsJsonObject("departure_stop").get("name").getAsString();
                        String arrival = transit.getAsJsonObject("arrival_stop").get("name").getAsString();
                        int numStops = transit.get("num_stops").getAsInt();

                        result.append("   🚇 Take ").append(vehicle).append(" ")
                                .append(lineName).append(" from ").append(departure)
                                .append(" to ").append(arrival)
                                .append(" (").append(numStops).append(" stops)\n");
                    }
                }

                if (steps.size() > stepCount) {
                    result.append("... and ").append(steps.size() - stepCount)
                            .append(" more steps\n");
                }

                return result.toString();
            }
        } catch (Exception e) {
            log.error("Error fetching directions", e);
            return "Error fetching directions: " + e.getMessage();
        }
    }

    @Tool("Get travel time between two locations")
    public String getTravelTime(String origin, String destination) {
        String directions = getDirections(origin, destination, "transit");
        // Extract just the duration from the full directions
        return directions;
    }
}