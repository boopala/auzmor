package auzmor.boopal.netty;

import java.security.cert.CertificateException;

import javax.net.ssl.SSLException;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;

public class Server {

	static final boolean SSL = System.getProperty("ssl") != null;
	static final int PORT = Integer.parseInt(System.getProperty("port", SSL ? "8443" : "8080"));

	public static void main(String[] args) throws InterruptedException, CertificateException, SSLException {
		final SslContext sslCtx;
		if (SSL) {
			SelfSignedCertificate ssc = new SelfSignedCertificate();
			sslCtx = SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey()).build();
		} else {
			sslCtx = null;
		}

		EventLoopGroup eventLoopGroupMaster = new NioEventLoopGroup();
		EventLoopGroup eventLoopGroupSlave = new NioEventLoopGroup();
		try {
			ServerBootstrap serverBootStrap = new ServerBootstrap();
			serverBootStrap.group(eventLoopGroupMaster, eventLoopGroupSlave);
			serverBootStrap.channel(NioServerSocketChannel.class);
			serverBootStrap.handler(new LoggingHandler(LogLevel.INFO));
			//serverBootStrap.localAddress("localhost", 8080);

			serverBootStrap.childHandler(new ChannelInitializer<SocketChannel>() {

				protected void initChannel(SocketChannel socketChannel) throws Exception {
					if(sslCtx != null) {
						socketChannel.pipeline().addLast(sslCtx.newHandler(socketChannel.alloc()));
					}
					//socketChannel.pipeline().addLast("codec", new HttpServerCodec());
					//socketChannel.pipeline().addLast("aggregator", new HttpObjectAggregator(512 * 1024));
					socketChannel.pipeline().addLast(new ServerHandler());
				}
			});

			Channel channel = serverBootStrap.bind(PORT).sync().channel();
			channel.closeFuture().sync();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			eventLoopGroupMaster.shutdownGracefully().sync();
			eventLoopGroupSlave.shutdownGracefully().sync();
		}
	}
}
