---
description: SDD · Valida la implementación de una spec RF por RF (uso - /sdd-validate 002-nombre)
agent: reviewer
---
Valida specs/$1/ según tu modo "validar la implementación": recorre spec.md RF por RF,
con sus tests y, en los RF de interfaz, la verificación con Chrome DevTools (incluida la
vista móvil de 375 px). Si specs/$1/ no existe, PARA y dímelo. NO arregles nada.
Termina con tu veredicto en el formato habitual (APROBADO / CAMBIOS NECESARIOS).