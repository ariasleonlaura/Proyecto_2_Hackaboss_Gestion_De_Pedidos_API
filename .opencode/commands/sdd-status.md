---
description: SDD · Dónde estamos - fase actual y siguiente paso de una spec
agent: plan
---
Lee specs/$1/ (spec.md, plan.md y tasks.md, los que existan) y MEMORY.md. Si specs/$1/
no existe, dilo y sugiere /sdd-spec. No modifiques ningún archivo. Si MEMORY.md y los
archivos de la spec se contradicen, fíate de los archivos.
Dime en pocas líneas:
1. En qué fase del flujo SDD está la spec y su Estado (borrador | aprobada | implementada).
2. Tareas hechas y pendientes (x de y), contando las casillas [x] y [ ] de tasks.md, y
cuál es la siguiente pendiente.
3. El siguiente paso exacto, usando solo estos comandos:
- Sin spec.md → /sdd-spec <carpeta> <idea>
- Spec en borrador → /sdd-clarify <carpeta>; después, cambiar a mano su Estado a "aprobada"
- Spec aprobada sin plan.md → /sdd-plan <carpeta>
- Plan sin tasks.md → /sdd-tasks <carpeta>
- Tareas pendientes → /sdd-implement <carpeta> <Tn>
- Todas hechas → /sdd-validate → reviewer
- Cambio de requisitos → /sdd-change <carpeta> <texto>