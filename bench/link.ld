OUTPUT_ARCH( "riscv" )

ENTRY (_start)
MEMORY
{
  insnmem   (rx) : ORIGIN = 0x0000, LENGTH = 8K
  datamem   (wai!x) : ORIGIN = 0x2000, LENGTH = 8K
}

PHDRS
{
  text PT_LOAD;
  data PT_LOAD;
  bss PT_LOAD;
}

SECTIONS
{
  PROVIDE(_start = ORIGIN(insnmem));
  .text : {
    PROVIDE(_text_start = .);
    *(.text._start) *(.text .text.*)
    PROVIDE(_text_end = .);
  } >insnmem AT>insnmem :text

  . = ORIGIN(datamem);
  .rodata ALIGN(4) : {
    PROVIDE(_rodata_start = .);
    *(.rodata .rodata.*)
    PROVIDE(_rodata_end = .);
  } >datamem AT>datamem :data

  .data ALIGN(4) : {
    PROVIDE(_data_start = .);
    *(.sdata .sdata.*) *(.data .data.*)
    PROVIDE(_data_end = .);
  } >datamem AT>datamem :data

  .bss ALIGN(4) : {
    PROVIDE(_bss_start = .);
    *(.sbss .sbss.*) *(.bss .bss.*)
    PROVIDE(_bss_end = ALIGN(4));
  } >datamem AT>datamem :bss

  PROVIDE(_memory_start = ORIGIN(datamem));
  PROVIDE(_memory_end = ORIGIN(datamem) + LENGTH(datamem));
}
