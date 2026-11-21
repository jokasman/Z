package pt.setorzero.core;

/** A* em 8 direções sobre a grelha de quadrículas, com alisamento do caminho por linha de vista. */
final class PathFinder {
    private static final int[] DX = {1, -1, 0, 0, 1, 1, -1, -1};
    private static final int[] DY = {0, 0, 1, -1, 1, -1, 1, -1};

    private final World w;
    private final int W, H, N;
    private final float[] g;
    private final int[] parent, stamp, closed;
    private int cur;
    private final int[] heap;
    private final float[] hf;
    private int hs;
    private final int[] tmp;
    private final float[] pts;

    PathFinder(World w) {
        this.w = w;
        W = w.W; H = w.H; N = W * H;
        g = new float[N];
        parent = new int[N];
        stamp = new int[N];
        closed = new int[N];
        heap = new int[N * 8 + 16];
        hf = new float[N * 8 + 16];
        tmp = new int[N];
        pts = new float[N * 2 + 4];
    }

    private boolean blocked(int x, int y) { return w.blockedTile(x, y); }

    private float h(int a, int b) {
        int dx = Math.abs(a % W - b % W), dy = Math.abs(a / W - b / W);
        return Math.max(dx, dy) + 0.4142f * Math.min(dx, dy);
    }

    private void push(int n, float f) {
        int i = hs++;
        heap[i] = n; hf[i] = f;
        while (i > 0) {
            int p = (i - 1) >> 1;
            if (hf[p] <= hf[i]) break;
            swap(i, p);
            i = p;
        }
    }

    private int pop() {
        int top = heap[0];
        hs--;
        heap[0] = heap[hs]; hf[0] = hf[hs];
        int i = 0;
        while (true) {
            int l = 2 * i + 1, r = l + 1, m = i;
            if (l < hs && hf[l] < hf[m]) m = l;
            if (r < hs && hf[r] < hf[m]) m = r;
            if (m == i) break;
            swap(i, m);
            i = m;
        }
        return top;
    }

    private void swap(int a, int b) {
        int t = heap[a]; heap[a] = heap[b]; heap[b] = t;
        float f = hf[a]; hf[a] = hf[b]; hf[b] = f;
    }

    /** Quadrícula livre mais próxima de t (até maxR de distância), ou -1. */
    int nearestFree(int t, int maxR) {
        int tx = t % W, ty = t / W;
        if (!blocked(tx, ty)) return t;
        for (int r = 1; r <= maxR; r++) {
            int best = -1;
            float bd = Float.MAX_VALUE;
            for (int y = ty - r; y <= ty + r; y++) {
                for (int x = tx - r; x <= tx + r; x++) {
                    if (Math.max(Math.abs(x - tx), Math.abs(y - ty)) != r) continue;
                    if (blocked(x, y)) continue;
                    float d = (x - tx) * (x - tx) + (y - ty) * (y - ty);
                    if (d < bd) { bd = d; best = y * W + x; }
                }
            }
            if (best >= 0) return best;
        }
        return -1;
    }

    private int tileOf(float px, float py) {
        int x = Math.max(0, Math.min(W - 1, (int) (px / World.TILE)));
        int y = Math.max(0, Math.min(H - 1, (int) (py / World.TILE)));
        return y * W + x;
    }

    /** Devolve os pontos do caminho (x0, y0, x1, y1, ...), sem incluir a posição inicial, ou null. */
    float[] find(float sx, float sy, float tx, float ty, float radius) {
        int s = tileOf(sx, sy);
        int t = tileOf(tx, ty);
        boolean exact = !blocked(t % W, t / W);
        if (!exact) {
            t = nearestFree(t, 10);
            if (t < 0) return null;
        }
        float T = World.TILE;
        float ex = exact ? tx : (t % W + 0.5f) * T, ey = exact ? ty : (t / W + 0.5f) * T;
        if (s == t) return new float[]{ex, ey};

        cur++;
        hs = 0;
        g[s] = 0; stamp[s] = cur; parent[s] = -1;
        push(s, h(s, t));
        boolean found = false;
        while (hs > 0) {
            int n = pop();
            if (closed[n] == cur) continue;
            closed[n] = cur;
            if (n == t) { found = true; break; }
            int nx = n % W, ny = n / W;
            for (int k = 0; k < 8; k++) {
                int ax = nx + DX[k], ay = ny + DY[k];
                if (blocked(ax, ay)) continue;
                boolean diag = k >= 4;
                if (diag && (blocked(nx + DX[k], ny) || blocked(nx, ny + DY[k]))) continue;
                int m = ay * W + ax;
                if (closed[m] == cur) continue;
                float ng = g[n] + (diag ? 1.4142f : 1f);
                if (stamp[m] != cur || ng < g[m]) {
                    stamp[m] = cur; g[m] = ng; parent[m] = n;
                    push(m, ng + h(m, t));
                }
            }
        }
        if (!found) return null;

        int k = 0;
        for (int n = t; n != s && n >= 0; n = parent[n]) tmp[k++] = n;
        // pontos pela ordem do percurso (centros das quadrículas; o último é o destino exato)
        for (int i = 0; i < k; i++) {
            int n = tmp[k - 1 - i];
            pts[i * 2] = (n % W + 0.5f) * T;
            pts[i * 2 + 1] = (n / W + 0.5f) * T;
        }
        pts[(k - 1) * 2] = ex;
        pts[(k - 1) * 2 + 1] = ey;

        float r = radius * 0.9f;
        float[] out = new float[k * 2];
        int o = 0;
        float cx = sx, cy = sy;
        int i = 0;
        while (i < k) {
            int j = i;
            while (j + 1 < k && clear(cx, cy, pts[(j + 1) * 2], pts[(j + 1) * 2 + 1], r)) j++;
            cx = pts[j * 2]; cy = pts[j * 2 + 1];
            out[o++] = cx; out[o++] = cy;
            i = j + 1;
        }
        float[] res = new float[o];
        System.arraycopy(out, 0, res, 0, o);
        return res;
    }

    boolean clear(float ax, float ay, float bx, float by, float r) {
        float dx = bx - ax, dy = by - ay;
        float d = (float) Math.sqrt(dx * dx + dy * dy);
        int steps = (int) Math.ceil(d / 6f);
        for (int i = 1; i <= steps; i++) {
            float f = (float) i / steps;
            if (!w.freeCircle(ax + dx * f, ay + dy * f, r)) return false;
        }
        return true;
    }
}
