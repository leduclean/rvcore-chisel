COMMON_DIR  := common

TOCLEAN = $(filter %log %.jou %.rpt %.mmi %.dcp %.csv\
                   %.wdb vivado% updatemem% usage_statistics_webtalk%,$(wildcard *))
TOCLEAN += $(wildcard _*) .Xil out RTL build download.bit

# macro de verbosité
VERB     := 0
q         = $(if $(filter 1, $(VERB)),$1,\
				$(if $2,@echo $2 && $1 > $3 2>&1, @$1))

# macro de vérification de variables d'environnement
check_vars = $(foreach v, $1, $(if $(filter undefined, $(origin $v)),\
		if [ "$v" = "XILINX_VIVADO" ]; then \
			echo "⚠️  Tapez : source /bigsoft/Xilinx/Vivado/2019.1/settings64.sh"; \
			exit 1; \
		else \
			echo "⚠️ ⚡⚡ Variable d'environnement $v manquante ⚡⚡⚠️ "; \
			$(MAKE) --no-print-directory help; \
			exit 1; \
		fi;))


all: help

help:
	@awk 'BEGIN {FS = ":.*##!"; printf "Usage: make \033[32m<commande>\033[0m \
	[PROG=<nom du fichier de test sans extension>] \
	[CYCLES=durée de la simulation] [VERB=1 pour la verbosité]\
	\nCommandes par \033[36mcatégories :\n"} \
	/^[a-zA-Z0-9_-]+:.*##!/ { printf "  \033[32m%-15s\033[0m %s\n", $$1, $$2 } \
	/^##@/ { printf "\n\033[36m%s\033[0m\n", substr($$0, 5) }' $(MAKEFILE_LIST)


.PHONY: synthese fpga clean compile autotest autotest-filter simulation

###############################################################################


## Partie Compilation

