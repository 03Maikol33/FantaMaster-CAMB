package it.camb.fantamaster.model;

import it.camb.fantamaster.util.RequestStatus;
import org.junit.Test;
import static org.junit.Assert.*;

public class RequestTest {

    @Test
    public void shouldSetStatusToAccepted_WhenFlagIsTrue() {
        // Arrange
        Request request = new Request();
        
        // Act
        request.setAccepted(true);
        
        // Assert (Verifico SOLO lo stato, principio di singola responsabilità)
        assertEquals("Se passo true, lo stato deve diventare ACCETTATA", 
                     RequestStatus.accettata, request.getRequestStatus());
    }

    @Test
    public void shouldSetStatusToRejected_WhenFlagIsFalse() {
        // Arrange
        Request request = new Request();
        
        // Act
        request.setAccepted(false);
        
        // Assert
        assertEquals("Se passo false, lo stato deve diventare RIFIUTATA", 
                     RequestStatus.rifiutata, request.getRequestStatus());
    }
    
    @Test
    public void shouldReturnTrue_WhenCheckingIsAcceptedOnAcceptedRequest() {
        // Arrange
        Request request = new Request();
        request.setRequestStatus(RequestStatus.accettata);
        
        // Act & Assert
        assertTrue("isAccepted() deve tornare true se lo stato è ACCETTATA", request.isAccepted());
    }
}