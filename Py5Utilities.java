package py5utils;

import py5.core.Sketch;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import processing.core.PApplet;

public class Py5Utilities {
    public PApplet sketch;
    protected FloatBuffer points;
    protected FloatBuffer strokeWeights; // May be null if not provided
    protected IntBuffer colors;         // May be null if not provided
    protected int numPrimitives;
    protected int coordCount;  // Total coordinates for the primitive
    protected String primitiveType;     // Explicit primitive type
    protected boolean isClosed;         // Whether polylines should be drawn as closed shapes

    public Py5Utilities(PApplet sketch) {
        this.sketch = sketch;
    }

    /**
     * Receives the Direct Buffers from Python with explicit primitive type.
     * @param primitiveType Type of primitive ("point_2d", "point_3d", "line_2d", "line_3d", "polyline_2d", "polyline_3d")
     * @param points Direct buffer containing primitive coordinates (floats)
     * @param strokeWeights Direct buffer containing stroke weights (floats) or null
     * @param colors Direct buffer containing colors (one int per primitive) or null
     * @param numPrimitives Number of primitives
     * @param coordCount Total number of coordinates
     * @param isClosed Whether polylines should be drawn as closed shapes
     */
    public void shareBuffers(String primitiveType, FloatBuffer points, FloatBuffer strokeWeights, IntBuffer colors, 
                             int numPrimitives, int coordCount, boolean isClosed) {
        this.points = points;
        this.strokeWeights = strokeWeights;
        this.colors = colors;
        this.numPrimitives = numPrimitives;
        this.coordCount = coordCount;
        this.isClosed = isClosed;
        
        // Convert primitive type to uppercase for consistent internal handling
        this.primitiveType = primitiveType.toUpperCase().replace('_', '_');
    }

    /**
     * Legacy overload for backward compatibility
     */
    public void shareBuffers(FloatBuffer points, FloatBuffer strokeWeights, IntBuffer colors, 
                             int numPrimitives, int coordCount) {
        // Call the main method with default primitive type (can be modified as needed)
        shareBuffers("UNKNOWN", points, strokeWeights, colors, numPrimitives, coordCount, false);
    }

    /**
     * Draws primitives using the data in the shared Direct Buffers.
     * Handles points, lines, and polylines in 2D and 3D.
     * If colors is null, uses current sketch stroke color.
     * If strokeWeights is null, uses current sketch stroke weight.
     */
    public void drawPrimitives() {
        sketch.pushStyle();
        
        boolean hasColors = (colors != null);
        boolean hasStrokeWeights = (strokeWeights != null);
        
        if ("POINT_2D".equals(primitiveType) || "POINT_3D".equals(primitiveType)) {
            drawPoints(hasColors, hasStrokeWeights);
        } else if ("LINE_2D".equals(primitiveType) || "LINE_3D".equals(primitiveType)) {
            drawLines(hasColors, hasStrokeWeights);
        } else if ("POLYLINE_2D".equals(primitiveType) || "POLYLINE_3D".equals(primitiveType)) {
            drawPolylines(hasColors, hasStrokeWeights);
        } else {
            System.err.println("Unknown primitive type: " + primitiveType);
        }
        
        sketch.popStyle();
    }
    
    private void drawPoints(boolean hasColors, boolean hasStrokeWeights) {
        boolean is3D = "POINT_3D".equals(primitiveType);
        int pointsPerVertex = is3D ? 3 : 2;
        
        if (!hasStrokeWeights && !hasColors) {
            // Batch drawing with uniform appearance
            sketch.beginShape(PApplet.POINTS);
            for (int i = 0; i < numPrimitives; i++) {
                if (is3D) {
                    float x = points.get(i * pointsPerVertex);
                    float y = points.get(i * pointsPerVertex + 1);
                    float z = points.get(i * pointsPerVertex + 2);
                    sketch.vertex(x, y, z);
                } else {
                    float x = points.get(i * pointsPerVertex);
                    float y = points.get(i * pointsPerVertex + 1);
                    sketch.vertex(x, y);
                }
            }
            sketch.endShape();
        } else {
            // Draw points individually for custom appearance
            for (int i = 0; i < numPrimitives; i++) {
                if (hasColors) {
                    int col = colors.get(i);
                    sketch.stroke(col);
                }
                
                if (hasStrokeWeights) {
                    float sw = strokeWeights.get(i);
                    sketch.strokeWeight(sw);
                }
                
                sketch.beginShape(PApplet.POINTS);
                if (is3D) {
                    float x = points.get(i * pointsPerVertex);
                    float y = points.get(i * pointsPerVertex + 1);
                    float z = points.get(i * pointsPerVertex + 2);
                    sketch.vertex(x, y, z);
                } else {
                    float x = points.get(i * pointsPerVertex);
                    float y = points.get(i * pointsPerVertex + 1);
                    sketch.vertex(x, y);
                }
                sketch.endShape();
            }
        }
    }
    
