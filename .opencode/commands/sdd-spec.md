---
description: SDD · Entrevista y genera la spec (uso - /sdd-spec 002-nombre idea inicial)
agent: coordinator
---
NO escribas código en ningún momento. Usa la skill sdd. Esto es solo la fase 1 del
flujo (la spec): cuando esté generada, enséñamela y PARA.
Carpeta de la spec: specs/$1/ (usa exactamente ese nombre). Si specs/$1/spec.md ya
existe, PARA y dímelo (para cambiar una spec existente usa /sdd-change).
Idea inicial: $2
Tu trabajo:
1. Pide a @planner que lea docs/constitution.md, AGENTS.md y MEMORY.md y, si la idea
es ambigua, que devuelva sus preguntas. Hazmelas de UNA en UNA (casos límite, errores,
qué queda fuera de esta versión). Máximo 5 preguntas.
2. Con mis respuestas, @planner genera specs/$1/spec.md siguiendo la plantilla de la
skill sdd, con los requisitos en EARS y "Estado: borrador".
3. Solo el QUÉ y el POR QUÉ: nada de stack, arquitectura ni nombres de archivos.