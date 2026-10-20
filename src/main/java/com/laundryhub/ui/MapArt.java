package com.laundryhub.ui;

/**
 * Simple map drawing from the design, shown when Google Maps is not available.
 */
public final class MapArt {

    /** Map drawing (from the prototype). */
    public static final String SVG = "<svg width=\"100%\" height=\"100%\" viewBox=\"0 0 346 250\" "
            + "preserveAspectRatio=\"none\" aria-hidden=\"true\">"
            + "<rect x=\"0\" y=\"0\" width=\"346\" height=\"250\" fill=\"#E3EAE2\"/>"
            + "<rect x=\"210\" y=\"20\" width=\"110\" height=\"70\" rx=\"10\" fill=\"#CFE0C9\"/>"
            + "<rect x=\"20\" y=\"160\" width=\"90\" height=\"70\" rx=\"10\" fill=\"#CFE0C9\"/>"
            + "<path d=\"M230 160 L310 170 L310 230 L225 230 Z\" fill=\"#BFD6EA\"/>"
            + "<path d=\"M0 120 C 90 100, 200 150, 346 110\" stroke=\"#fff\" stroke-width=\"16\" fill=\"none\"/>"
            + "<path d=\"M0 50 L346 70\" stroke=\"#fff\" stroke-width=\"8\" fill=\"none\"/>"
            + "<path d=\"M0 200 L346 185\" stroke=\"#fff\" stroke-width=\"8\" fill=\"none\"/>"
            + "<path d=\"M140 0 L170 250\" stroke=\"#fff\" stroke-width=\"12\" fill=\"none\"/>"
            + "<path d=\"M296 0 L275 250\" stroke=\"#fff\" stroke-width=\"8\" fill=\"none\"/></svg>";

    private MapArt() {
    }
}
