---
description: SDD · Revisa la spec como un QA (solo detecta, no resuelve)
agent: plan
---
Revisa specs/$1/spec.md como si fueras un QA muy profesional. Usa la skill sdd.
Si specs/$1/ no existe, PARA y dímelo. No modifiques ningún archivo.
Enumera, indicando el RF afectado en cada punto:
1. Ambigüedades restantes (requisitos que no se pueden verificar).
2. Contradicciones entre requisitos.
3. Casos límite no cubiertos.
4. Conflictos con docs/constitution.md.
5. Dudas abiertas ([NECESITA ACLARACIÓN]) sin resolver.
Si una categoría no tiene hallazgos, escribe "Ninguno": no inventes problemas.
No propongas soluciones todavía: solo detecta. Formato: lista numerada.