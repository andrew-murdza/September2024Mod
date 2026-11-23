package net.amurdza.examplemod.client;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/** Subtracts upward extrusions of opaque model texels, without changing the plant's texture or height elsewhere. */
public final class AquaticPlantMask {
    private static final double EPSILON = 1.0E-7;

    private AquaticPlantMask() { }

    public record Plane(double x, double y, double z, double distance) {
        double at(double[] vertex) { return x * vertex[0] + y * vertex[1] + z * vertex[2] - distance; }
    }

    public record Column(List<Plane> planes, double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        public Column relativeTo(Vec3 origin) {
            return new Column(planes.stream().map(p -> new Plane(p.x, p.y, p.z,
                    p.distance - p.x * origin.x - p.y * origin.y - p.z * origin.z)).toList(),
                    minX - origin.x, minY - origin.y, minZ - origin.z, maxX - origin.x, maxY - origin.y, maxZ - origin.z);
        }
    }

    public static Column column(Vec3 a, Vec3 b, Vec3 c, Vec3 d) {
        Vec3 normal = b.subtract(a).cross(c.subtract(a));
        if (Math.abs(normal.y) < EPSILON) return null; // A vertical face has no area when viewed from above.
        if (normal.y < 0) normal = normal.scale(-1);
        List<Plane> planes = new ArrayList<>();
        planes.add(new Plane(normal.x, normal.y, normal.z, normal.dot(a)));
        Vec3[] points = {a, b, c, d};
        Vec3 centre = a.add(b).add(c).add(d).scale(.25);
        double minX = a.x, minY = a.y, minZ = a.z, maxX = a.x, maxY = a.y, maxZ = a.z;
        for (int i = 0; i < 4; i++) {
            Vec3 from = points[i], to = points[(i + 1) % 4];
            double nx = -(to.z - from.z), nz = to.x - from.x;
            double distance = nx * from.x + nz * from.z;
            if (nx * centre.x + nz * centre.z < distance) { nx = -nx; nz = -nz; distance = -distance; }
            planes.add(new Plane(nx, 0, nz, distance));
            minX = Math.min(minX, from.x); minY = Math.min(minY, from.y); minZ = Math.min(minZ, from.z);
            maxX = Math.max(maxX, from.x); maxY = Math.max(maxY, from.y); maxZ = Math.max(maxZ, from.z);
        }
        return new Column(List.copyOf(planes), minX, minY, minZ, maxX, maxY, maxZ);
    }

    public static List<BakedQuad> clip(BakedQuad quad, List<Column> columns) {
        if (columns.isEmpty()) return List.of(quad);
        int[] raw = quad.getVertices();
        int stride = raw.length / 4;
        List<double[]> polygon = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            int offset = i * stride;
            polygon.add(new double[]{Float.intBitsToFloat(raw[offset]), Float.intBitsToFloat(raw[offset + 1]),
                    Float.intBitsToFloat(raw[offset + 2]), Float.intBitsToFloat(raw[offset + 4]), Float.intBitsToFloat(raw[offset + 5])});
        }
        List<List<double[]>> pieces = new ArrayList<>();
        pieces.add(polygon);
        for (Column column : columns) {
            List<List<double[]>> remaining = new ArrayList<>();
            for (List<double[]> piece : pieces) {
                boolean left = true, right = true, front = true, back = true, below = true;
                for (double[] point : piece) {
                    left &= point[0] <= column.minX; right &= point[0] >= column.maxX;
                    front &= point[2] <= column.minZ; back &= point[2] >= column.maxZ;
                    below &= point[1] <= column.minY;
                }
                if (left || right || front || back || below) { remaining.add(piece); continue; }
                List<double[]> inside = piece;
                for (Plane plane : column.planes) {
                    List<double[]> outside = split(inside, plane, false);
                    if (outside.size() >= 3) remaining.add(outside);
                    inside = split(inside, plane, true);
                    if (inside.size() < 3) break;
                }
            }
            pieces = remaining;
            if (pieces.isEmpty()) break;
        }
        List<BakedQuad> result = new ArrayList<>();
        for (List<double[]> piece : pieces) {
            for (int corner = 1; corner + 1 < piece.size(); corner++) {
                int[] vertices = raw.clone();
                double[][] triangle = {piece.get(0), piece.get(corner), piece.get(corner + 1), piece.get(corner + 1)};
                for (int i = 0; i < 4; i++) {
                    int offset = i * stride;
                    for (int axis = 0; axis < 3; axis++) vertices[offset + axis] = Float.floatToRawIntBits((float) triangle[i][axis]);
                    vertices[offset + 4] = Float.floatToRawIntBits((float) triangle[i][3]);
                    vertices[offset + 5] = Float.floatToRawIntBits((float) triangle[i][4]);
                }
                result.add(new BakedQuad(vertices, quad.getTintIndex(), quad.getDirection(), quad.getSprite(), quad.isShade()));
            }
        }
        return result;
    }

    private static List<double[]> split(List<double[]> polygon, Plane plane, boolean positive) {
        List<double[]> result = new ArrayList<>();
        if (polygon.isEmpty()) return result;
        double[] previous = polygon.get(polygon.size() - 1);
        double before = plane.at(previous);
        for (double[] current : polygon) {
            double after = plane.at(current);
            boolean had = positive ? before >= -EPSILON : before < -EPSILON;
            boolean has = positive ? after >= -EPSILON : after < -EPSILON;
            if (had != has) {
                double fraction = before / (before - after);
                double[] crossing = new double[5];
                for (int i = 0; i < crossing.length; i++) crossing[i] = previous[i] + fraction * (current[i] - previous[i]);
                result.add(crossing);
            }
            if (has) result.add(current);
            previous = current; before = after;
        }
        return result;
    }
}
