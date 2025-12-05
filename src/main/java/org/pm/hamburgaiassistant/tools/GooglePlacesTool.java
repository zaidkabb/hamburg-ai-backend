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
public class GooglePlacesTool {

    @Value("${google.places.api.key}")
    private String apiKey;

    @Value("${google.places.api.url}")
    private String apiUrl;

    private final OkHttpClient client = new OkHttpClient();

    @Tool("Search for places like restaurants, museums, hotels, or attractions in a city. " +
            "Use this when user asks about places to visit, eat, or stay.")
    public String searchPlaces(String query, String location) {
        try {
            // Build the search query
            String searchQuery = query + " in " + location;
            String encodedQuery = URLEncoder.encode(searchQuery, StandardCharsets.UTF_8);

            String url = String.format("%s/textsearch/json?query=%s&key=%s",
                    apiUrl, encodedQuery, apiKey);

            Request request = new Request.Builder()
                    .url(url)
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    log.error("Google Places API call failed: {}", response.code());
                    return "Sorry, I couldn't fetch places data.";
                }

                String responseBody = response.body().string();
                JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();

                if (!json.has("results") || json.getAsJsonArray("results").isEmpty()) {
                    return "No places found for: " + searchQuery;
                }

                // Parse results
                JsonArray results = json.getAsJsonArray("results");
                StringBuilder resultText = new StringBuilder();
                resultText.append("Found ").append(Math.min(results.size(), 5))
                        .append(" places for '").append(searchQuery).append("':\n\n");

                // Get top 5 results
                for (int i = 0; i < Math.min(5, results.size()); i++) {
                    JsonObject place = results.get(i).getAsJsonObject();

                    String name = place.get("name").getAsString();
                    String address = place.has("formatted_address")
                            ? place.get("formatted_address").getAsString()
                            : "Address not available";

                    double rating = place.has("rating")
                            ? place.get("rating").getAsDouble()
                            : 0.0;

                    int userRatingsTotal = place.has("user_ratings_total")
                            ? place.get("user_ratings_total").getAsInt()
                            : 0;

                    resultText.append(i + 1).append(". **").append(name).append("**\n");
                    resultText.append("   - Address: ").append(address).append("\n");

                    if (rating > 0) {
                        resultText.append("   - Rating: ").append(rating)
                                .append("/5 (").append(userRatingsTotal).append(" reviews)\n");
                    }

                    if (place.has("opening_hours")) {
                        JsonObject openingHours = place.getAsJsonObject("opening_hours");
                        boolean openNow = openingHours.has("open_now")
                                && openingHours.get("open_now").getAsBoolean();
                        resultText.append("   - Status: ")
                                .append(openNow ? "Open now" : "Closed now")
                                .append("\n");
                    }

                    resultText.append("\n");
                }

                return resultText.toString();
            }
        } catch (Exception e) {
            log.error("Error fetching places", e);
            return "Error fetching places: " + e.getMessage();
        }
    }

    @Tool("Get details about a specific place by name and location")
    public String getPlaceDetails(String placeName, String location) {
        return searchPlaces(placeName, location);
    }
}