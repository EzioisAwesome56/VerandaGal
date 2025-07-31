package com.eziosoft.verandagal.client.utils;

import com.eziosoft.verandagal.Main;
import com.luciad.imageio.webp.WebPWriteParam;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

public class ImageUtils {

    private static final Logger log = LoggerFactory.getLogger(ImageUtils.class);

    /**
     * load an image from a file, and remove its alpha transparency if any exists
     * @param file the file to load
     * @return loaded file
     */
    public static BufferedImage loadAndRemoveTransparency(File file) throws IOException{
        // disable imageio caching
        ImageIO.setUseCache(false);
        // load file
        BufferedImage tmp = ImageIO.read(file);
        // stolen from some stack overflow answer, but its pretty simple
        // uses graphics2d to remove transparency from an image
        if (tmp.getTransparency() == BufferedImage.OPAQUE)
            return tmp;
        int w = tmp.getWidth();
        int h = tmp.getHeight();
        int[] pixels = new int[w * h];
        tmp.getRGB(0, 0, w, h, pixels, 0, w);
        BufferedImage bi2 = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        bi2.setRGB(0, 0, w, h, pixels, 0, w);
        return bi2;
    }

    /**
     * normalizes troublesome images, incase they decide to be weird for some reason
     * @param in weird image
     * @return normalized image
     */
    public static BufferedImage normalizeImage(BufferedImage in){
        // create new buffered image
        BufferedImage temp = new BufferedImage(in.getWidth(), in.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D tempgfx = temp.createGraphics();
        // draw it
        tempgfx.drawImage(in, 0, 0, null);
        // clean up
        tempgfx.dispose();
        // return our normalized image
        return temp;
    }

    /**
     * tries to fix weird images by converting to jpeg also
     * @param in buffered image to convert
     * @return jpeg'd buffered image
     */
    public static BufferedImage normalizeImageviaJPEG(BufferedImage in) throws IOException{
        // run the original normalize function just to be safe
        BufferedImage temp = normalizeImage(in);
        // create a new stream to use
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        // convert to jpeg
            ImageOutputStream imgstream = ImageIO.createImageOutputStream(stream);
            ImageWriter jpg = ImageIO.getImageWritersByFormatName("jpeg").next();
            ImageWriteParam iwp = jpg.getDefaultWriteParam();
            // we dont need to set any custom params so
            jpg.setOutput(imgstream);
            jpg.write(null, new IIOImage(temp, null, null), iwp);
            jpg.dispose();
            imgstream.close();
            // get our content before closing the stream
            byte[] content = stream.toByteArray();
            // now close the stream
            stream.close();
            // read it back in using imageio
            ByteArrayInputStream input = new ByteArrayInputStream(content);
            temp = ImageIO.read(input);
            // return that
            return temp;
    }

    /**
     * Generates a webp image preview of the same resolution, but at 45% quality
     * @param img image to make a preview of
     * @return byte array of the preview
     */
    public static byte[] generateImagePreview(BufferedImage img) throws IOException{
        // disable caching
        ImageIO.setUseCache(false);
        // create output stream to write to
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        // based on https://stackoverflow.com/a/13207079
        ImageOutputStream imgstream = ImageIO.createImageOutputStream(stream);
        ImageWriter webp = ImageIO.getImageWritersByFormatName("webp").next();
        ImageWriteParam iwp = webp.getDefaultWriteParam();
        iwp.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        iwp.setCompressionType(iwp.getCompressionTypes()[WebPWriteParam.LOSSY_COMPRESSION]);
        iwp.setCompressionQuality(0.45f);
        webp.setOutput(imgstream);
        try {
            webp.write(null, new IIOImage(img, null, null), iwp);
        } catch (NullPointerException e){
            Main.LOGGER.error("Webp image preview failed to write");
            Main.LOGGER.error(e);
            return null;
        }
        webp.dispose();
        imgstream.close();
        // with everything done, convert output stream to byte[]
        byte[] content = stream.toByteArray();
        // now we can close the stream
        stream.close();
        // return content
        return content;
    }

    public static byte[] generateCustomJpeg(BufferedImage img, float quality) throws IOException{
        // disable caching
        ImageIO.setUseCache(false);
        // create output stream to write to
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        // from https://stackoverflow.com/a/13207079
        ImageOutputStream imgstream = ImageIO.createImageOutputStream(stream);
        ImageWriter jpg = ImageIO.getImageWritersByFormatName("jpeg").next();
        ImageWriteParam iwp = jpg.getDefaultWriteParam();
        // compression quality is set here
        iwp.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        iwp.setCompressionQuality(quality);
        jpg.setOutput(imgstream);
        jpg.write(null, new IIOImage(img, null, null), iwp);
        jpg.dispose();
        imgstream.close();
        // get our content before closing the stream
        byte[] content = stream.toByteArray();
        // now close and return the final product
        stream.close();
        return content;
    }

    // add extensions to force webp previews for here
    private static final String[] extensions = new String[]{"psd"};
    /**
     * this is the main check that sees if we have to ignore the option to not use webp previews
     * namely, PSD needs this
     * @param filename filename to prase to see if we need an exception
     * @return false if no, true if we need to use a webp preview image
     */
    public static boolean checkIfFormatRequiresPreview(String filename){
        // get the extension for the file we are dealing with
        String ext = FilenameUtils.getExtension(filename).toLowerCase();
        // check to see if the extension is in the list
        for (String s : extensions){
            if (s.equals(ext)){
                return true;
            }
        }
        // if we fall out of the for loop, return false
        return false;
    }
}
