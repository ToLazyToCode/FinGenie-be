package fingenie.com.fingenie.utils;

import java.sql.Date;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class ParseUtil {

    public static Date parseStringToDate(String date) {
        if (date == null) return null;
        // Normalize separators: allow yyyy-MM-dd or yyyy/MM/dd
        String normalized = date.replace('/', '-').trim();
        try {
            LocalDate ld = LocalDate.parse(normalized, DateTimeFormatter.ISO_LOCAL_DATE);
            return Date.valueOf(ld);
        } catch (DateTimeParseException ex) {
            // Fallback: try other common formats (e.g., yyyyMMdd)
            try {
                DateTimeFormatter alt = DateTimeFormatter.ofPattern("yyyyMMdd");
                LocalDate ld = LocalDate.parse(normalized, alt);
                return Date.valueOf(ld);
            } catch (DateTimeParseException ex2) {
                throw new RuntimeException("Invalid date format: " + date);
            }
        }
    }

}
