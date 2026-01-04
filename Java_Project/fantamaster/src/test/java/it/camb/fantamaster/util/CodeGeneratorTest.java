package it.camb.fantamaster.util;

import org.junit.Test;
import java.util.HashSet;
import java.util.Set;
import static org.junit.Assert.*;

public class CodeGeneratorTest {

    @Test
    public void testCodeStructure() {
        String code = CodeGenerator.generateCode();
        
        // 1. Verifica lunghezza fissa (6 come da costante nel tuo codice)
        assertNotNull(code);
        assertEquals("Il codice deve essere lungo esattamente 6 caratteri", 6, code.length());
        
        // 2. Verifica contenuto (solo Alfanumerici Maiuscoli)
        // Regex: ^[A-Z0-9]+$ significa "dall'inizio alla fine solo lettere maiuscole o numeri"
        assertTrue("Il codice deve contenere solo lettere maiuscole e numeri", 
                   code.matches("[A-Z0-9]+"));
    }

    @Test
    public void testCodeUniqueness() {
        // Generiamo un piccolo campione per verificare che non siano tutti uguali statici
        // (Testiamo la casualità del SecureRandom)
        String code1 = CodeGenerator.generateCode();
        String code2 = CodeGenerator.generateCode();
        
        assertNotEquals("Due generazioni successive dovrebbero produrre codici diversi", 
                        code1, code2);
    }
}