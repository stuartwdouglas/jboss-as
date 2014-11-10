/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.wildfly.extension.undertow;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import javax.net.ssl.SSLContext;

import io.undertow.UndertowOptions;
import io.undertow.server.HttpHandler;
import io.undertow.server.OpenListener;
import io.undertow.server.protocol.http.AlpnOpenListener;
import io.undertow.server.protocol.http.HttpOpenListener;
import io.undertow.server.protocol.spdy.SpdyOpenListener;
import org.jboss.as.domain.management.SecurityRealm;
import org.jboss.msc.value.InjectedValue;
import org.xnio.BufferAllocator;
import org.xnio.ByteBufferSlicePool;
import org.xnio.ChannelListener;
import org.xnio.IoUtils;
import org.xnio.OptionMap;
import org.xnio.OptionMap.Builder;
import org.xnio.Options;
import org.xnio.Pool;
import org.xnio.StreamConnection;
import org.xnio.XnioWorker;
import org.xnio.channels.AcceptingChannel;
import org.xnio.ssl.JsseXnioSsl;
import org.xnio.ssl.SslConnection;
import org.xnio.ssl.XnioSsl;

/**
 * An extension of {@see HttpListenerService} to add SSL.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 * @author Tomaz Cerar
 */
public class HttpsListenerService extends HttpListenerService {

    private final InjectedValue<SecurityRealm> securityRealm = new InjectedValue<>();
    private volatile AcceptingChannel<SslConnection> sslServer;
    static final String PROTOCOL = "https";

    public HttpsListenerService(final String name, String serverName, OptionMap listenerOptions, OptionMap socketOptions) {
        super(name, serverName, listenerOptions, socketOptions, false, false);
    }

    @Override
    protected void startListening(XnioWorker worker, InetSocketAddress socketAddress, ChannelListener<AcceptingChannel<StreamConnection>> acceptListener) throws IOException {

        SSLContext sslContext = securityRealm.getValue().getSSLContext();
        Builder builder = OptionMap.builder().addAll(commonOptions);
        builder.addAll(socketOptions);
        builder.set(Options.USE_DIRECT_BUFFERS, true);
        OptionMap combined = builder.getMap();

        XnioSsl xnioSsl = new JsseXnioSsl(worker.getXnio(), combined, sslContext);
        sslServer = xnioSsl.createSslConnectionServer(worker, socketAddress, (ChannelListener) acceptListener, combined);
        sslServer.resumeAccepts();

        UndertowLogger.ROOT_LOGGER.listenerStarted("HTTPS", getName(), socketAddress);
    }


    @Override
    protected OpenListener createOpenListener() {

        boolean spdy = getListenerOptions().get(UndertowOptions.ENABLE_SPDY, false);
        HttpOpenListener httpOpenListener = new HttpOpenListener(getBufferPool().getValue(), OptionMap.builder().addAll(commonOptions).addAll(listenerOptions).getMap());
        if(!spdy) {
            return httpOpenListener;
        }
        try {
            getClass().getClassLoader().loadClass("org.eclipse.jetty.alpn.ALPN");
        } catch (ClassNotFoundException e) {
            UndertowLogger.ROOT_LOGGER.noAlpn();
            return httpOpenListener;
        }
        final List<OpenListener> listeners = new ArrayList<>();
        listeners.add(httpOpenListener);
        final AlpnOpenListener alpn = new AlpnOpenListener(getBufferPool().getValue(), "http/1.1", httpOpenListener);
        if(spdy) {
            //TODO: non-direct buffer pool should be configurable
            SpdyOpenListener spdyOpen = new SpdyOpenListener(getBufferPool().getValue(), new ByteBufferSlicePool(BufferAllocator.BYTE_BUFFER_ALLOCATOR, 1000, 1000), OptionMap.builder().addAll(commonOptions).addAll(listenerOptions).getMap());
            listeners.add(spdyOpen);
            alpn.addProtocol(SpdyOpenListener.SPDY_3_1, spdyOpen, 5);
        }
        return new OpenListener() {


            @Override
            public HttpHandler getRootHandler() {
                return listeners.get(0).getRootHandler();
            }

            @Override
            public void setRootHandler(HttpHandler rootHandler) {
                for(OpenListener l : listeners) {
                    l.setRootHandler(rootHandler);
                }
            }

            @Override
            public OptionMap getUndertowOptions() {
                return listeners.get(0).getUndertowOptions();
            }

            @Override
            public void setUndertowOptions(OptionMap undertowOptions) {
                for(OpenListener l : listeners) {
                    l.setUndertowOptions(undertowOptions);
                }
            }

            @Override
            public Pool<ByteBuffer> getBufferPool() {
                return bufferPool.getValue();
            }

            @Override
            public void handleEvent(StreamConnection channel) {
                alpn.handleEvent(channel);
            }
        };
    }

    @Override
    public boolean isSecure() {
        return true;
    }

    @Override
    protected void stopListening() {
        sslServer.suspendAccepts();
        UndertowLogger.ROOT_LOGGER.listenerSuspend("HTTPS", getName());
        IoUtils.safeClose(sslServer);
        sslServer = null;
        UndertowLogger.ROOT_LOGGER.listenerStopped("HTTPS", getName(), getBinding().getValue().getSocketAddress());
        httpListenerRegistry.getValue().removeListener(getName());
    }

    public InjectedValue<SecurityRealm> getSecurityRealm() {
        return securityRealm;
    }

    @Override
    protected String getProtocol() {
        return PROTOCOL;
    }

}
