#include "fmnsoc.h"

struct color_t {
   uint8_t b:3;
   uint8_t g:3;
   uint8_t r:2;
} __attribute__((packed));

typedef struct color_t color_t;

static volatile color_t *framebuffer  = (volatile color_t *)FRAMEBUFFER;
static volatile uint32_t *led          = (volatile uint32_t *)LED_LEDS;
static volatile uint32_t *push_irq     = (volatile uint32_t *)LED_IRQ_BUTTON_STATE;
static volatile uint64_t *timer        = (volatile uint64_t *)CLINT_TIMER;
static volatile uint64_t *timer_cmp    = (volatile uint64_t *)CLINT_TIMER_CMP;
static volatile uint32_t *timer_hi     = (volatile uint32_t *)CLINT_TIMER_HI;
static volatile uint32_t *timer_lo     = (volatile uint32_t *)CLINT_TIMER_LOW;
static volatile uint32_t *timer_cmp_hi = (volatile uint32_t *)CLINT_TIMER_CMP_HI;
static volatile uint32_t *timer_cmp_lo = (volatile uint32_t *)CLINT_TIMER_CMP_LO;
static volatile uint64_t *source       = (volatile uint64_t *)PLIC_SOURCE;
static volatile uint64_t *pending      = (volatile uint64_t *)PLIC_PENDING;
static volatile uint32_t *enable       = (volatile uint32_t *)PLIC_ENABLE;
static volatile uint32_t *target       = (volatile uint32_t *)PLIC_TARGET;
static volatile uint32_t *claim        = (volatile uint32_t *)PLIC_IRQ_CLAIM;

#if 1
#define R(x) (((x & 0xf0) >> 4) | ((x & 0x0f) << 4))
#else
#define R(x) (x)
#endif

static const color_t green = {0, 7, 0};
static const color_t red = {0, 0, 3};
static const color_t black = {0, 0, 0};
static const color_t white = {7, 7, 3};

void
led_set (value)
uint32_t value;
{
    *led = value;
}

void write_pixel_scaling(color_t pixel, uint32_t x, uint32_t y)
{
   for (int i = 0; i < DISPLAY_SCALE; ++i) {
      for (int j = 0; j < DISPLAY_SCALE; ++j) {
         uint32_t real_y = y * DISPLAY_SCALE + i;
         uint32_t real_x = x * DISPLAY_SCALE + j;

         framebuffer[real_y * DISPLAY_WIDTH + real_x] = pixel;
      }
   }
}

void display_pattern_line(uint8_t m, uint32_t x, uint32_t y, color_t color)
{
   for (int i = 0; i < 8; i++) {
      color_t new_color = (m & 1) == 1 ? color : black;
      m >>= 1;
      write_pixel_scaling(new_color, x + i, y);
   }
}

void display_pattern(uint8_t pattern[8], uint32_t x, uint32_t y, color_t color)
{
   for (int i = 0; i < 8; i++)
      display_pattern_line(pattern[i], x, y + i, color);
}

#define SECOND (10000000)
#define DELAY (SECOND / 20)

/* morse sequence - ti is 0 and ta is 1*/
static uint8_t sequence = 0;
/* sequence length - used to compute which bit is to be written
 *  and how to read the sequence
 */
static uint8_t sequence_l = 0;

static uint32_t x = 0;
static uint32_t y = 0;

#define BACKSPACE (0b10100000)
#define LEFT  (0b10101000)
#define RIGHT (0b10100001)
#define UP    (0b10100010)
#define DOWN  (0b10100100)
#define CLEAR (0b11111111)

static uint8_t morse[256] = {
    /* LLLCCCCC */
    [0b01000010] = 'A',
    [0b10000001] = 'B',
    [0b10000101] = 'C',
    [0b01100001] = 'D',
    [0b00100000] = 'E',
    [0b10000100] = 'F',
    [0b01100011] = 'G',
    [0b10000000] = 'H',
    [0b01000000] = 'I',
    [0b10001110] = 'J',
    [0b01100101] = 'K',
    [0b10000010] = 'L',
    [0b01000011] = 'M',
    [0b01000001] = 'N',
    [0b01100111] = 'O',
    [0b10000110] = 'P',
    [0b10001011] = 'Q',
    [0b01100010] = 'R',
    [0b01100000] = 'S',
    [0b00100001] = 'T',
    [0b01100100] = 'U',
    [0b10001000] = 'V',
    [0b01100110] = 'W',
    [0b10001001] = 'X',
    [0b10001101] = 'Y',
    [0b10000011] = 'Z',

    [0b10111111] = ' ', /* ----- is space */
    [BACKSPACE] = '\b', /* ..... is backspace */
    [LEFT] = '<',
    [RIGHT] = '>',
    [UP] = '^',
    [DOWN] = ',',
    [CLEAR] = '\b',
};

#define LETTER_HEIGHT (8)
#define LETTER_WIDTH (8)

