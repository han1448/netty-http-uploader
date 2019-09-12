package com.oppa.woodpecker;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;

public class HttpFileServer {

    // port which listens from client
    private static int port = 8080;

    private static String path = "/usr/local/download/";

    public HttpFileServer(int port) {
        this.port = port;
    }

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.out.println("Please input port and path. ex) 8080 /usr/local/download/");
        }
        if (args.length > 0) {
            port = Integer.parseInt(args[0]);
        }

        path = args[1];

        new HttpFileServer(port).run();
    }

    public void run() throws Exception {
        //accepts an incoming connection
        EventLoopGroup bossGroup = new NioEventLoopGroup();

        // handles the traffic of the accepted connection
        EventLoopGroup workerGroup = new NioEventLoopGroup();

        try {

            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)

                    //specify to use the NioServerSocketChannel class
                    .channel(NioServerSocketChannel.class)

                    // help a user configure
                    .childHandler(new ChannelInitializer<SocketChannel>() {

                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ch.pipeline()
                                    .addLast(new HttpRequestDecoder())
                                    .addLast(new HttpResponseEncoder())
                                    .addLast(new HttpFileServerHandler(path));
                        }
                    })

                    // set the parameters (incoming connection - NioServerSocketChannel)
                    .option(ChannelOption.SO_BACKLOG, 128)

                    // set for the Channels accepted by the parent ServerChannel
                    .childOption(ChannelOption.SO_KEEPALIVE, true);

            // Bind and start to accept incoming connections.
            ChannelFuture f = b.bind(port).sync();

            // Wait until the server socket is closed.
            // In this example, this does not happen, but you can do that to gracefully
            // shut down your server.
            f.channel().closeFuture().sync();

        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }
}
