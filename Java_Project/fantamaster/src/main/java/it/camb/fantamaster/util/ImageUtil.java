package it.camb.fantamaster.util;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Iterator;

public class ImageUtil {

    /**
     * Comprime, ritaglia a quadrato e riduce i colori di un'immagine per il DB.
     * @param imageBytes Array di byte originale
     * @param targetSize Dimensione del lato del quadrato (es. 200)
     * @param quality Qualità della compressione JPEG (da 0.0 a 1.0)
     * @return Array di byte dell'immagine ottimizzata
     */
    public static byte[] compressImage(byte[] imageBytes, int targetSize, float quality) {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(imageBytes)) {
            BufferedImage originalImage = ImageIO.read(bais);
            if (originalImage == null) return imageBytes;

            // 1. RITAGLIO QUADRATO (Square Crop dal centro)
            int width = originalImage.getWidth();
            int height = originalImage.getHeight();
            int minDim = Math.min(width, height);
            
            // Calcoliamo le coordinate per centrare il ritaglio
            int x = (width - minDim) / 2;
            int y = (height - minDim) / 2;
            
            BufferedImage croppedImage = originalImage.getSubimage(x, y, minDim, minDim);

            // 2. RIDIMENSIONAMENTO FISSO (es. 200x200)
            BufferedImage resizedImage = new BufferedImage(targetSize, targetSize, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = resizedImage.createGraphics();
            
            // Impostazioni per alta qualità di ridimensionamento
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            g.drawImage(croppedImage, 0, 0, targetSize, targetSize, null);
            g.dispose();

            // 3. RIDUZIONE COLORI (Quantizzazione)
            // Convertiamo l'immagine in un formato indicizzato (palette ridotta).
            // Nota: JPEG non supporta nativamente le palette a 20 colori, 
            // ma convertire il buffer aiuta a eliminare sfumature pesanti prima della compressione finale.
            BufferedImage lowColorImage = new BufferedImage(targetSize, targetSize, BufferedImage.TYPE_BYTE_INDEXED);
            Graphics2D g2 = lowColorImage.createGraphics();
            g2.drawImage(resizedImage, 0, 0, null);
            g2.dispose();

            // 4. COMPRESSIONE JPEG FINALE
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
            if (!writers.hasNext()) return imageBytes;
            
            ImageWriter writer = writers.next();
            try (ImageOutputStream ios = ImageIO.createImageOutputStream(baos)) {
                writer.setOutput(ios);
                ImageWriteParam param = writer.getDefaultWriteParam();
                
                if (param.canWriteCompressed()) {
                    param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                    param.setCompressionQuality(quality); // es. 0.7f per ottimo compromesso
                }

                writer.write(null, new IIOImage(lowColorImage, null, null), param);
            }
            writer.dispose();

            return baos.toByteArray();

        } catch (IOException e) {
            ErrorUtil.log("Errore compressione immagine", e);
            return imageBytes;
        }
    }
}