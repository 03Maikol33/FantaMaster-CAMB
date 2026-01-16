package it.camb.fantamaster.util;

import org.junit.Test;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.Assert.*;

public class ImageUtilTest {

    /**
     * Helper per generare un'immagine di test in memoria.
     * Crea un rettangolo colorato per testare il ritaglio.
     */
    private byte[] generateTestImage(int width, int height) throws IOException {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.RED);
        g.fillRect(0, 0, width, height);
        g.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "png", baos);
        return baos.toByteArray();
    }

    @Test
    public void testCompressImageSuccess() throws IOException {
        // Creiamo un'immagine rettangolare 400x200
        byte[] original = generateTestImage(400, 200);
        
        // Eseguiamo la compressione a 100x100
        byte[] compressed = ImageUtil.compressImage(original, 100, 0.7f);
        
        assertNotNull(compressed);
        assertTrue("L'immagine compressa dovrebbe avere una dimensione diversa", compressed.length > 0);
        
        // Opzionale: verifichiamo che l'output sia effettivamente un'immagine leggibile
        java.io.ByteArrayInputStream bais = new java.io.ByteArrayInputStream(compressed);
        BufferedImage resultImg = ImageIO.read(bais);
        assertNotNull(resultImg);
        assertEquals(100, resultImg.getWidth());
        assertEquals(100, resultImg.getHeight());
    }

    @Test
    public void testCompressImageWithInvalidBytes() {
        // Passiamo array di byte casuali che non sono un'immagine
        byte[] invalidData = new byte[]{0, 1, 2, 3, 4, 5};
        
        byte[] result = ImageUtil.compressImage(invalidData, 100, 0.5f);
        
        // La logica dice che se l'immagine è null, ritorna i byte originali
        assertArrayEquals(invalidData, result);
    }

    @Test
    public void testCompressImageVerySmall() throws IOException {
        // Testiamo il ridimensionamento verso l'alto o con qualità bassissima
        byte[] original = generateTestImage(10, 10);
        byte[] result = ImageUtil.compressImage(original, 50, 0.1f);
        
        assertNotNull(result);
        // Verifichiamo che non crashi con dimensioni ridotte
    }
}