static uint8_t letters[128][8] = {
    [' '] = {
        R(0b00000000),
        R(0b00000000),
        R(0b00000000),
        R(0b00000000),
        R(0b00000000),
        R(0b00000000),
        R(0b00000000),
        R(0b00000000),
    },

    ['A'] = {
        R(0b00111100),
        R(0b01100110),
        R(0b11000011),
        R(0b11000011),
        R(0b11111111),
        R(0b11000011),
        R(0b11000011),
        R(0b00000000),
    },
    
    ['B'] = {
        R(0b01111100),
        R(0b01100110),
        R(0b01100110),
        R(0b01111110),
        R(0b01100110),
        R(0b01100110),
        R(0b01111100),
        R(0b00000000),
    },
    
    ['C'] = {
        R(0b00111110),
        R(0b01100000),
        R(0b11000000),
        R(0b11000000),
        R(0b11000000),
        R(0b01100000),
        R(0b00111110),
        R(0b00000000),
    },
    
    ['D'] = {
        R(0b01111100),
        R(0b01100110),
        R(0b01100011),
        R(0b01100011),
        R(0b01100011),
        R(0b01100110),
        R(0b01111100),
        R(0b00000000),
    },
    
    ['E'] = {
        R(0b01111111),
        R(0b01100000),
        R(0b01100000),
        R(0b01111100),
        R(0b01100000),
        R(0b01100000),
        R(0b01111111),
        R(0b00000000),
    },
    
    ['F'] = {
        R(0b01111110),
        R(0b01100000),
        R(0b01100000),
        R(0b01111100),
        R(0b01100000),
        R(0b01100000),
        R(0b01100000),
        R(0b00000000),
    },
    
    ['G'] = {
        R(0b00111110),
        R(0b01100000),
        R(0b11000000),
        R(0b11001110),
        R(0b11000010),
        R(0b01100010),
        R(0b00111110),
        R(0b00000000),
    },
    
    ['H'] = {
        R(0b11000011),
        R(0b11000011),
        R(0b11000011),
        R(0b11111111),
        R(0b11000011),
        R(0b11000011),
        R(0b11000011),
        R(0b00000000),
    },
    
    ['I'] = {
        R(0b01111110),
        R(0b00011000),
        R(0b00011000),
        R(0b00011000),
        R(0b00011000),
        R(0b00011000),
        R(0b01111110),
        R(0b00000000),
    },
    
    ['J'] = {
        R(0b00011110),
        R(0b00001100),
        R(0b00001100),
        R(0b00001100),
        R(0b11001100),
        R(0b11001100),
        R(0b01111000),
        R(0b00000000),
    },
    
    ['K'] = {
        R(0b11000110),
        R(0b11001100),
        R(0b11011000),
        R(0b11110000),
        R(0b11011000),
        R(0b11001100),
        R(0b11000110),
        R(0b00000000),
    },
    
    ['L'] = {
        R(0b01100000),
        R(0b01100000),
        R(0b01100000),
        R(0b01100000),
        R(0b01100000),
        R(0b01100000),
        R(0b01111110),
        R(0b00000000),
    },
    
    ['M'] = {
        R(0b01000010),
        R(0b01100110),
        R(0b01111110),
        R(0b01011010),
        R(0b01000010),
        R(0b01000010),
        R(0b01000010),
        R(0b00000000),
    },
    
    ['N'] = {
        R(0b01000010),
        R(0b01100010),
        R(0b01110010),
        R(0b01011010),
        R(0b01001110),
        R(0b01000110),
        R(0b01000010),
        R(0b00000000),
    },
    
    ['O'] = {
        R(0b00111100),
        R(0b01100110),
        R(0b01000010),
        R(0b01000010),
        R(0b01000010),
        R(0b01100110),
        R(0b00111100),
        R(0b00000000),
    },
    
    ['P'] = {
        R(0b01111100),
        R(0b01100110),
        R(0b01100110),
        R(0b01111100),
        R(0b01100000),
        R(0b01100000),
        R(0b01100000),
        R(0b00000000),
    },
    
    ['Q'] = {
        R(0b00111100),
        R(0b01100110),
        R(0b11000011),
        R(0b11000011),
        R(0b11011011),
        R(0b01100110),
        R(0b00111101),
        R(0b00000000),
    },
    
    ['R'] = {
        R(0b01111100),
        R(0b01100110),
        R(0b01100110),
        R(0b01111100),
        R(0b01111000),
        R(0b01101100),
        R(0b01100110),
        R(0b00000000),
    },
    
    ['S'] = {
        R(0b00111110),
        R(0b01100000),
        R(0b01100000),
        R(0b00111100),
        R(0b00000110),
        R(0b00000110),
        R(0b01111100),
        R(0b00000000),
    },
    
    ['T'] = {
        R(0b11111111),
        R(0b11011011),
        R(0b00011000),
        R(0b00011000),
        R(0b00011000),
        R(0b00011000),
        R(0b00111100),
        R(0b00000000),
    },
    
    ['U'] = {
        R(0b11000011),
        R(0b11000011),
        R(0b11000011),
        R(0b11000011),
        R(0b11000011),
        R(0b01100110),
        R(0b00111100),
        R(0b00000000),
    },

    ['V'] = {
        R(0b11000011),
        R(0b11000011),
        R(0b11000011),
        R(0b01100110),
        R(0b00111100),
        R(0b00011000),
        R(0b00011000),
        R(0b00000000),
    },
    
    ['W'] = {
        R(0b11000011),
        R(0b11000011),
        R(0b11011011),
        R(0b11111111),
        R(0b11100111),
        R(0b11000011),
        R(0b11000011),
        R(0b00000000),
    },
    
    ['X'] = {
        R(0b11000011),
        R(0b01100110),
        R(0b00111100),
        R(0b00011000),
        R(0b00111100),
        R(0b01100110),
        R(0b11000011),
        R(0b00000000),
    },

    ['Y'] = {
        R(0b11000011),
        R(0b01100110),
        R(0b00111100),
        R(0b00011000),
        R(0b00011000),
        R(0b00011000),
        R(0b00111100),
        R(0b00000000),
    },

    ['Z'] = {
        R(0b11111111),
        R(0b00000110),
        R(0b00001100),
        R(0b00011000),
        R(0b00110000),
        R(0b01100000),
        R(0b11111111),
        R(0b00000000),
    },
};

