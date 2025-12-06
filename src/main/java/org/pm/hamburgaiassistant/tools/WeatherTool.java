package org.pm.hamburgaiassistant.tools;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class WeatherTool {

    @Value("${weather.api.key}")
    private String apiKey;

    @Value("${weather.api.url}")
    private String apiUrl;

    private final OkHttpClient client = new OkHttpClient();

    @Tool("""
            Get current real-time weather conditions for any city.
            
            **When to use this tool:**
            - User asks about current weather, temperature, or conditions
            - User wants to know what to wear or if they need an umbrella
            - Planning outdoor activities and need weather information
            - Comparing weather between cities
            
            **Examples:**
            - "What's the weather like in Hamburg?"
            - "Is it raining right now?"
            - "Do I need a jacket today?"
            - "What's the temperature?"
            
            **Parameters:**
            - city: The city name (e.g., "Hamburg", "Berlin", "Munich")
            
            **Returns:** Current temperature, weather description, feels-like temperature, and humidity.
            Always mention the weather when recommending outdoor activities!
            """)
    public String getCurrentWeather(String city) {
        try {
            String url = String.format("%s?q=%s&appid=%s&units=metric",
                    apiUrl, city, apiKey);

            Request request = new Request.Builder()
                    .url(url)
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    log.error("Weather API call failed: {}", response.code());
                    return "Sorry, I couldn't fetch the weather data for " + city;
                }

                String responseBody = response.body().string();
                JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();

                String weatherDescription = json.getAsJsonArray("weather")
                        .get(0).getAsJsonObject()
                        .get("description").getAsString();

                double temperature = json.getAsJsonObject("main")
                        .get("temp").getAsDouble();

                double feelsLike = json.getAsJsonObject("main")
                        .get("feels_like").getAsDouble();

                int humidity = json.getAsJsonObject("main")
                        .get("humidity").getAsInt();

                return String.format(
                        "Weather in %s: %s. Temperature: %.1f°C (feels like %.1f°C). Humidity: %d%%",
                        city, weatherDescription, temperature, feelsLike, humidity
                );
            }
        } catch (Exception e) {
            log.error("Error fetching weather", e);
            return "Error fetching weather: " + e.getMessage();
        }
    }
}