#ifndef __FMNSOC_H__
#define __FMNSOC_H__

#include <stdint.h>

// Leds et boutons
// Même adresse, leds en écriture, boutons en lecture
#define LED_LEDS                        0x30000000
#define LED_PUSHBUTTONS                 0x30000000

// Vga sans configuration possible
#define FRAMEBUFFER                     0x80000000

// CLint pour le timer
#define CLINT_MSIP                      0x02000000
#define CLINT_TIMER_CMP                 0x02004000
#define CLINT_TIMER_CMP_HI              0x02004004
#define CLINT_TIMER_CMP_LO              0x02004000
#define CLINT_TIMER                     0x0200bff8
#define CLINT_TIMER_HI                  0x0200bffc
#define CLINT_TIMER_LOW                 0x0200bff8

// PLIC (Platform-Level Interrupt Controller)
#define PLIC_BASE       0x0c000000
#define PLIC_SOURCE     PLIC_BASE
#define PLIC_PENDING    (PLIC_BASE + 0x1000)
#define PLIC_ENABLE     (PLIC_BASE + 0x2000)
#define PLIC_TARGET     (PLIC_BASE + 0x3000)
#define PLIC_IRQ_CLAIM  (PLIC_BASE + 0x200004)

// IRQ Machine-level
#define IRQ_M_EXT 11
#define IRQ_M_TMR 7
#define IRQ_M_SWR 3

// MSTATUS bits
#define MSTATUS_MIE 0x00000008

// Interruption bouton
#define PLIC_IRQ_BTN 1


// Vga 320x200, 8 bits par pixels
#define DISPLAY_WIDTH                   320
#define DISPLAY_HEIGHT                  240
#define DISPLAY_SIZE                    (DISPLAY_WIDTH * DISPLAY_HEIGHT)
#define DISPLAY_SCALE                   1

#define TIMER_FREQ                      10000000 // 10MHz
#define TIMER_RATIO                     200

void timer_set(uint32_t period, uint32_t start_value);
void timer_wait(void);
void timer_set_and_wait(uint32_t period, uint32_t time);
void led_set(uint32_t value);
uint32_t push_button_get();

#endif // __FMNSOC_H__
