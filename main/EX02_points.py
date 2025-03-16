from render_utils import init_buffers, pack_color, direct_render
import numpy as np

W, H = 1600, 900  # Dimensions of canvas
N = 80000         # Number of points

# Number of polylines and coordinates
num_coords = 2    # (px, py) for a 2d point 
num_points = N

# Initialize the buffers
coords_buffer, _, _ = init_buffers("point_2d", num_points)

# Initialize associated arrays
verts = np.asarray(coords_buffer).reshape(num_points, num_coords)


def setup():
    size(W, H, P2D)
    stroke_cap(PROJECT)
    frame_rate(1000)
    text_size(18)
    fill("#00F")
    
    global balls
    
    # Create boxes with varying radii scattered randomly
    balls = Balls(num=N, canvas_width=W, canvas_height=H)
    
   
def draw():
    background("#FFF")
        
    # Update balls' positions
    balls.update()

    # Send coordinates to the corresponding buffers
    verts[:] = balls.pos
    
    # Batch render the polylines directly from the native buffers
    direct_render()

    # Info
    text("num: " + str(N), 10, 20)
    text("fps: %i" % int(get_frame_rate()), 10, 40)


class Balls:
    
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