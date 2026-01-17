package it.camb.fantamaster.util;

import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class PasswordUtilTest {
    
    // Verifica che l'hash della password venga generato correttamente.
    @Test
    public void testHashIsGenerated() {
        String password = "Password1234!";
        String hash = PasswordUtil.hashPassword(password);
        
        // 1. Deve essere generato
        assertNotNull(hash, "L'hash della password non deve essere null");
    }

    // Verifica che l'hash non sia uguale alla password in chiaro.
    @Test
    public void testHashIsNotEqualToPassword() {
        String password = "Password1234!";
        String hash = PasswordUtil.hashPassword(password);
        
        // L'hash non deve essere uguale alla password in chiaro
        assertNotEquals(password, hash, "L'hash non deve essere uguale alla password in chiaro");
    }

    // Verifica che due hash della stessa password siano diversi grazie al salt.
    @Test
    public void testHashesAreDifferentForSamePassword() {
        String password = "Password1234!";
        String hash1 = PasswordUtil.hashPassword(password);
        String hash2 = PasswordUtil.hashPassword(password);
        
        // Il sale (salt) rende l'hash diverso ogni volta
        assertNotEquals(hash1, hash2, "Gli hash dovrebbero essere diversi per la stessa password a causa del sale (salt)");
    }

    // Verifica che la password venga verificata correttamente contro un hash.
    @Test
    public void testPasswordVerification() {
        String password = "Password1234!";
        String hash = PasswordUtil.hashPassword(password);
        
        // La password dovrebbe essere verificata correttamente
        assertTrue(PasswordUtil.checkPassword(password, hash));
    }
}