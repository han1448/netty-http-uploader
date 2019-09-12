package com.oppa.woodpecker;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.multipart.*;
import io.netty.util.CharsetUtil;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

public class HttpFileServerHandler extends SimpleChannelInboundHandler<HttpObject> {

    static {
        DiskFileUpload.deleteOnExitTemporaryFile = true;
        DiskAttribute.deleteOnExitTemporaryFile = true;
    }

    private String path;
    private File dir;
    private HttpPostRequestDecoder httpDecoder;

    HttpFileServerHandler(String path) {
        this.dir = new File(path);
        this.path = path;
    }

    private static void sendResponse(ChannelHandlerContext ctx, HttpResponseStatus status, String message) {
        final FullHttpResponse response;
        String msgDesc = message;
        if (message == null) {
            msgDesc = "Failure: " + status;
        }
        msgDesc += " \r\n";

        final ByteBuf buffer = Unpooled.copiedBuffer(msgDesc, CharsetUtil.UTF_8);
        if (status.code() >= HttpResponseStatus.BAD_REQUEST.code()) {
            response = new DefaultFullHttpResponse(HTTP_1_1, status, buffer);
        } else {
            response = new DefaultFullHttpResponse(HTTP_1_1, status, buffer);
        }
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");

        // Close the connection as soon as the response is sent.
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }

    @Override
    public void channelActive(final ChannelHandlerContext ctx) {
        System.out.println("Connected.");
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, HttpObject msg) throws Exception {
        if (msg instanceof HttpRequest) {
            HttpRequest request = (HttpRequest) msg;
            httpDecoder = new HttpPostRequestDecoder(request);
        } else if (msg instanceof HttpContent) {
            HttpContent httpContent = (HttpContent) msg;
            httpDecoder.offer(httpContent);
            while (httpDecoder.hasNext()) {
                InterfaceHttpData chunk = httpDecoder.next();
                if (Objects.isNull(chunk)) {
                    break;
                }

                if (chunk.getHttpDataType() == InterfaceHttpData.HttpDataType.FileUpload) {
                    final FileUpload fileUpload = (FileUpload) chunk;
                    if (fileUpload.isCompleted()) {
                        File file = createFile(fileUpload);
                        fileUpload.renameTo(file);
                        System.out.println("Received : " + fileUpload.get().length);

                    }
                }
            }

            if (httpContent instanceof LastHttpContent) {
                finalize(ctx);
            }

        } else {
            System.out.println(msg.toString());
            sendResponse(ctx, HttpResponseStatus.NOT_ACCEPTABLE, "try again.");
            finalize(ctx);
        }
    }

    private void finalize(ChannelHandlerContext ctx) {
        httpDecoder.destroy();
        httpDecoder = null;
        sendResponse(ctx, HttpResponseStatus.OK, "good bye.");
        System.out.println("Upload completed");
    }

    private File createFile(FileUpload fileUpload) throws IOException {
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                System.err.println("fail to create dir : " + dir);
            }
        }

        final File file = new File(path + fileUpload.getFilename());
        if (!file.exists()) {
            if (!file.createNewFile()) {
                System.err.println("fail to create file : " + file);
            }
        }
        return file;
    }
}
