package ir.alamoot.plus;

import android.content.Context;
import android.net.wifi.WifiManager;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** WS-Discovery based ONVIF camera discovery (UDP multicast 239.255.255.250:3702). */
public class OnvifDiscovery {

    public interface Listener { void onDeviceFound(String name, String host); }

    private static final String PROBE =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
        "<e:Envelope xmlns:e=\"http://www.w3.org/2003/05/soap-envelope\" " +
        "xmlns:w=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\" " +
        "xmlns:d=\"http://schemas.xmlsoap.org/ws/2005/04/discovery\" " +
        "xmlns:dn=\"http://www.onvif.org/ver10/network/wsdl\">" +
        "<e:Header><w:MessageID>uuid:%s</w:MessageID>" +
        "<w:To e:mustUnderstand=\"true\">urn:schemas-xmlsoap-org:ws:2005:04:discovery</w:To>" +
        "<w:Action e:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2005/04/discovery/Probe</w:Action>" +
        "</e:Header><e:Body><d:Probe><d:Types>dn:NetworkVideoTransmitter</d:Types></d:Probe></e:Body></e:Envelope>";

    private final Context ctx;
    private final Listener listener;
    private final ExecutorService pool = Executors.newSingleThreadExecutor();
    private volatile boolean running;

    public OnvifDiscovery(Context ctx, Listener listener) {
        this.ctx = ctx.getApplicationContext();
        this.listener = listener;
    }

    public void start(int timeoutMs) {
        running = true;
        pool.execute(() -> discover(timeoutMs));
    }

    public void stop() { running = false; pool.shutdownNow(); }

    private void discover(int timeoutMs) {
        WifiManager wm = (WifiManager) ctx.getSystemService(Context.WIFI_SERVICE);
        WifiManager.MulticastLock lock = null;
        if (wm != null) lock = wm.createMulticastLock("onvif");
        if (lock != null) lock.acquire();
        try (MulticastSocket sock = new MulticastSocket()) {
            InetAddress group = InetAddress.getByName("239.255.255.250");
            sock.setSoTimeout(500);
            sock.joinGroup(group);
            byte[] probe = String.format(PROBE, UUID.randomUUID()).getBytes(StandardCharsets.UTF_8);
            sock.send(new DatagramPacket(probe, probe.length, group, 3702));

            long deadline = System.currentTimeMillis() + timeoutMs;
            byte[] buf = new byte[8192];
            while (running && System.currentTimeMillis() < deadline) {
                DatagramPacket p = new DatagramPacket(buf, buf.length);
                try { sock.receive(p); } catch (SocketTimeoutException e) { continue; }
                String resp = new String(p.getData(), 0, p.getLength(), StandardCharsets.UTF_8);
                String host = p.getAddress().getHostAddress();
                String name = extract(resp, "<d:Types>", "</d:Types>");
                if (resp.contains("NetworkVideoTransmitter")) {
                    listener.onDeviceFound("ONVIF Camera", host);
                } else if (name != null) {
                    listener.onDeviceFound("Device", host);
                }
            }
        } catch (Exception ignored) {
        } finally {
            if (lock != null) { try { lock.release(); } catch (Exception ignored) {} }
        }
    }

    private static String extract(String s, String a, String b) {
        int i = s.indexOf(a);
        if (i < 0) return null;
        int j = s.indexOf(b, i + a.length());
        return j < 0 ? null : s.substring(i + a.length(), j);
    }
}