BENCH_DIR := bench
MEM_DIR := $(BENCH_DIR)/mem
SRC_SW := $(wildcard $(BENCH_DIR)/*.s)
SRC_SW += $(wildcard $(BENCH_DIR)/*.c) # Contient tous les .s et .c du projet
MEM = $(addprefix $(MEM_DIR)/, $(addsuffix .mem, $(basename $(notdir $(SRC_SW))))) # Tous les .mem du projet
PREFIX  := $(shell command -v riscv64-unknown-elf-gcc >/dev/null 2>&1 && echo riscv64-unknown-elf- || echo /matieres/3MMFMN/riscv32/bin/riscv32-unknown-elf-)
CC      := $(PREFIX)gcc
OBJDUMP := $(PREFIX)objdump
OBTOMEM := common/objtomem.awk

## Flags
ASFLAGS       :=-march=rv32i -mabi=ilp32 -ffreestanding -nostdlib -T $(BENCH_DIR)/link.ld
CFLAGS        :=$(ASFLAGS)
ELFFLAGS      :=$(ASFLAGS) $(MEM_DIR)/crt.o -Os -fno-unroll-loops
ODFLAGS       :=-j .text -j .rodata -j .data -s

.PRECIOUS: $(MEM_DIR)/%.elf

compile: $(MEM)
$(MEM_DIR):
	@mkdir -p $@
$(MEM_DIR)/crt.o: $(BENCH_DIR)/crt.S |$(MEM_DIR)
	$(call q,\
	$(CC) $(CFLAGS) -c $< -o $@ , "  Compilation crt",$@.log)
$(MEM_DIR)/%.elf: $(BENCH_DIR)/%.c $(MEM_DIR)/crt.o|$(MEM_DIR)
	$(call q,\
	$(CC) $(ELFFLAGS) $< -o $@ , "  Compilation $<",$@.log)
$(MEM_DIR)/%.elf: $(BENCH_DIR)/%.s |$(MEM_DIR)
	$(call q,\
	$(CC) $(CFLAGS) -o $@ $<, "  Compilation $<",$@.log)
$(MEM_DIR)/%.mem: $(MEM_DIR)/%.elf |$(MEM_DIR)
	$(call q,\
	$(OBJDUMP) $(ODFLAGS) $< | awk -f $(OBTOMEM) | awk -f common/fix_mem.awk > $@)


###############################################################################


##@ Simulation

TOP         := ZzTop
TOP_REP     := projet
PATH_SRC    := src/main/scala/$(TOP_REP)
TOP_TEST    := $(addsuffix Spec, $(TOP))

CYCLES=500
# Pour filtrer les tests dans une simulation
ifdef PROG
	FILTRE_TEST:= -- -z "$(PROG)"
endif

mill:
	@curl -L https://raw.githubusercontent.com/lefou/millw/0.4.11/millw > mill
	@chmod +x mill

autotest: mill compile ##! Lance la simulation automatique pour tous les tests ou juste celui fourni dans PROG
	@PROOT=$(PWD) ./mill TPchisel.test.testOnly $(TOP_REP).$(TOP_TEST) $(FILTRE_TEST)

autotest-filter:  mill ##! Lance la simuation sur les tests spécifiés dans `ci-tests.txt`
	@test -d bench/mem || { echo "bench/mem missing, run make compile first"; exit 1; }
	@mkdir -p ci-logs
ifeq ($(CI_RUN),1) ## On évite la dépendance de compilation imposée par make autotest en CI car déjà faite lors du build
	@echo "Running in CI mode"
	@xargs -I {} sh -c ' \
	echo "Running {}"; \
	PROOT=$$PWD ./mill TPchisel.test.testOnly $(TOP_REP).$(TOP_TEST) -- -z "{}" > ci-logs/{}.log 2>&1; \
	if [ $$? -eq 0 ]; then \
		echo "{} OK"; \
	else \
		echo "❌ {} failed (see ci-logs/{}.log)"; \
		exit 1; \
	fi' < ci-tests.txt
else
	@echo "Running in local mode"
	@xargs -I {} sh -c 'echo "Running {}" && make autotest PROG="{}"' < ci-tests.txt
endif

simulation: mill compile ##! Lance gtkwave sur le test fourni dans PROG
	@$(call check_vars,PROG)
	@PROOT=$(PWD) PROG=$(PROG) CYCLES=$(CYCLES) ./mill TPchisel.test.testOnly $(TOP_REP).$(TOP_TEST) -- -DemitVcd=1 -z "gtkwave"
	@gtkwave -a common/config.gtkw ./build/chiselsim/ZzTopSpec/Simulation-pour-gtkwave/workdir-verilator/trace.vcd


###############################################################################


##@ Passage sur carte

SRC         := $(wildcard $(PATH_SRC)/*.scala)
SRC         += $(wildcard src/main/resources/vsrc/*.v)
FPGA	    := xc7z010-clg400-1
SRC_SV      := RTL/$(TOP_REP).$(TOP)/$(TOP).sv


XILINX_PREFIX := $(XILINX_VIVADO)/bin/
MEM_FILE:= $(MEM_DIR)/$(PROG).mem
MMI_FILE:=$(TOP).mmi
MEM_ID=$(shell grep -oP 'InstPath="\K[^"]+' $(MMI_FILE))
PARAM = $(MEM_FILE) false # Variable pour passer des parametres au générateur SV

.PRECIOUS: golden.bit
# Créer un hash unique de la variable
PROG_HASH := $(shell echo "$(PROG)" | md5sum | cut -d' ' -f1)

RTL: $(SRC_SV) ##! Génération du SystemVerilog

$(SRC_SV): mill $(SRC)
	$(call q, ./mill TPchisel.runMain common.EmitModule $(TOP_REP).$(TOP) $(PARAM) \
	, "  Génération du SystemVerilog",/dev/null)

$(TOP)_utilization.rpt $(TOP)_timing.rpt $(TOP)_summary.rpt: $(COMMON_DIR)/synthese.vivado.tcl RTL
	@$(call check_vars,XILINX_VIVADO)
	$(call q, \
	    $(XILINX_PREFIX)vivado -nolog -nojournal -mode batch -source $< -tclargs $(FPGA) $(TOP) $(TOP_REP)\
	    , "  Synthese",$@.log)

synthese: $(TOP)_utilization.rpt $(TOP)_timing.rpt $(TOP)_summary.rpt  ##! Réalise la synthèse grossière pour évaluer les performances
	@echo "Rapport d'utilisation disponible dans $(TOP)_utilization.rpt"
	@echo "Rapport de timing disponible dans $(TOP)_timing.rpt"
	@python3 $(COMMON_DIR)/parse_report.py $^

golden.bit: $(COMMON_DIR)/bitstream.vivado.tcl $(COMMON_DIR)/$(TOP).xdc RTL
	@$(call check_vars,XILINX_VIVADO PROG)
	$(call q, \
	    $(XILINX_PREFIX)vivado -nolog -nojournal -mode batch -source $< -tclargs $(FPGA) $(TOP) $(TOP_REP) $@ $(MEM_FILE) \
	    , "  Génération du bitstream",$@.log)

.prog_$(PROG_HASH):
	@rm -f .prog_*  # Supprimer les anciens marqueurs
	@touch $@

download.bit: golden.bit $(MEM_FILE) $(MMI_FILE) .prog_$(PROG_HASH)
	@$(call check_vars,XILINX_VIVADO PROG)
	$(call q, $(XILINX_PREFIX)updatemem -debug -force --meminfo $(MMI_FILE) --data $(MEM_FILE) --proc $(MEM_ID) --bit $< --out $@\
	    , "  Mise à jour du fichier du bitstream", $@.mem.log)

check_usb:
	@lsusb | grep -q FT2232C || \
	( echo "❌ Carte Digilent non détectée (FT2232C manquante)" && \
	  echo "   → Vérifiez le câble USB, la carte ou changez de port." && \
	  exit 1 )
check_djtgcfg:
	@command -v djtgcfg >/dev/null 2>&1 || \
	( echo "❌ Outil djtgcfg introuvable." && \
	  echo "   → Changez de machine ou rebootez !" && \
	  exit 1 )
check_driver: check_djtgcfg
	@djtgcfg enum | grep -q "Digilent Zybo" || \
	( echo "❌ Aucun périphérique détecté par djtgcfg." && \
	  echo "   → Essayez un autre port USB ou vérifiez le câble." && \
	  exit 1 )

check_hw: check_usb check_driver

fpga: download.bit $(COMMON_DIR)/programFPGA.vivado.tcl check_hw ##! Génére le fichier de configuration du FPGA et le programme
	@$(call check_vars,XILINX_VIVADO)
	$(call q, \
	    $(XILINX_PREFIX)vivado -nolog -nojournal -mode batch -source $(COMMON_DIR)/programFPGA.vivado.tcl -nolog -nojournal \
	                           -tclargs $< \
	    , "  Programmation",prog.log )

##@ Nettoyage
TMPREP = /scratch/$(LOGNAME)
clean: ##! Fais le nettoyage
	$(call q, rm -rf $(TOCLEAN),)
	@if [ -e $(TMPREP) ] && [ ! -d $(TMPREP) ]; then rm -f $(TMPREP) ;fi
