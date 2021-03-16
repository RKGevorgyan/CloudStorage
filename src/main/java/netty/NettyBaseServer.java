package netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;

public class NettyBaseServer {
    public NettyBaseServer() {
        EventLoopGroup auth = new NioEventLoopGroup(1);
        EventLoopGroup workers = new NioEventLoopGroup();
        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(auth, workers)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch) throws Exception {
                            ch.pipeline().addLast(
                                    new StringDecoder(), // in-1
                                    new StringEncoder(), // out-1
                                    new FileHandler() // in-2
                            );
                        }
                    });
            ChannelFuture future = bootstrap.bind(1234).sync();
            System.out.println("Server started");
            future.channel().closeFuture().sync();
            System.out.println("Server finished");

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            auth.shutdownGracefully();
            workers.shutdownGracefully();
        }
    }

    public static void main(String[] args) {
        new NettyBaseServer();
    }
}
