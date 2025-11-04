#include "fmnsoc.h"
#include "invaders.h"
#define N_OBJECTS 7

/*
 * Definition of bitmap for each line in the 8x8 pattern 
 *
 * Since we have a word addressable bitmap,  we need to reorder the patterns
 * so that they display correctly.
 * We do that in the definitions to avoid run time penalties.
 */
#define R(x) (((x & 0xf0) >> 4) | ((x & 0x0f) << 4))
static uint8_t sprite_sship[8]  = {
    R(0b00000000),
    R(0b00111100),
    R(0b01111110),
    R(0b11111111),
    R(0b11111111),
    R(0b11100111),
    R(0b11000011),
    R(0b11000011),
};
static uint8_t sprite_laser[8]  = {
    R(0b00011000),
    R(0b00011000),
    R(0b00011000),
    R(0b00011000),
    R(0b00011000),
    R(0b00011000),
    R(0b00011000),
    R(0b00011000),
};
static uint8_t sprite_alien1[8] = {
    R(0b11000011),
    R(0b00111100),
    R(0b01011010),
    R(0b11111111),
    R(0b11111111),
    R(0b10000001),
    R(0b01000010),
    R(0b00100100),
};
static uint8_t sprite_alien2[8] = {
    R(0b11000011),
    R(0b00111100),
    R(0b01011010),
    R(0b11111111),
    R(0b11111111),
    R(0b10100101),
    R(0b10100101),
    R(0b01011010),
};
static uint8_t sprite_alien3[8] = {
    R(0b01000010),
    R(0b00100100),
    R(0b00111100),
    R(0b01011010),
    R(0b11111111),
    R(0b10111101),
    R(0b10000001),
    R(0b01000010),
};
static uint8_t sprite_alien4[8] = {
    R(0b11000011),
    R(0b00100100),
    R(0b10111101),
    R(0b11100111),
    R(0b00111100),
    R(0b00100100),
    R(0b01011010),
    R(0b10011001),
};
static uint8_t sprite_alien5[8] = {
    R(0b01100110),
    R(0b01111110),
    R(0b11011011),
    R(0b01111110),
    R(0b10111101),
    R(0b10100101),
    R(0b10000001),
    R(0b01100110),
};
// bleu, vert, rouge pour tenir dans l'uint8_t de color_t comme il faut
static const color_t black = {0, 0, 0};
static const color_t white = {7, 7, 3};
static const color_t red = {0, 0, 3};
static const color_t green = {0, 7, 0};
static const color_t blue = {7, 0, 0};
static const color_t magenta = {7, 0, 3};
static const color_t cyan = {7, 7, 1};
static const color_t yellow = {0, 7, 3};

/* sprite objects */
object_t object[N_OBJECTS] = {
   /* blue spaceship */
   {1, 3, 1, 18, 23,  0, 0, sprite_sship,  blue, {[0 ... 7]={0}}, 0, 0},
   /* white laser */
   {0, 1, 1, 18,  0,  0, 0, sprite_laser,  white, {[0 ... 7]={0}}, 0, 0},
   /* green alien */
   {1, 4, 1, 10,  0, -1, 0, sprite_alien1, green, {[0 ... 7]={0}}, 0, 0},
   /* red alien */
   {1, 4, 1, 18,  0, -1, 0, sprite_alien2, red, {[0 ... 7]={0}}, 0, 0},
   /* magenta alien */
   {1, 4, 1, 26,  0, -1, 0, sprite_alien3, magenta, {[0 ... 7]={0}}, 0, 0},
   /* yellow alien */
   {1, 4, 1, 14,  2, -1, 0, sprite_alien4, yellow, {[0 ... 7]={0}}, 0, 0},
   /* cyan alien */
   {1, 4, 1, 22,  2, -1, 0, sprite_alien5, cyan, {[0 ... 7]={0}}, 0, 0}
};

/* Tableau d'états pour la mécanique du jeu */
state_t state[5] = {
   {0, 1, 1},
   {0, 1, 2},
   {1, 0, 3},
   {0, 1, 4},
   {-1, 0, 1}
};

/*
 * Deux pointeurs vers le frame buffer pour éviter les problèmes
 * d'aliasing sur les types.
 */
static volatile color_t  *img          = (volatile color_t *)FRAMEBUFFER;
static volatile uint32_t *framebuffer  = (volatile uint32_t *)FRAMEBUFFER;
static volatile uint32_t *led          = (volatile uint32_t *)LED_LEDS;
static volatile uint32_t *push         = (volatile uint32_t *)LED_PUSHBUTTONS;
static volatile uint64_t *timer        = (volatile uint64_t *)CLINT_TIMER;
static volatile uint64_t *timer_cmp    = (volatile uint64_t *)CLINT_TIMER_CMP;
static volatile uint32_t *timer_hi     = (volatile uint32_t *)CLINT_TIMER_HI;
static volatile uint32_t *timer_lo     = (volatile uint32_t *)CLINT_TIMER_LOW;
static volatile uint32_t *timer_cmp_hi = (volatile uint32_t *)CLINT_TIMER_CMP_HI;
static volatile uint32_t *timer_cmp_lo = (volatile uint32_t *)CLINT_TIMER_CMP_LO;

