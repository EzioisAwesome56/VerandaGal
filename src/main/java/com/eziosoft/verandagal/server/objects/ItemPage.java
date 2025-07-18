package com.eziosoft.verandagal.server.objects;


import com.eziosoft.verandagal.server.utils.SessionUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

public class ItemPage {
    /**
     * before any objects are displayed on a page, you need to get one of these objects
     * they handle generating the pages from just settings and list of ids
     */
    private Long[] item_ids;
    private int current_page;
    private int total_pages;
    // not returnable by this object, but will be used later
    private final Long[] source;
    private final HttpServletRequest req;
    private boolean generated = false;

    public ItemPage(Long[] source, HttpServletRequest req){
        // store the variables we absolutely need
        this.source = source;
        this.req = req;
        this.current_page = 0;
    }

    /**
     * call this to set the current page the user is viewing
     * @param page page number that the user is currently on
     */
    public void setCurrentPage(int page){
        this.current_page = page;
    }

    /**
     * you are REQUIRED to use this before you get the page content or you will explode
     */
    public void generatePage(){
        // get the current user's session
        HttpSession httpsession = this.req.getSession();
        SessionObject sesh = SessionUtils.getSessionDetails(httpsession);
        // check to see if pagination is turned on
        if (sesh.isUse_pagination()){
            this.PageGeneration(sesh.getItems_per_page());
        } else {
            // litterally do nothing and set the thing to null
            this.item_ids = null;
            this.total_pages = 1;
        }
        // set our flag
        this.generated = true;
    }

    /**
     * get the list of ids to display
     * @return page of content
     */
    public Long[] getPageContents(){
        // check to make sure they actually did it
        if (!this.generated){
            throw new NullPointerException("Page Content not generated!");
        }
        // otherwise, return the list of ids
        if (this.item_ids != null){
            return this.item_ids;
        }
        return this.source;
    }

    public int getTotal_pages(){
        return this.total_pages;
    }

    /**
     * internal routine called to generate the actual page. mostly here for completeness
     * @param itemsperpage how many items to show per page
     */
    private void PageGeneration(int itemsperpage){
        // also calculate how many pages we have
        this.total_pages = Math.ceilDiv(this.source.length, itemsperpage);
        // bounds checking
        if (this.current_page > this.total_pages - 1){
            // set the current page to total pages - 1
            this.current_page = this.total_pages - 1;
        }
        // calculate how far into the array we need to skip based on page number
        int skip_index = this.current_page * itemsperpage;
        // create an array of the size of items per page
        this.item_ids = new Long[itemsperpage];
        // populate it
        for (int i = 0; i < itemsperpage; i++){
            try {
                this.item_ids[i] = this.source[i + skip_index];
            } catch (ArrayIndexOutOfBoundsException e){
                // ran out of items, set to -1
                this.item_ids[i] = -1L;
            }
        }
        // we're done now i think

    }
}
