package it.camb.fantamaster.util;

import org.junit.Test;
import static org.junit.Assert.*;

public class RequestStatusTest {

    @Test
    public void testFromDbValidValues() {
        // Testiamo la conversione da Stringa DB a Enum
        assertEquals("La stringa 'in_attesa' deve essere convertita nell'enum corretto",
                RequestStatus.in_attesa, RequestStatus.fromDb("in_attesa"));
        
        assertEquals("La stringa 'accettata' deve essere convertita nell'enum corretto",
                RequestStatus.accettata, RequestStatus.fromDb("accettata"));
        
        assertEquals("La stringa 'rifiutata' deve essere convertita nell'enum corretto",
                RequestStatus.rifiutata, RequestStatus.fromDb("rifiutata"));
    }

    @Test
    public void testFromDbNullValue() {
        // Testiamo il caso limite: null
        assertNull("Se il valore dal DB è null, deve tornare null", 
                RequestStatus.fromDb(null));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFromDbInvalidValue() {
        // Testiamo che lanci eccezione se arriva una stringa sconosciuta
        RequestStatus.fromDb("STATO_INESISTENTE");
    }

    @Test
    public void testToDbConversion() {
        // Testiamo il percorso inverso: da Enum a Stringa
        assertEquals("in_attesa", RequestStatus.in_attesa.toDb());
        assertEquals("accettata", RequestStatus.accettata.toDb());
    }
}