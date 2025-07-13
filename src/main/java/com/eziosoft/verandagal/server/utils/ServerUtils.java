package com.eziosoft.verandagal.server.utils;

import com.eziosoft.verandagal.database.objects.Image;
import com.eziosoft.verandagal.server.objects.ItemPage;
import com.eziosoft.verandagal.server.objects.SessionObject;
import jakarta.servlet.AsyncContext;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import com.eziosoft.verandagal.server.VerandaServer;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class ServerUtils {

    // change this constant to add a new rating level
    public static int max_rating = 3;

    public static String getRatingText(int rate){
        return switch (rate) {
            case 0 -> "safe";
            case 1 -> "normal";
            case 2 -> "spicy";
            case 3 -> "extra spicy";
            default -> "Unknown rating";
        };
    }

    public static String getFileSizeMiB(File file){
        long raw_size = FileUtils.sizeOf(file);
        // do math
        float math = (float) raw_size / 1024 / 1024;
        // return that output
        return Float.toString(math) + "MiB";
    }
    public static String getFileSizeMB(File file){
        long raw_size = FileUtils.sizeOf(file);
        // do math
        float math = (float) raw_size / 1000 / 1000;
        // return that output
        return Float.toString(math) + "MB";
    }

    public static File getImageFile(String urlpath){
        return new File(FilenameUtils.concat(VerandaServer.configFile.getImageDir(), urlpath));
    }

    /**
     * math is really hard, man
     * im too stupid to figure this shit out on my own
     * so i stole this code from https://howtodoinjava.com/java/array/split-arrays/
     * @param numitems how many items you want in each array
     * @param input the list you want to split
     * @return
     */
    public static List<Object[]> splitList(int numitems, List<Object> input){
        int numberOfArrays = input.size() / numitems;
        int remainder = input.size() % numitems;

        // dumb fucking hack because i am a lazy piece of shit
        // make the input into a []
        // copyofrange does not accept list as an argument lmao
        Object[] crack = input.toArray(new Object[0]);

        int start = 0;
        int end = 0;

        List<Object[]> list = new ArrayList<Object[]>();
        for (int i = 0; i < numberOfArrays; i++) {
            end += numitems;
            list.add(Arrays.copyOfRange(crack, start, end));
            start = end;
        }

        if(remainder > 0) {
            list.add(Arrays.copyOfRange(crack, start, (start + remainder)));
        }
        return list;
    }

    /**
     * This gets ran near the start of the function to see if the user's view settings
     * state that this image should be filtered
     * if it does get filtered, return true
     * @param req http request
     * @param img the image we are trying to view
     * @return true if it got filtered
     */
    public static boolean checkForFiltering(HttpServletRequest req, Image img){
        VerandaServer.LOGGER.debug("Now running filter check routine");
        // get the rating information about the image
        int rating = img.getRating();
        boolean ai = img.isAI();
        // get the session for the user
        HttpSession sesh = req.getSession();
        SessionObject session = SessionUtils.getSessionDetails(sesh);
        // because of how the filter flow is set, the AI filter has to come first
        if (ai){
            // check too see if the ai filter is set or not
            if (!session.isShow_ai()){
                // ai filter is enabled
                // return true
                return true;
            }
        }
        // switch case to handle stuff
        return switch (rating) {
            case 0 ->
                // safe images cannot be disabled
                    false;
            case 1 -> !session.isShow_normal();
            case 2 -> !session.isShow_spicy();
            case 3 -> !session.isShow_extra_spicy();
            default -> {
                VerandaServer.LOGGER.warn("Somehow, the default case in the filter tree was reached");
                yield false;
            }
        };
    }

    /**
     * this code is used like 3 or 4 times in seperate places but does basically the same thing
     * so im moving it here to reduce code copy pasta
     * @param req http request
     * @param resp http response
     * @param template name of template to load
     * @throws IOException if somehing breaks
     */
    public static void handleInvalidRequest(HttpServletRequest req, HttpServletResponse resp, String template) throws IOException {
        // setup a string builder
        StringBuilder b = new StringBuilder();
        try {
            // get the header
            b.append(VerandaServer.template.getTemplate("header"));
            // sidebar
            b.append(VerandaServer.sidebarBuilder.getSideBar());
            // load the invalid image text
            b.append(VerandaServer.template.getTemplate(template));
            // append the footer
            b.append(VerandaServer.template.getTemplate("footer"));
        } catch (Exception e){
            // give up and explode
            AsyncContext cxt = req.startAsync();
            ServletOutputStream out = resp.getOutputStream();
            out.setWriteListener(new BasicTextWriter("something broke", cxt, out));
            return;
        }
        // send the result down the tube
        AsyncContext cxt = req.startAsync();
        ServletOutputStream out = resp.getOutputStream();
        out.setWriteListener(new BasicTextWriter(b.toString(), cxt, out));
    }

    /**
     * generates a gallery of thumbnails based on the provided image ids
     * builds it as html table elements
     * @param req http servlet request, used for filtering
     * @param raw_imageids the image ids of which you want to display
     * @param items_per_row how many items per row you want
     * @return hashmap; 0 is the built table as string, 1 is how many items got filtered
     * @throws IOException if something breaks
     */
    public static HashMap<Integer, Object> buildThumbnailGallery(HttpServletRequest req, Long[] raw_imageids, int items_per_row) throws IOException {
        // setup the hashmap that we will be returning the values
        HashMap<Integer, Object> output = new HashMap<>();
        // filter code directly stolen from gallery view
        List<Object> filtered = new ArrayList<>();
        // for funsies, count how  many items got filtered
        int filter_count = 0;
        for (long image_id : raw_imageids){
            // because pagination exists, check to see if negative 1
            if (image_id == -1){
                // if so, skip it
                continue;
            }
            // load the image from the database
            Image tmpimage = VerandaServer.maindb.LoadObject(Image.class, image_id);
            if (tmpimage == null){
                // this isnt supposed to happen, but we have to handle it
                // just in case
                VerandaServer.LOGGER.warn("Image ID {} somehow doesnt exist yet it was present in input list", image_id);
                VerandaServer.LOGGER.warn("This may be a sign of larger problems, but not something we will fix");
                // just move on to the next one
                continue;
            }
            // check to see if it is filtered or not
            boolean is_filtered = ServerUtils.checkForFiltering(req, tmpimage);
            if (!is_filtered){
                // if it did not get filtered, add it to the new list
                filtered.add(image_id);
            } else {
                // add 1 to the count
                filter_count++;
            }
        }
        // NEW FEATURE: customizable items per row!
        // variable for later
        int real_itemsrow = items_per_row;
        // get the session of the user
        HttpSession httpsession = req.getSession();
        SessionObject sesh = SessionUtils.getSessionDetails(httpsession);
        // check if its not equal to the default
        if (sesh.getItemsperrow() != items_per_row){
            // update the value with whatever is stored
            real_itemsrow = sesh.getItemsperrow();
        }
        // more stolen code for building the gallery view
        List<Object[]> splitobjs = ServerUtils.splitList(real_itemsrow, filtered);
        // loop thru this
        StringBuilder tablebuilder = new StringBuilder();
        for (Object[] array : splitobjs){
            // print a table start element
            tablebuilder.append("<tr>");
            // convert this shit to long array
            // loop thru this
            for (Object rawobj : array){
                // convert that shit to a long
                long imgid = (long) rawobj;
                // load the basic gallery entry
                String base = VerandaServer.template.getTemplate("galent");
                // replace the content with what we need
                base = base.replace("${THUMBURL}", "/thumb/?id=" + imgid);
                base = base.replace("${IMGID}", Long.toString(imgid));
                // append to string builder
                tablebuilder.append(base);
            }
            // put a closing tr element
            tablebuilder.append("</tr>");
        }
        // ok, the main function has finished
        // now build the output
        output.put(0, tablebuilder.toString());
        output.put(1, filter_count);
        return output;
    }

    /**
     * general-purpose function to easily create the navigation at the bottom of a page, if required
     * @param page the current page object
     * @param cur_page current page
     * @return navigation
     */
    public static String buildNavigation(ItemPage page, int cur_page, String cur_url){
        // create a new stringbuilder to work with
        StringBuilder nav = new StringBuilder();
        // first part: build previous and next page if required
        if (cur_page > 0){
            nav.append("<td><a href=\"").append(cur_url).append("&p=").append(cur_page - 1).append("\">Previous page</a></td>");
            nav.append("<td>|</td>");
        }
        if (cur_page < page.getTotal_pages() - 1){
            nav.append("<td><a href=\"").append(cur_url).append("&p=").append(cur_page + 1).append("\">Next Page</a></td>");
            nav.append("<td>|</td>");
        }
        // first and last page buttons
        nav.append("<td><a href=\"").append(cur_url).append("&p=0").append("\">First page</a></td>").append("<td>|</td>");
        nav.append("<td><a href=\"").append(cur_url).append("&p=").append(page.getTotal_pages() - 1).append("\">Last page</a></td>");
        // return it
        return nav.toString();
    }
}
