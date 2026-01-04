package it.camb.fantamaster.model;

import org.junit.Test;
import java.time.LocalDateTime;
import static org.junit.Assert.*;

public class MessageTest {

    @Test
    public void shouldFormatTimeAsHourMinute() {
        // Arrange
        Message message = new Message();
        // Fisso una data specifica: 2023-10-05 alle 14:07
        LocalDateTime fixedTime = LocalDateTime.of(2023, 10, 5, 14, 7); 
        message.setTimestamp(fixedTime);

        // Act
        String result = message.getFormattedTime();

        // Assert
        assertEquals("Il formato deve essere HH:mm", "14:07", result);
    }

    @Test
    public void shouldReturnEmptyString_WhenTimestampIsNull() {
        // Arrange
        Message message = new Message();
        message.setTimestamp(null);

        // Act
        String result = message.getFormattedTime();

        // Assert
        assertEquals("Se il timestamp è null, deve tornare stringa vuota per non rompere la UI", 
                     "", result);
    }
}