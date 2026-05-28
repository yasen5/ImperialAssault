package net;

import java.net.SocketException;
import java.net.UnknownHostException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

public final class NetworkConfig {
    public static final int PORT = 5050;
    public static final String SERVER_BIND_ADDRESS = "0.0.0.0";

    private NetworkConfig() {
    }

    public static String resolveMachineHostAddress() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();
                if (!networkInterface.isUp() || networkInterface.isLoopback() || networkInterface.isVirtual()) {
                    continue;
                }
                Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress address = addresses.nextElement();
                    if (address instanceof Inet4Address && !address.isLoopbackAddress() && !address.isAnyLocalAddress()) {
                        return address.getHostAddress();
                    }
                }
            }
            InetAddress localHost = InetAddress.getLocalHost();
            if (localHost != null) {
                return localHost.getHostAddress();
            }
        } catch (SocketException | UnknownHostException ex) {
            // Fall back to localhost below.
        }
        return "127.0.0.1";
    }
}
