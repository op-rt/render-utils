from render_utils import init_buffers, pack_color, direct_render
import numpy as np

W, H = 1600, 900  # Dimensions of canvas
N = 5000          # Number of lines

# Number of polylines and coordinates
num_coords = 4    # (p1x, p1y, p2x, p2y) for a 2d line 
num_lines = N

# Create unique pairs of indices (lines' endpoints)
indices = np.arange(N * 2)
np.random.shuffle(indices)  
pairs = indices.reshape(-1, 2)

# Initialize the buffers
coords_buffer, _, colors_buffer = init_buffers("line_2d", num_lines, is_colored=True)

# Initialize associated arrays
verts = np.asarray(coords_buffer).reshape(num_lines, num_coords)
colors = np.asarray(colors_buffer).reshape(num_lines)

# Predefine colors (randomly)
colors[:] = np.apply_along_axis(pack_color, axis=1, arr=np.random.randint(0, 255, (num_lines, 3)))


def setup():
    size(W, H, P2D)
    stroke_cap(PROJECT)
    frame_rate(1000)
    text_size(18)
    fill("#00F")
    
    global balls
    
    # Create balls scattered randomly
    balls = Balls(num=N*2, canvas_width=W, canvas_height=H)
    
   
def draw():
    background("#FFF")
        
    # Update balls' positions
    balls.update()

    # Send coordinates to the corresponding buffers
    verts[:] = balls.pos[pairs].reshape(num_lines, num_coords)
    
    # Batch render the lines directly from the native buffers
    direct_render()

    # Info
    text("num: " + str(num_lines), 10, 20)
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