# render-utils
![Python Version](https://img.shields.io/badge/python-3.11-blue)
![Dependencies](https://img.shields.io/badge/dependencies-NumPy-brightgreen)

Simple module designed to streamline the creation of **graphic primitives** (points, lines, polylines) and enable their fast rendering in **py5** using *Direct Buffers*.

## Utility functions
- `init_buffers`: Initialize and allocate native Direct Buffers for rendering primitives via a custom *Py5Utilities* class.
- `pack_color`: Pack a color array into a single 32-bit integer in ARGB format.
- `direct_render`: Trigger the rendering of primitives from the shared Direct Buffers.

The script uses JPype to access Java classes and methods, NumPy to manipulate arrays, and py5 for the sketch (drawing) environment.

## Description

The main `init_buffers` function takes up to 6 parameters, only the first 2 are required, the rest is optional:

```python
init_buffers(primitive_type,   # (str)  either 'point2d', 'point3d', 'line2d', 'line3d', 'polyline2d' or 'polyline3d'
             num_primitive,    # (int)  the number of primitives to render
             num_coords,       # (int)  the number of coordinates (only required when the primitive is of type 'polyline')
             is_stroked,       # (bool) whether the primitives have stroke weights/widths or not
             is_colored,       # (bool) whether the primitives have colors or not
             is_closed)        # (bool) whether the primitives are closed or not (only valid for polylines)
```

## Example

```python
N = 16000

# To render N 2d points with no predefined stroke weights or colors
coords_buffer, _, _ = init_buffers("point_2d", N)

# To render N 3d lines with colors and no predefined stroke weights
coords_buffer, _, colors_buffer = init_buffers("line_3d", N, is_colored=True)

# To render N closed 2d polylines of 4 vertices each, with colors and stroke weights
coords_count = 8   # 4 vertices in 2d means 8 coordinates (p1x, p1y, p2x, p2y, p3x, p3y, p4x, p4y)
coords_buffer, weights_buffer, colors_buffer = init_buffers("polyline_2d", N, num_coords=coords_count, is_stroked=True, is_colored=True, is_closed=True)
```
Once the buffers have been initialized, the associated arrays must be set up:

```python
verts = np.asarray(coords_buffer).reshape(N, coords_count)
colors = np.asarray(colors_buffer).reshape(N)              # optional
weights = np.asarray(weights_buffer).reshape(N)            # optional
```

Then these arrays should be updated, either in `setup()` or in `draw()`, with the desired values (coordinates, colors, stroke weights) before rendering:

```python
# Send coordinates to the corresponding buffers
verts[:] = balls.pos  # 'balls' is an object returning a numpy array of positions similar in shape to the corresponding buffer

# Batch render the lines directly from the native buffers
direct_render()
```
See the provided example sketches for more details.

## Setup
The Py5Utilities Java class is already compiled into a .jar file using Maven, so users do not need to download or build the Java project themselves. Simply include the pre-compiled .jar in the `jars` folder of your project and use the provided methods directly within Python through the utility functions of `render_utils.py`.

```
Sketch Folder/
├── jars/
│   └── Py5Utilities.jar
├── render_utils.py 
└── main_sketch.py
```


## How it works
`render_utils` employs a custom Java utility class, `Py5Utilities`, that provides essential methods for efficient rendering and memory management in the context of the Py5 library.

Specifically, `Py5Utilities` allows for the creation, allocation, and management of native memory buffers ([Direct Buffers](https://jpype.readthedocs.io/en/latest/userguide.html#buffer-backed-numpy-arrays)), which are used for storing coordinate data, stroke weights, and color information of rendering primitives. By using Direct Buffers, it ensures that the data is stored in memory directly accessible by the GPU, minimizing the overhead of copying data between Python, Java, and the underlying graphics hardware.

Simply put, it's an intermediary that bridges the Python and Java sides, ensuring that any changes to the buffers made in Python (e.g., coordinate updates or color modifications) are reflected in Java’s rendering pipeline without the need for redundant data copying. This "zero-copy" data sharing enhances performance.

## Tests
- 20_000 moving rectangles as **2d polylines** (80_000 short lines) at 60fps
- 80_000 moving square-cornered **2d points** at 60fps
- 5_000 moving (very long) **2d lines** at 60fps

*Please note that performance can vary depending on the stroke weight (and/or the length) selected. The thicker (or the longer) the primitive, the more pixels it requires, and the slower the rendering.
All the above tests are performed with a default stroke weight of 1.*

Tested on a 2020 MSI laptop with Intel Core i7-8750H CPU @ 2.20GHz and 32Gb RAM

## Dependancies
- py5
- NumPy



Takes much inspirations from the py5 official [documentation](https://py5coding.org/content/hybrid_programming.html) on Hybrid Programming by James Schmitz. Many thanks to him.
