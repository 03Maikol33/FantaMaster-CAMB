
package it.camb.fantamaster.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;

import org.junit.Test;

public class RequestStatusTest {

    // 1. Verifica della Conversione Bidirezionale
    @Test
    public void testToDbAndFromDbConsistency() {
        // Test 1: in_attesa
        String dbStatus1 = RequestStatus.in_attesa.toDb();
        assertEquals("in_attesa", dbStatus1);
        assertEquals(RequestStatus.in_attesa, RequestStatus.fromDb(dbStatus1));

        // Test 2: accettata
        String dbStatus2 = RequestStatus.accettata.toDb();
        assertEquals("accettata", dbStatus2);
        assertEquals(RequestStatus.accettata, RequestStatus.fromDb(dbStatus2));

        // Test 3: rifiutata
        String dbStatus3 = RequestStatus.rifiutata.toDb();
        assertEquals("rifiutata", dbStatus3);
        assertEquals(RequestStatus.rifiutata, RequestStatus.fromDb(dbStatus3));
    }

    // 2. Verifica dei casi limite in fromDb()
    @Test
    public void testFromDbNullInput() {
        assertNull(RequestStatus.fromDb(null));
    }

    @Test
    public void testFromDbInvalidInputThrowsException() {
        // Test stringa sconosciuta
        assertThrows(IllegalArgumentException.class, () -> {
            RequestStatus.fromDb("sconosciuto");
        });

        // Test stringa in caso errato (le ENUM SQL sono spesso case-sensitive)
        assertThrows(IllegalArgumentException.class, () -> {
            RequestStatus.fromDb("In_attesa");
        });
    }

    // Nota: toDb() non richiede test per input non validi, perché il metodo
    // è chiamato sull'istanza Enum, che è sempre valida.
}