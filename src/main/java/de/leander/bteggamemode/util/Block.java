package de.leander.bteggamemode.util;

import org.bukkit.Material;

public class Block {

    int x;
    int y;
    int z;
    Material mat;
    byte data;

    public Block(int x, int z, Material mat, byte data) {
        this.x = x;
        this.z = z;
        this.mat = mat;
        this.data = data;
    }

    public void setX(int x) {
        this.x = x;
    }

    public void setZ(int z) {
        this.z = z;
    }

    public void setY(int y) { this.y = y;}

    public void setMat(Material mat) {
        this.mat = mat;
    }

    public void setData(byte data) {
        this.data = data;
    }

    public int getX() {
        return x;
    }

    public int getZ() {
        return z;
    }

    public int getY() { return y; }

    public Material getMat() {
        return mat;
    }

    public byte getData() {
        return data;
    }
}
