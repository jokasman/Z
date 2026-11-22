package pt.setorzero.core;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Ligação entre dois telemóveis na mesma rede local (Wi-Fi ou ponto de acesso).
 * O anfitrião anuncia o jogo por difusão UDP e aceita uma ligação TCP; as mensagens são blocos
 * de bytes com o tamanho à frente. Toda a rede corre em fios próprios: o jogo só usa filas.
 */
public final class Net {
    public static final int PORT = 47321, DISC_PORT = 47322, PROTO = 1;
    private static final String TAG = "SETORZERO";

    /** A plataforma pode precisar de autorizar a receção de difusões (Android: MulticastLock). */
    public interface Hook { void listening(boolean on); }
    public static volatile Hook hook;

    private volatile Socket sock;
    private volatile ServerSocket server;
    private volatile DatagramSocket disc;
    private final ConcurrentLinkedQueue<byte[]> inbox = new ConcurrentLinkedQueue<>();
    private final LinkedBlockingQueue<byte[]> outbox = new LinkedBlockingQueue<>();
    private final Map<String, String[]> found = Collections.synchronizedMap(new LinkedHashMap<String, String[]>());
    public volatile boolean connected, closed;
    public volatile String error;
    private volatile long lastRecv;

    // ---------------------------------------------------------------- anfitrião

    /** Abre o jogo e anuncia-o na rede. info: texto curto mostrado a quem procura (ex.: nome do nível). */
    public void host(final String info) {
        new Thread(new Runnable() {
            public void run() {
                try {
                    ServerSocket ss = new ServerSocket();
                    ss.setReuseAddress(true);
                    ss.bind(new InetSocketAddress(PORT));
                    server = ss;
                    startBeacon(info);
                    Socket s = ss.accept();
                    try { ss.close(); } catch (Exception ignored) { }
                    server = null;
                    attach(s);
                } catch (Exception e) {
                    if (!closed) error = "Não foi possível abrir o jogo na rede.";
                }
            }
        }, "rede-anfitriao").start();
    }

    private void startBeacon(final String info) {
        new Thread(new Runnable() {
            public void run() {
                DatagramSocket ds = null;
                try {
                    ds = new DatagramSocket();
                    ds.setBroadcast(true);
                    byte[] msg = (TAG + "|" + PROTO + "|" + info).getBytes("UTF-8");
                    while (!closed && !connected) {
                        for (InetAddress a : broadcastAddresses()) {
                            try { ds.send(new DatagramPacket(msg, msg.length, a, DISC_PORT)); } catch (Exception ignored) { }
                        }
                        Thread.sleep(800);
                    }
                } catch (Exception ignored) {
                    // sem anúncio: o outro jogador pode ligar pelo endereço
                } finally {
                    if (ds != null) ds.close();
                }
            }
        }, "rede-anuncio").start();
    }

    // ---------------------------------------------------------------- convidado

    /** Procura jogos anunciados na rede. */
    public void discover() {
        new Thread(new Runnable() {
            public void run() {
                Hook h = hook;
                if (h != null) h.listening(true);
                try {
                    DatagramSocket ds = new DatagramSocket(null);
                    ds.setReuseAddress(true);
                    ds.setBroadcast(true);
                    ds.bind(new InetSocketAddress(DISC_PORT));
                    ds.setSoTimeout(500);
                    disc = ds;
                    byte[] buf = new byte[512];
                    while (!closed && !connected) {
                        DatagramPacket p = new DatagramPacket(buf, buf.length);
                        try { ds.receive(p); } catch (java.net.SocketTimeoutException t) { continue; }
                        String s = new String(p.getData(), 0, p.getLength(), "UTF-8");
                        String[] parts = s.split("\\|", 3);
                        if (parts.length < 3 || !parts[0].equals(TAG)) continue;
                        String ip = p.getAddress().getHostAddress();
                        found.put(ip, new String[]{ip, parts[2], parts[1], String.valueOf(System.currentTimeMillis())});
                    }
                    ds.close();
                } catch (Exception e) {
                    // a procura automática falhou; resta escrever o endereço
                } finally {
                    if (h != null) h.listening(false);
                }
            }
        }, "rede-procura").start();
    }

