package org.pm.hamburgaiassistant.tools;

import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Slf4j
@Component
@RequiredArgsConstructor
public class HamburgEventsTool {

    private final GooglePlacesTool googlePlacesTool;



    public String findHamburgEvents(String eventType) {
        // Use Google Places to find event venues
        String query = eventType + " events venues";
        String placesResult = googlePlacesTool.searchPlaces(query, "Hamburg");

        LocalDate today = LocalDate.now();
        String formattedDate = today.format(DateTimeFormatter.ofPattern("MMMM dd, yyyy"));

        StringBuilder result = new StringBuilder();
        result.append("Hamburg Events & Venues (as of ").append(formattedDate).append("):\n\n");
        result.append(placesResult).append("\n\n");
        result.append("**Popular Hamburg Events & Festivals:**\n");
        result.append("- **Hamburg DOM** - One of Europe's largest funfairs (held 3 times per year)\n");
        result.append("- **Hafengeburtstag (Port Anniversary)** - May - Massive harbor celebration\n");
        result.append("- **Reeperbahn Festival** - September - Music and arts festival\n");
        result.append("- **Christmas Markets** - November-December - Traditional German markets\n");
        result.append("- **Alstervergnügen** - Late summer - Festival around the Alster lake\n");
        result.append("- **Long Night of Museums** - Twice yearly - Museums open late with special programs\n\n");
        result.append("💡 **Tip:** For real-time event listings, check:\n");
        result.append("- hamburg.com/events\n");
        result.append("- Eventbrite Hamburg\n");
        result.append("- Local venue websites\n");

        return result.toString();
    }

    @Tool("""
        Find Hamburg-specific events, festivals, cultural activities, and entertainment.
        
        **When to use this tool:**
        - User asks what to do in Hamburg
        - Looking for events, concerts, or shows
        - Asking about festivals or seasonal activities
        - Questions about Hamburg's cultural scene
        - Looking for entertainment or nightlife
        
        **Examples:**
        - "What events are happening this weekend?"
        - "Any concerts coming up?"
        - "What festivals does Hamburg have?"
        - "Things to do at night in Hamburg"
        - "Cultural activities in Hamburg"
        
        **Parameters:**
        - eventType: Type of event (e.g., "concerts", "theater", "festivals", "museums")
        
        **Returns:** Event venues, major Hamburg festivals, and tips for finding current events.
        Hamburg has amazing cultural offerings year-round - be enthusiastic!
        """)
    public String getHamburgVenues() {
        return "**Major Hamburg Venues & Cultural Centers:**\n\n" +
                "**Concert Halls & Music:**\n" +
                "- Elbphilharmonie - World-class concert hall with stunning architecture\n" +
                "- Laeiszhalle - Historic concert hall for classical music\n" +
                "- Barclays Arena - Large venue for concerts and sports\n\n" +
                "**Theaters:**\n" +
                "- Deutsches Schauspielhaus - One of Germany's most important theaters\n" +
                "- Thalia Theater - Renowned theater company\n" +
                "- English Theatre of Hamburg - English-language performances\n\n" +
                "**Museums:**\n" +
                "- Hamburger Kunsthalle - Major art museum\n" +
                "- Miniatur Wunderland - World's largest model railway\n" +
                "- International Maritime Museum\n\n" +
                "**Event Spaces:**\n" +
                "- Hamburg Messe - Trade fair and convention center\n" +
                "- Reeperbahn - Famous entertainment district\n" +
                "- Speicherstadt - Historic warehouse district with galleries";
    }
}