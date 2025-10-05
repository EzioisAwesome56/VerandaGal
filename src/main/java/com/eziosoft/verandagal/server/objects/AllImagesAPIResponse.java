package com.eziosoft.verandagal.server.objects;

public class AllImagesAPIResponse {
    private Long[] images;
    private long page_number;
    private long max_page;

    public AllImagesAPIResponse(Long[] images, long page_number, long max_page){
        this.images = images;
        this.page_number = page_number;
        this.max_page = max_page;
    }
}
