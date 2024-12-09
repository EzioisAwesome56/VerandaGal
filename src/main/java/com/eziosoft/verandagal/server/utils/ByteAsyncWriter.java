package com.eziosoft.verandagal.server.utils;

import com.eziosoft.verandagal.server.VerandaServer;
import jakarta.servlet.AsyncContext;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class ByteAsyncWriter implements WriteListener {

    // stuff we need for later
    private ByteArrayInputStream data;
    private AsyncContext ctx;
    private ServletOutputStream out;

    public ByteAsyncWriter(byte[] content, AsyncContext ctx, ServletOutputStream out){
        // assign our variables
        this.ctx = ctx;
        this.out = out;
        this.data = new ByteArrayInputStream(content);
    }

    @Override
    public void onWritePossible() throws IOException {
        byte[] buffer = new byte[2048];
        while (this.out.isReady()){
            int len = this.data.read(buffer);
            // check if we ran out of data
            if (len < 0){
                // we're done
                this.ctx.complete();
                // close streams
                this.out.close();
                this.data.close();
                // yeet
                return;
            }
            // write out the data
            this.out.write(buffer, 0, len);
        }
    }

    @Override
    public void onError(Throwable throwable) {
        VerandaServer.LOGGER.error("Error while doing async task", throwable);
        this.ctx.complete();
    }
}