uint64_t mult(uint32_t x, uint32_t y)
{
   uint64_t res = 0;
   while (y != 0) {
      if (y & 1) {
         res += x;
      }
      x <<= 1;
      y >>= 1;
   }
   return res;
}

void timer_set(uint32_t period, uint32_t time)
{
    uint64_t now = *timer;
    *timer_cmp = now + mult(period >> 8, time);
}

void timer_wait(void)
{
    while (*timer <=  *timer_cmp);
}

void timer_set_and_wait(uint32_t period, uint32_t time)
{
    timer_set(period, time);
    timer_wait();
}

void led_set(uint32_t value)
{
    *led = value;
}

uint32_t push_button_get()
{
    return *push;
}


/*
 * main program
 * ---------------------------------------------------------------------------
 */
int main(void)
{
   /* declaration of local variables */
   uint32_t i;
   uint32_t push_state, led_state, alien_state, edge_reached;
   uint32_t n_aliens;
   object_t *spaceship, *laser;

 init:
   /* initialization stage */
   push_state = 0;             /* no button pressed at beginning */
   led_state = 0;              /* initial value displayed on leds */
   alien_state = 0;            /* state of alien in a line */
   edge_reached = 0;           /* no edge reached at beginning */
   n_aliens = N_OBJECTS - 2;   /* number of displayed aliens */
   initialize();
   laser = &object[1];         /* laser is the second declared object */
   spaceship = &object[0];     /* spaceship is the first declared object */
   spaceship->x = 20;          /* set spaceship at the middle */
   spaceship->y = 25;          /* and bottom of the screen */
#define MAX_X 39

   /* display stage */
   while (1) {
      edge_reached = 0;

      /* decrease deadline of alive objects */
      for (i = 0; i < N_OBJECTS; i++) {
         if (object[i].alive)
            object[i].deadline--;
      }

      /* display all alive objects */
      for (i = 0; i < N_OBJECTS; i++) {
         if (object[i].alive)
            display_sprite(&object[i]);
      }

      /* determine new positions of all alive objects */
      for (i = 0; i < N_OBJECTS; i++) {
         /* update object state when deadline is reached */
         if (object[i].alive && object[i].deadline == 0) {
            /* reinitialize the object deadline to period */
            object[i].deadline = object[i].period;
            /* determine new position and manage screen edges */
            object[i].x += object[i].dx;
            if (object[i].x < 0)
               object[i].x = 0;
            if (object[i].x > MAX_X)
               object[i].x = MAX_X;
            object[i].y += object[i].dy;
            /* test if an edge of the screen was reached by an alien */
            if (i >= 2 && (object[i].x == 0 || object[i].x == MAX_X))
               edge_reached = 1;
            /* store background of the next position */
            if (i > 1 && object[i].y >= spaceship->y) {
               clear_screen(blue);    /* blue screen */
               timer_set_and_wait(TIMER_FREQ, 1000);
               initialize();
            }
         }
      }

      /* test if alien is hit by an alive laser */
      if (laser->alive) {
         for (i = 2; i < N_OBJECTS; i++) {
            if (object[i].alive && laser->x == object[i].x && laser->y == object[i].y) {
               n_aliens--;
               object[i].alive = 0;
               laser->alive = 0;
               if (n_aliens == 0) {
                  /* no more aliens */
                  spaceship->alive = 0;
                  clear_screen(yellow); /* yellow screen */
                  timer_set_and_wait(TIMER_FREQ, 1000);
                  clear_screen(red);    /* red screen */
               } else {
                  display_sprite(&object[i]);
                  display_sprite(laser);
               }
            }
         }
      }

      /* when an alien reaches a screen edge, the group of aliens is moved */
      if (edge_reached) {
         for (i = 2; i < N_OBJECTS; i++) {
            object[i].dx = state[alien_state].dx;
            object[i].dy = state[alien_state].dy;
         }
         alien_state = state[alien_state].next_state;
      }

      /* laser disappears when it reaches the screen top */
      if (laser->alive && laser->y == 0) {
         laser->alive = 0;
         display_sprite(laser);
      }

      /* manage push buttons */
      push_state = push_button_get();
      // if we won, press fire to restart
      if ((n_aliens == 0)
          && (push_state & 0x4)) {
         goto init;
      }
      if ((spaceship->deadline == 1)
          || (n_aliens == 0)) {
         spaceship->dx = 0;
         if (push_state & 0x1)
            /* to the right */
            spaceship->dx = 1;
         if (push_state & 0x2)
            /* to the left */
            spaceship->dx = -1;
         if (push_state & 0x4) {
            /* fire a laser */
            if (!laser->alive) {
               laser->alive = 1;
               laser->dx = 0;
               laser->dy = -1;
               laser->x = spaceship->x;
               laser->y = spaceship->y - 1;
               laser->deadline = laser->period;
            }
         }
      }

      /* manage leds' state */
      led_set(led_state);
      led_state++;
      timer_set_and_wait(TIMER_FREQ, 4);
   }
}

