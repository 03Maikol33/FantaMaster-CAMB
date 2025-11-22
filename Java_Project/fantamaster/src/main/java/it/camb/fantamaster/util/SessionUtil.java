package it.camb.fantamaster.util;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

import it.camb.fantamaster.model.User;
import it.camb.fantamaster.util.Session;

public class SessionUtil {

    private static final String SESSION_FOLDER = ".sessions";

    // Crea o aggiorna la sessione
    public static void createSession(User user) {
        ensureSessionFolder();

        Session session = new Session();
        session.user = user;
        session.lastAccess = LocalDateTime.now();

        File file = getSessionFile(user.getEmail());

        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(file))) {
            out.writeObject(session);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Recupera la sessione se esiste
    public static Session loadSession(String email) {
        File file = getSessionFile(email);

        if (!file.exists()) return null;

        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(file))) {
            return (Session) in.readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    // Cancella la sessione
    public static boolean deleteSession(String email) {
        File file = getSessionFile(email);
        return file.exists() && file.delete();
    }

    // Utility: percorso file
    private static File getSessionFile(String email) {
        String safeName = email.replaceAll("[^a-zA-Z0-9]", "_");
        return new File(SESSION_FOLDER + File.separator + safeName + ".session");
    }

    // Crea la cartella se non esiste
    private static void ensureSessionFolder() {
        Path path = Path.of(SESSION_FOLDER);
        if (!Files.exists(path)) {
            try {
                Files.createDirectory(path);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
