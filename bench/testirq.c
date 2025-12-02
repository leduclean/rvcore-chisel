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
static volatile uint32_t *push         = (volatile uint32_t *)LED_PUSHBUTTONS;
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

static uint8_t digit[10][8] = {
    {
        R(0b01111100),
        R(0b11000110),
        R(0b11001110),
        R(0b11011110),
        R(0b11110110),
        R(0b11100110),
        R(0b01111100),
        R(0b00000000),
    },
    {
        R(0b00110000),
        R(0b01110000),
        R(0b00110000),
        R(0b00110000),
        R(0b00110000),
        R(0b00110000),
        R(0b11111100),
        R(0b00000000),
    },
    {
        R(0b01111000),
        R(0b11001100),
        R(0b00001100),
        R(0b00111000),
        R(0b01100000),
        R(0b11001100),
        R(0b11111100),
        R(0b00000000),
    },
    {
        R(0b01111000),
        R(0b11001100),
        R(0b00001100),
        R(0b00111000),
        R(0b00001100),
        R(0b11001100),
        R(0b01111000),
        R(0b00000000),
    },
    {
        R(0b00011100),
        R(0b00111100),
        R(0b01101100),
        R(0b11001100),
        R(0b11111110),
        R(0b00001100),
        R(0b00011110),
        R(0b00000000),
    },
    {
        R(0b11111100),
        R(0b11000000),
        R(0b11111000),
        R(0b00001100),
        R(0b00001100),
        R(0b11001100),
        R(0b01111000),
        R(0b00000000),
    },
    {
        R(0b00111000),
        R(0b01100000),
        R(0b11000000),
        R(0b11111000),
        R(0b11001100),
        R(0b11001100),
        R(0b01111000),
        R(0b00000000),
    },
    {
        R(0b11111100),
        R(0b11001100),
        R(0b00001100),
        R(0b00011000),
        R(0b00110000),
        R(0b00110000),
        R(0b00110000),
        R(0b00000000),
    },
    {
        R(0b01111000),
        R(0b11001100),
        R(0b11001100),
        R(0b01111000),
        R(0b11001100),
        R(0b11001100),
        R(0b01111000),
        R(0b00000000),
    },
    {
        R(0b01111000),
        R(0b11001100),
        R(0b11001100),
        R(0b01111100),
        R(0b00001100),
        R(0b00011000),
        R(0b01110000),
        R(0b00000000),
    }
};

static const color_t green = {0, 7, 0};
static const color_t red = {0, 0, 3};
static const color_t black = {0, 0, 0};
static const color_t white = {7, 7, 3};

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

static inline uint32_t divu10(uint32_t n)
{
    uint32_t x = n;
    x = (x << 1) + x + 3;
    x = (x >> 4) + x;
    x = (x >> 8) + x;
    x = (x >> 16) + x;
    x = (x >> 5);
    return x;
}

static inline uint32_t mod10(uint32_t n)
{
    uint32_t x = divu10(n);
    return n - ((x << 3) + (x << 1));
}

void vga_set(int32_t cnt)
{
    int time[10];
    int digits = 0;
    color_t color;
    uint32_t value;

    if (cnt < 0) {
        color = red;
        value = -cnt;
    } else {
        color = green;
        value = cnt;
    }

    while (value > 0) {
        int c = mod10(value);
        time[digits++] = c;
        value = divu10(value);
    }

    for (uint32_t i = 116; i < 124; i++) {
        for (uint32_t j = 0; j < 320; j++) {
            write_pixel_scaling(black, j, i);
        }
    }

    if (digits == 0) {
        display_pattern(digit[0], 156, 116, color);
    } else {
        int shift = digits * 4;
        for (int i = 0; i < digits; i++) {
            display_pattern(digit[time[i]], 156 + shift - 4 - i * 8, 116, color);
        }
    }
}

#define DELAY 10000000
/* Pour mémoire les variables statiques/globales sont initialisées à 0 */
static int32_t cntinsec;
static enum {OP_PLUS, OP_MINUS} op;

void led_set(uint32_t value)
{
    *led = value;
}

void handler(uint32_t mcause, uint32_t mie, uint32_t mip)
{
    /* Ce n'est pas une interruption ou la cause de l'interruption a disparue */
    if ((mcause & ~0x80000000) == 0 || (mip & mie) == 0) {
        return;
    }
    /*
     * 3 causes possibles : externe, software, timer, dans
     * l'ordre des priorités
     */
    uint32_t cause = mcause & 0xf;
    if (cause == IRQ_M_EXT) {
        uint32_t v = *claim;
        *claim = v;
        v = *push_irq;
        if (v == 1) {
            cntinsec = 0;
        } else if (v == 2) {
            op = OP_PLUS;
        } else if (v == 4) {
            op = OP_MINUS;
        }
    } else if (cause == IRQ_M_TMR) {
        led_set(cntinsec);
        vga_set(cntinsec);
        *timer_cmp = *timer + DELAY; /* 1 seconde */
        if (op == OP_PLUS) {
            cntinsec++;
        }
        if (op == OP_MINUS) {
            cntinsec--;
        }
    } else if (cause == IRQ_M_SWR) {
        __asm__("csrc mip, %0":: "r"(1 << IRQ_M_SWR));
    }
}

int main(void)
{
    /* On affiche rien au début */
    led_set(0);
    /* On programme le timer pour une attente d'une seconde */
    *timer_cmp = *timer + DELAY;
    /* Autorisation globale des interruptions dans mstatus */
    __asm__("csrw mstatus, %0" :: "i"(MSTATUS_MIE));
    /* Prise en compte des interruptions "machine" dans mie */
    const uint32_t irq = (1 << IRQ_M_EXT) | (1 << IRQ_M_TMR) | (1 << IRQ_M_SWR);
    __asm__("csrw mie, %0":: "r"(irq));

    /* Configuration du PLIC pour autoriser l'irq 1 (à laquelle sont reliés les boutons) */
    *enable = 1 << PLIC_IRQ_BTN;

    /* Le timer est lancé dès le démarrage */
    while (1);
}
