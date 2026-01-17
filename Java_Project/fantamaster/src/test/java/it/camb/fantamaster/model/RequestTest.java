package it.camb.fantamaster.model;

import it.camb.fantamaster.util.RequestStatus;
import org.junit.Test;
import static org.junit.Assert.*;

public class RequestTest {

    // Verifica che impostare accepted=true cambii lo stato a ACCETTATA.
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

    // Verifica che impostare accepted=false cambii lo stato a RIFIUTATA.
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
    
    // Verifica che isAccepted() ritorni true quando lo stato è ACCETTATA.
    @Test
    public void shouldReturnTrue_WhenCheckingIsAcceptedOnAcceptedRequest() {
        // Arrange
        Request request = new Request();
        request.setRequestStatus(RequestStatus.accettata);
        
        // Act & Assert
        assertTrue("isAccepted() deve tornare true se lo stato è ACCETTATA", request.isAccepted());
    }
}