    private void drawLines(boolean hasColors, boolean hasStrokeWeights) {
        boolean is3D = "LINE_3D".equals(primitiveType);
        int stride = is3D ? 6 : 4;
        
        if (!hasStrokeWeights && !hasColors) {
            // Batch drawing for uniform appearance
            sketch.beginShape(PApplet.LINES);
            for (int i = 0; i < numPrimitives; i++) {
                if (is3D) {
                    float x1 = points.get(i * stride);
                    float y1 = points.get(i * stride + 1);
                    float z1 = points.get(i * stride + 2);
                    float x2 = points.get(i * stride + 3);
                    float y2 = points.get(i * stride + 4);
                    float z2 = points.get(i * stride + 5);
                    sketch.vertex(x1, y1, z1);
                    sketch.vertex(x2, y2, z2);
                } else {
                    float x1 = points.get(i * stride);
                    float y1 = points.get(i * stride + 1);
                    float x2 = points.get(i * stride + 2);
                    float y2 = points.get(i * stride + 3);
                    sketch.vertex(x1, y1);
                    sketch.vertex(x2, y2);
                }
            }
            sketch.endShape();
        } else if (!hasStrokeWeights && hasColors) {
            // Batch by color
            sketch.beginShape(PApplet.LINES);
            int currentColor = -1;
            
            for (int i = 0; i < numPrimitives; i++) {
                int col = colors.get(i);
                if (col != currentColor) {
                    if (i > 0) {
                        sketch.endShape();
                        sketch.beginShape(PApplet.LINES);
                    }
                    sketch.stroke(col);
                    currentColor = col;
                }
                
                if (is3D) {
                    float x1 = points.get(i * stride);
                    float y1 = points.get(i * stride + 1);
                    float z1 = points.get(i * stride + 2);
                    float x2 = points.get(i * stride + 3);
                    float y2 = points.get(i * stride + 4);
                    float z2 = points.get(i * stride + 5);
                    sketch.vertex(x1, y1, z1);
                    sketch.vertex(x2, y2, z2);
                } else {
                    float x1 = points.get(i * stride);
                    float y1 = points.get(i * stride + 1);
                    float x2 = points.get(i * stride + 2);
                    float y2 = points.get(i * stride + 3);
                    sketch.vertex(x1, y1);
                    sketch.vertex(x2, y2);
                }
            }
            sketch.endShape();
        } else {
            // Draw each line individually to allow varying appearance
            for (int i = 0; i < numPrimitives; i++) {
                if (hasColors) {
                    int col = colors.get(i);
                    sketch.stroke(col);
                }
                
                if (hasStrokeWeights) {
                    float sw = strokeWeights.get(i);
                    sketch.strokeWeight(sw);
                }
                
                if (is3D) {
                    float x1 = points.get(i * stride);
                    float y1 = points.get(i * stride + 1);
                    float z1 = points.get(i * stride + 2);
                    float x2 = points.get(i * stride + 3);
                    float y2 = points.get(i * stride + 4);
                    float z2 = points.get(i * stride + 5);
                    sketch.line(x1, y1, z1, x2, y2, z2);
                } else {
                    float x1 = points.get(i * stride);
                    float y1 = points.get(i * stride + 1);
                    float x2 = points.get(i * stride + 2);
                    float y2 = points.get(i * stride + 3);
                    sketch.line(x1, y1, x2, y2);
                }
            }
        }
    }
    
    private void drawPolylines(boolean hasColors, boolean hasStrokeWeights) {
        boolean is3D = "POLYLINE_3D".equals(primitiveType);
        int pointsPerVertex = is3D ? 3 : 2;
        int numVertices = coordCount / pointsPerVertex;
        
        for (int i = 0; i < numPrimitives; i++) {
            // Apply style if provided
            if (hasColors) {
                int col = colors.get(i);
                sketch.stroke(col);
            }
            
            if (hasStrokeWeights) {
                float sw = strokeWeights.get(i);
                sketch.strokeWeight(sw);
            }
            
            // Disable fill for polylines (outline only)
            sketch.noFill();
            
            // Always use LINE_STRIP instead of POLYGON to avoid filling
            sketch.beginShape(PApplet.LINE_STRIP);
            
            // Add all vertices for this polyline
            int baseIndex = i * coordCount;
            for (int j = 0; j < numVertices; j++) {
                int vertexIndex = baseIndex + (j * pointsPerVertex);
                
                if (is3D) {
                    float x = points.get(vertexIndex);
                    float y = points.get(vertexIndex + 1);
                    float z = points.get(vertexIndex + 2);
                    sketch.vertex(x, y, z);
                } else {
                    float x = points.get(vertexIndex);
                    float y = points.get(vertexIndex + 1);
                    sketch.vertex(x, y);
                }
            }
            
            // If closed, explicitly add the first vertex again at the end
            if (isClosed && numVertices > 0) {
                int firstVertexIndex = baseIndex;
                
                if (is3D) {
                    float x = points.get(firstVertexIndex);
                    float y = points.get(firstVertexIndex + 1);
                    float z = points.get(firstVertexIndex + 2);
                    sketch.vertex(x, y, z);
                } else {
                    float x = points.get(firstVertexIndex);
                    float y = points.get(firstVertexIndex + 1);
                    sketch.vertex(x, y);
                }
            }
            
            sketch.endShape();
        }
    }
}