/*
 * definition of functions
 * ---------------------------------------------------------------------------
 */

/* function to read a pixel from a (x,y) position of video framebuffer */
color_t read_pixel(uint32_t x, uint32_t y)
{
   uint32_t real_y = y * DISPLAY_WIDTH * DISPLAY_SCALE;
   uint32_t real_x = x * DISPLAY_SCALE;
   return img[real_y + real_x];
}

void write_pixel_scaling(color_t pixel, uint32_t x, uint32_t y)
{
   for (int i = 0; i < DISPLAY_SCALE; ++i) {
      for (int j = 0; j < DISPLAY_SCALE; ++j) {
         uint32_t real_y = y * DISPLAY_SCALE + i;
         uint32_t real_x = x * DISPLAY_SCALE + j;

         img[real_y * DISPLAY_WIDTH + real_x] = pixel;
      }
   }
}

void memsetw(volatile uint32_t* dest, uint32_t c, uint32_t n)
{
   volatile uint32_t *p = dest;
   while (n-- > 0) {
      *(volatile uint32_t *)dest++ = c;
   }
}

void clear_screen(color_t color)
{
   pixels_t p;
   p.pixel[0] = color;
   p.pixel[1] = color;
   p.pixel[2] = color;
   p.pixel[3] = color;
   memsetw(framebuffer, p.pixels, DISPLAY_SIZE / 4);
}

/* function to initialize all objects */
void initialize()
{
   uint32_t i, dx, dy;
   clear_screen(black);           /* black screen */
   for (i = 0; i < N_OBJECTS; i++) {
      if (i == 1) {
         /* laser */
         object[i].alive = 0;
         object[i].period = 1;
      } else {
         /* spaceship or aliens */
         object[i].alive = 1;
         if (i == 0)
            /* spaceship */
            object[i].period = 3;
         else
            /* aliens */
            object[i].period = 4;
      }
      object[i].deadline = 1;
      if (i > 1) {
         /* aliens */
         if (i > 4) {
            /* alien4 or alien5 */
            object[i].y = 3;    /* 3rd line */
            object[i].x = 6 + (i - 4) * 8;
         } else {
            /* alien1, alien2 or alien3 */
            object[i].y = 1;    /* 1st line */
            object[i].x = 10 + (i - 2) * 8;
         }
         object[i].dx = -1;
         object[i].dy = 0;
      }
      object[i].ax = -1;
      object[i].ay = -1;

      /* initialization of object background considering the last one */
      for (dx = 0; dx < 8; dx++)
         for (dy = 0; dy < 8; dy++)
            object[i].bg[dx][dy] = black;
   }
}

/* function to display the 8 pixels of a pattern line */
void display_pattern_line(uint8_t m, uint32_t x, uint32_t y, color_t color)
{
   for (int i = 0; i < 8; i++) {
      color_t new_color = (m & 1) == 1 ? color : black;
      m >>= 1;
      write_pixel_scaling(new_color, x + i, y);
   }
}

/* function to display an 8x8 object considering the last background */
void display_pattern(uint8_t pattern[8], uint32_t x, uint32_t y, color_t color)
{
   for (int i = 0; i < 8; i++)
      display_pattern_line(pattern[i], x, y + i, color);
}

/* function to display an 8x8 object (spaceship, laser or alien) */
void display_sprite(object_t *object)
{
   if ((object->ax > -1 && object->ay > -1) && (object->x != object->ax || object->y != object->ay || !object->alive)) {
      for (uint32_t dx = 0; dx < 8; dx++) {
         for (uint32_t dy = 0; dy < 8; dy++) {
            write_pixel_scaling(object->bg[dx][dy], ((object->ax) << 3) + dx, ((object->ay) << 3) + dy);
            if (!object->alive)
               object->bg[dx][dy] = black;
         }
      }
   }

   object->ax = object->x;
   object->ay = object->y;

   if (object->alive)
      display_pattern(object->pattern, (object->x) << 3, (object->y) << 3, object->color);
}