static uint8_t blank = 1;

void write_char (uint8_t);

void
redraw_screen (void)
{
    x = 0;
    y = 0;
    if (blank)
	return;

    do
    {
	blank = 1;
	write_char (' ');
    } while (x + y != 0);

    blank = 1;
}

void
write_char (uint8_t chr)
{
    color_t color = white;

    if (x + LETTER_WIDTH > DISPLAY_WIDTH)
    {
	x = 0;
	y += LETTER_HEIGHT;
    }

    if (y > DISPLAY_HEIGHT - 8)
	redraw_screen ();

    display_pattern (letters[chr], x, y, color);
    blank = 0;

    x += LETTER_WIDTH;
}

void
handler (mcause, mie, mip)
uint32_t mcause, mie, mip;
{
    /* Ce n'est pas une interruption ou la cause de l'interruption a disparue */
    if ((mcause & ~0x80000000) == 0 || (mip & mie) == 0) {
        return;
    }

    uint32_t cause = mcause & 0xf;

    if (cause == IRQ_M_EXT)
    {
	uint32_t v = *claim;
	*claim = v;

	v = *push_irq;

	switch (v)
	{
	    /* button 1 is 'confirm character' */
	case 1:
	    sequence |= sequence_l << 5;
	    if (morse[sequence])
	    {
		switch (sequence)
		{
		case BACKSPACE:
		    x -= (x > LETTER_WIDTH) ? LETTER_WIDTH : x;
		    write_char (' ');
		    x -= (x > LETTER_WIDTH) ? LETTER_WIDTH : x;
		    break;
		case CLEAR:
		    redraw_screen ();
		    break;
		case UP:
		    y -= LETTER_HEIGHT;
		    break;
		case DOWN:
		    y += LETTER_HEIGHT;
		    break;
		case LEFT:
		    x -= LETTER_WIDTH;
		    break;
		case RIGHT:
		    x += LETTER_WIDTH;
		    break;
		default:
		    write_char (morse[sequence]);
		    break;
		}
	    }
	    sequence_l = 0;
	    sequence = 0;
	    break;

	    /* button 2 is ti */
	case 2:
	    sequence &= ~(1 << sequence_l++);
	    break;

	    /* button 3 is ta */
	case 4:
	    sequence |= (1 << sequence_l++);
	    break;
	default:
	    break;
	}
    }
    else if (cause == IRQ_M_TMR)
    {
	led_set(sequence);

	*timer_cmp = *timer + DELAY;
    }
    else if (cause == IRQ_M_SWR)
    {
	__asm__ ("csrc mip, %0":: "r"(1 << IRQ_M_SWR));
    }
}

int
main (void)
{
    led_set (0);

    /* On programme le timer pour une attente d'une seconde */
    *timer_cmp = *timer + DELAY;

    /* Autorisation globale des interruptions dans mstatus */
    __asm__("csrw mstatus, %0" :: "i"(MSTATUS_MIE));
    /* Prise en compte des interruptions "machine" dans mie */
    const uint32_t irq = (1 << IRQ_M_EXT) | (1 << IRQ_M_TMR) | (1 << IRQ_M_SWR);
    __asm__("csrw mie, %0":: "r"(irq));

    /* Configuration du PLIC pour autoriser l'irq 1 (à laquelle sont reliés les boutons) */
    *enable = 1 << PLIC_IRQ_BTN;

    for (;;)
	;
}
