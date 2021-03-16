package netty;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.io.File;

public class FileHandler extends SimpleChannelInboundHandler<String> {
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("Client connected: " + ctx.channel().remoteAddress());
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
        System.out.println("Message: " + msg);
        String command = msg
                .replace("\r", "")
                .replace("\n", "");
        if (command.equals("list-files")) {
            File file = new File("server");
            File[] files = file.listFiles();
            StringBuffer sb = new StringBuffer();
            for (File f : files) {
                sb.append(f.getName() + "\n");
            }
            sb.append("end");
            ctx.writeAndFlush(sb.toString());
        }else {
            System.out.println("Chanel closed");
            ctx.channel().closeFuture();
            ctx.channel().close();
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("Client disconnected: " + ctx.channel().remoteAddress());
    }
}