    /** Jogos encontrados nos últimos segundos: {ip, info, protocolo}. */
    public List<String[]> found() {
        ArrayList<String[]> out = new ArrayList<>();
        long now = System.currentTimeMillis();
        synchronized (found) {
            for (String[] f : found.values()) if (now - Long.parseLong(f[3]) < 4000) out.add(f);
        }
        return out;
    }

    public void connect(final String ip) {
        new Thread(new Runnable() {
            public void run() {
                try {
                    Socket s = new Socket();
                    s.connect(new InetSocketAddress(ip, PORT), 5000);
                    attach(s);
                } catch (Exception e) {
                    if (!closed) error = "Não foi possível ligar a " + ip + ".";
                }
            }
        }, "rede-ligar").start();
    }

    // ---------------------------------------------------------------- ligação

    private void attach(final Socket s) throws Exception {
        s.setTcpNoDelay(true);
        sock = s;
        final DataInputStream in = new DataInputStream(new BufferedInputStream(s.getInputStream()));
        final DataOutputStream out = new DataOutputStream(new BufferedOutputStream(s.getOutputStream()));
        lastRecv = System.currentTimeMillis();
        connected = true;
        new Thread(new Runnable() {
            public void run() {
                try {
                    while (!closed) {
                        int n = in.readInt();
                        if (n < 0 || n > 4_000_000) throw new Exception("tamanho");
                        byte[] b = new byte[n];
                        in.readFully(b);
                        lastRecv = System.currentTimeMillis();
                        inbox.add(b);
                    }
                } catch (Exception e) {
                    if (!closed) error = "A ligação foi interrompida.";
                    closed = true;
                }
            }
        }, "rede-ler").start();
        new Thread(new Runnable() {
            public void run() {
                try {
                    while (!closed) {
                        byte[] b = outbox.poll(500, TimeUnit.MILLISECONDS);
                        if (b == null) continue;
                        out.writeInt(b.length);
                        out.write(b);
                        if (outbox.isEmpty()) out.flush();
                    }
                } catch (Exception e) {
                    if (!closed) error = "A ligação foi interrompida.";
                    closed = true;
                }
            }
        }, "rede-escrever").start();
    }

    public void send(byte[] msg) { if (connected && !closed) outbox.add(msg); }

    public byte[] poll() { return inbox.poll(); }

    /** Milissegundos desde a última mensagem recebida. */
    public long silence() { return System.currentTimeMillis() - lastRecv; }

    public void close() {
        closed = true;
        try { if (sock != null) sock.close(); } catch (Exception ignored) { }
        try { if (server != null) server.close(); } catch (Exception ignored) { }
        try { if (disc != null) disc.close(); } catch (Exception ignored) { }
    }

    // ---------------------------------------------------------------- endereços

    /** Endereços IPv4 locais deste aparelho (para mostrar ao outro jogador). */
    public static List<String> localAddresses() {
        ArrayList<String> out = new ArrayList<>();
        try {
            for (NetworkInterface ni : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (!ni.isUp() || ni.isLoopback()) continue;
                for (InetAddress a : Collections.list(ni.getInetAddresses()))
                    if (a instanceof Inet4Address && a.isSiteLocalAddress()) out.add(a.getHostAddress());
            }
        } catch (Exception ignored) { }
        return out;
    }

    private static List<InetAddress> broadcastAddresses() {
        ArrayList<InetAddress> out = new ArrayList<>();
        try {
            for (NetworkInterface ni : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (!ni.isUp() || ni.isLoopback()) continue;
                for (InterfaceAddress ia : ni.getInterfaceAddresses())
                    if (ia.getBroadcast() != null) out.add(ia.getBroadcast());
            }
            out.add(InetAddress.getByName("255.255.255.255"));
        } catch (Exception ignored) { }
        return out;
    }
}
