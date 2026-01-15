package it.camb.fantamaster.util;

import java.util.logging.Level;
import java.util.logging.Logger;

public class ErrorUtil {
    private static final Logger LOGGER = Logger.getLogger(ErrorUtil.class.getName());

    private ErrorUtil() {
        // Costruttore privato per evitare l'instanziazione
    }
    public static void log(String message, Throwable t) {
        // Sonar accetta questo perché è un logger formale
        LOGGER.log(Level.SEVERE, message, t);
    }
}