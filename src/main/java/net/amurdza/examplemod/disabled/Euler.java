//package net.amurdza.examplemod.entity.util;
//
//import net.minecraft.util.Mth;
//import net.minecraft.world.phys.Vec3;
//
//public class Euler {
//    public float x;
//    public float y;
//    public float z;
//
//    public Euler() {
//        this.setAngles(0.0F, 0.0F, 0.0F);
//    }
//
//    public Euler(float x, float y, float z) {
//        this.setAngles(x, y, z);
//    }
//
//    public Euler(Euler angle) {
//        this.setAngles(angle);
//    }
//
//    public Euler(Vec3 vec) {
//        this.setAngles(vec);
//    }
//
//    public Euler clone() {
//        return new Euler(this);
//    }
//
//    public void setAngles(float x, float y, float z) {
//        this.x = x;
//        this.y = y;
//        this.z = z;
//    }
//
//    public void setAngles(Euler angle) {
//        this.x = angle.x;
//        this.y = angle.y;
//        this.z = angle.z;
//    }
//
//    public void setAngles(Vec3 vec) {
//        this.x = (float) Math.atan2(vec.y, Math.sqrt(vec.x * vec.x + vec.z * vec.z));
//        this.y = (float) Math.atan2(vec.x, vec.z);
//        this.z = 0.0F;
//    }
//
//    public void rotate(float x, float y, float z) {
//        this.x += x;
//        this.y += y;
//        this.z += z;
//    }
//
//    public void rotate(Euler angle) {
//        this.x += angle.x;
//        this.y += angle.y;
//        this.z += angle.z;
//    }
//
//    public Vec3 rotateVector(float length) {
//        if (length == 0.0F) {
//            return Vec3.ZERO;
//        }
//
//        Euler rad = this.toRad();
//        float yawOffset = 1.5707964F;
//
//        float xSin = Mth.sin(rad.x);
//        float xCos = Mth.cos(rad.x);
//        float ySin = Mth.sin(rad.y + yawOffset);
//        float yCos = Mth.cos(rad.y + yawOffset);
//        float zSin = Mth.sin(rad.z);
//        float zCos = Mth.cos(rad.z);
//
//        double xOffset = xCos * yCos;
//        double yOffset = xCos * ySin * zSin - zCos * xSin;
//        double zOffset = xSin * zSin + xCos * zCos * ySin;
//
//        return new Vec3(xOffset * length, yOffset * length, zOffset * length);
//    }
//
//    public Euler getRotated(float x, float y, float z) {
//        Euler angle = new Euler(this);
//        angle.rotate(x, y, z);
//        return angle;
//    }
//
//    public Euler getRotated(Euler angle) {
//        Euler newAngle = new Euler(this);
//        newAngle.rotate(angle);
//        return newAngle;
//    }
//
//    public Euler toRad() {
//        return new Euler(this.x * 0.017453292F, this.y * 0.017453292F, this.z * 0.017453292F);
//    }
//
//    public Euler toDegrees() {
//        return new Euler(this.x * 57.295776F, this.y * 57.295776F, this.z * 57.295776F);
//    }
//}