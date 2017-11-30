package auzmor.boopal.netty;

import java.net.URI;
import java.net.URISyntaxException;

import javax.net.ssl.SSLException;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;

public class Client {

	static final String URL = System.getProperty("url", "http://localhost:8080/");

	public static void main(String[] args) throws InterruptedException, URISyntaxException, SSLException {
		URI uri = new URI(URL);
		String scheme = uri.getScheme() == null ? "http" : uri.getScheme();
		final String host = uri.getHost() == null ? "localhost" : uri.getHost();
		final int port = uri.getPort();
		/*if (port == -1) {
			if ("http".equalsIgnoreCase(scheme)) {
				port = 80;
			} else if ("https".equalsIgnoreCase(scheme)) {
				port = 443;
			}
		}*/

		/*if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
			System.err.println("Only HTTP(S) is supported.");
			return;
		}

		final boolean ssl = "https".equalsIgnoreCase(scheme);*/
		
		final boolean ssl =  System.getProperty("ssl") != null;
		final SslContext sslCtx;
		if (ssl) {
			sslCtx = SslContextBuilder.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE).build();
		} else {
			sslCtx = null;
		}
		EventLoopGroup eventLoopGroup = new NioEventLoopGroup();
		try {
			Bootstrap clientBootstrap = new Bootstrap();

			clientBootstrap.group(eventLoopGroup);
			clientBootstrap.channel(NioSocketChannel.class);
			// clientBootstrap.remoteAddress(new InetSocketAddress("localhost", 8080));
			clientBootstrap.handler(new LoggingHandler(LogLevel.INFO));
			clientBootstrap.handler(new ChannelInitializer<SocketChannel>() {
				protected void initChannel(SocketChannel socketChannel) throws Exception {
					if (sslCtx != null) {
						socketChannel.pipeline().addLast(sslCtx.newHandler(socketChannel.alloc(), host, port));
					}
					//socketChannel.pipeline().addLast("codec", new HttpServerCodec());
					//socketChannel.pipeline().addLast("aggregator", new HttpObjectAggregator(512 * 1024));
					socketChannel.pipeline().addLast(new ClientHandler());
				}
			});
			Channel channel = clientBootstrap.connect(host, port).sync().channel();
			channel.closeFuture().sync();
		} finally {
			eventLoopGroup.shutdownGracefully().sync();
		}
	}
}
