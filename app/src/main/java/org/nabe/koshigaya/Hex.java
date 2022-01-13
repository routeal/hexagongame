// copy from https://www.redblobgames.com/grids/hexagons/implementation.html

package org.nabe.koshigaya;

import java.util.ArrayList;

public class Hex {

    // added
    public int c;

    // added
    public Hex(int q, int r, int s, int c) {
        this(q, r, s);
        this.c = c;
    }

    public Hex(int q, int r, int s) {
        this.q = q;
        this.r = r;
        this.s = s;
        if (q + r + s != 0) throw new IllegalArgumentException("q + r + s must be 0");
    }


    public final int q;
    public final int r;
    public final int s;


    public Hex add(Hex b) {
        return new Hex(q + b.q, r + b.r, s + b.s);
    }


    public Hex subtract(Hex b) {
        return new Hex(q - b.q, r - b.r, s - b.s);
    }


    public Hex scale(int k) {
        return new Hex(q * k, r * k, s * k);
    }


    public Hex rotateLeft() {
        return new Hex(-s, -q, -r);
    }


    public Hex rotateRight() {
        return new Hex(-r, -s, -q);
    }


    static public ArrayList<Hex> directions = new ArrayList<Hex>() {{
        add(new Hex(1, 0, -1));
        add(new Hex(1, -1, 0));
        add(new Hex(0, -1, 1));
        add(new Hex(-1, 0, 1));
        add(new Hex(-1, 1, 0));
        add(new Hex(0, 1, -1));
    }};

    static public Hex direction(int direction) {
        return Hex.directions.get(direction);
    }


    public Hex neighbor(int direction) {
        return add(Hex.direction(direction));
    }


    public boolean isNeighbor(Hex b) {
        return b.equals(neighbor(0)) ||
                b.equals(neighbor(1)) ||
                b.equals(neighbor(2)) ||
                b.equals(neighbor(3)) ||
                b.equals(neighbor(4)) ||
                b.equals(neighbor(5));
    }


    static public ArrayList<Hex> diagonals = new ArrayList<Hex>() {{
        add(new Hex(2, -1, -1));
        add(new Hex(1, -2, 1));
        add(new Hex(-1, -1, 2));
        add(new Hex(-2, 1, 1));
        add(new Hex(-1, 2, -1));
        add(new Hex(1, 1, -2));
    }};


    public Hex diagonalNeighbor(int direction) {
        return add(Hex.diagonals.get(direction));
    }


    public boolean isDiagonalNeighbor(Hex b) {
        return b.equals(diagonalNeighbor(0)) ||
                b.equals(diagonalNeighbor(1)) ||
                b.equals(diagonalNeighbor(2)) ||
                b.equals(diagonalNeighbor(3)) ||
                b.equals(diagonalNeighbor(4)) ||
                b.equals(diagonalNeighbor(5));
    }


    public int length() {
        return (int) ((Math.abs(q) + Math.abs(r) + Math.abs(s)) / 2);
    }


    public int distance(Hex b) {
        return subtract(b).length();
    }


    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }

        if (obj.getClass() != this.getClass()) {
            return false;
        }

        final Hex other = (Hex) obj;

        return this.q == other.q && this.r == other.r && this.s == other.s;
    }

    @Override
    public int hashCode() {
        return Integer.valueOf(q).hashCode() + Integer.valueOf(r).hashCode() + Integer.valueOf(s).hashCode();
    }
}
