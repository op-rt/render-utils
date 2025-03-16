from render_utils import init_buffers, pack_color, direct_render
import numpy as np

W, H = 1600, 900  # Dimensions of canvas
N = 20000         # Number of boxes

# Number of polylines and coordinates
num_coords = 8    # (p1x, p1y, p2x, p2y, p3x, p3y, p4x, p4y) for a 2d polyline forming a box/square
num_plines = N

# Initialize the buffers
coords_buffer, weights_buffer, colors_buffer = init_buffers("polyline_2d", num_plines, num_coords, is_stroked=True, is_colored=True, is_closed=True)

# Initialize associated arrays
verts = np.asarray(coords_buffer).reshape(num_plines, num_coords)
colors = np.asarray(colors_buffer).reshape(num_plines)
weights = np.asarray(weights_buffer).reshape(num_plines)

# Predefine stroke weights and colors
# Note: in this example weights and colors are fixed and can be set outside of draw() in advance
weights[:] = np.full(num_plines, .5)
colors[:] = pack_color(np.array([0, 0, 0]))


def setup():
    size(W, H, P2D)
    frame_rate(1000)
    text_size(18)
    fill("#00F")
    
    global boxes
    
    # Create boxes with varying radii scattered randomly
    boxes = Boxes(num=N, canvas_width=W, canvas_height=H)
    
   
def draw():
    background("#FFF")
        
    # Update boxes' positions
    boxes.update()

    # Send edges' coordinates to the corresponding buffers
    verts[:, :] = boxes.get_vertices()
    
    # Batch render the polylines directly from the native buffers
    direct_render()

    # Info
    text("num: " + str(N), 10, 20)
    text("fps: %i" % int(get_frame_rate()), 10, 40)


class Boxes:
    
    def __init__(self, num, canvas_width, canvas_height):
        self.canvas_height = canvas_height
        self.canvas_width = canvas_width
        
        self.pos = np.column_stack((np.random.uniform(0, canvas_width, num), np.random.uniform(0, canvas_height, num)))
        self.phi = np.random.uniform(0, 2 * np.pi, num) * .25
        self.vel = np.column_stack((np.cos(self.phi), np.sin(self.phi))) * .5
        self.rad = np.random.randint(2, 7, num)        
        
    def update(self):
        
        # Update positions 
        self.pos += self.vel
        
        # Check for out-of-bound conditions along x and y axes
        out_of_bounds = (self.pos > [self.canvas_width, self.canvas_height]) | (self.pos < [0, 0])

        # Flip the velocity where out-of-bounds conditions are met
        self.vel *= ~out_of_bounds * 2 - 1
        
    def get_vertices(self):
        
        min_x = self.pos[:, 0] - self.rad
        max_x = self.pos[:, 0] + self.rad
        min_y = self.pos[:, 1] - self.rad
        max_y = self.pos[:, 1] + self.rad
        
        return np.array([min_x, max_y, max_x, max_y, max_x, min_y, min_x, min_y]).T