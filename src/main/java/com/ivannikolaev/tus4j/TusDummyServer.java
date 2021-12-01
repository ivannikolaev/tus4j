package com.ivannikolaev.tus4j;

import com.ivannikolaev.tus4j.handler.TusServerHandler;
import com.ivannikolaev.tus4j.man.TusDefaultUploadManager;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;

import java.io.IOException;
import java.nio.file.Paths;

public class TusDummyServer {
    private static final String UPLOAD_FOLDER = System.getProperty("java.io.tmpdir");
    private static final String CONTEXT_ROOT = "files";
    private Channel channel;

    public void run() throws InterruptedException, IOException {
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup(2);
        try {
            ChannelFuture future = init(bossGroup, workerGroup);
            future.channel().closeFuture().sync();
        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }

    private synchronized ChannelFuture init(EventLoopGroup bossGroup, EventLoopGroup workerGroup) throws IOException, InterruptedException {
        TusDefaultUploadManager uploadManager = TusDefaultUploadManager.create(Paths.get(UPLOAD_FOLDER));
        ServerBootstrap serverBootstrap = new ServerBootstrap().group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel ch) {
                        ch.pipeline().addLast(new HttpRequestDecoder());
                        ch.pipeline().addLast(new HttpResponseEncoder());
                        ch.pipeline().addLast(new TusServerHandler(CONTEXT_ROOT, uploadManager));
                    }
                })
                .option(ChannelOption.SO_BACKLOG, 128)
                .childOption(ChannelOption.SO_KEEPALIVE, true);
        ChannelFuture future = serverBootstrap.bind(8088).sync();
        this.channel = future.channel();
        return future;
    }

    public synchronized void stop() {
        if (channel == null) {
            throw new IllegalStateException();
        }
        channel.close();
    }

    public static void main(String[] args) throws InterruptedException, IOException {
        new TusDummyServer().run();
    }

}
