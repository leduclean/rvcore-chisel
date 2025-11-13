# expected: 00000000,FFFFF000,12345000
	.text

	lui x31, 0       #Test chargement d'une valeur nulle
	lui x31, 0xfffff #Test chargement d'une valeur maximal sur 20 bits
	lui x31, 0x12345 #Test chargement d'une valeur quelconque