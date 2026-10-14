# Pendências do port Thaumcraft 6 → NeoForge 1.21.1

Atualizado em 2026-10-12. Repositório git só local (sem remote). Branch principal: `master`.

## Estado do master (commit 91381b9)

- Build ok, `runGameTestServer`: 90 testes passando.
- Jar + Curios 9.5.1 copiados para `~/Downloads/mod/` (Curios agora é dependência obrigatória).
- Marcos prontos: 1–6 (aspectos, worldgen, aura, pesquisa, Bancada Arcana, Crisol), 7 Essentia,
  8 Infusão, 9 Manoplas/Focos, 10 Ferramentas/Armaduras/Acessórios (Curios), Dispositivos.
- Scripts de geração, em ordem: `gen_marco2.py`* … `gen_marco6.py`, `gen_marco7.py`, `gen_marco8.py`,
  `gen_marco9.py`, `gen_marco10.py` (depois do 8), `gen_devices.py`.
  *`gen_marco2.py` sobrescreve `mineable/axe.json`, `mineable/pickaxe.json` e `aspects/items/thaumcraft.json`:
  se rodar, rode os seguintes depois.
- Regras para agentes em paralelo: `tools/AGENT_BRIEF.md`.

## Trabalho parcial parado (não está no master)

Agentes interrompidos em 2026-10-12; o trabalho foi salvo como commit WIP no branch de cada um
(worktrees em `.claude/worktrees/`). Não compilam/testam necessariamente.

| Branch | Commit | Conteúdo |
|---|---|---|
| `worktree-agent-ac8663502e1776cbd` | 4a7fbdf | Marco 12 Fluxo/Taint — ~132 arquivos, o mais adiantado |
| `worktree-agent-ad7581b5df62a8d21` | f1f7a51 | Marco 11 Golems — início (6 arquivos) |
| `worktree-agent-a81613e9989856664` | 3c2baae | Marco 13 Mobs e mundo — início (13 arquivos) |

Para retomar: continuar no branch (ou relançar o agente apontando para ele), `git merge master`,
build + gametests, commit, e só então merge no master.

## Marcos que faltam

1. **Marco 12 — Fluxo e Taint** (parcial no branch acima): efeitos de poção (flux_taint, vis_exhaust,
   infectious_vis_exhaust, warp ward, etc.), fluidos (gosma de fluxo, morte líquida, fluido purificante +
   baldes), fendas de fluxo (gatilho `AuraHandler.RIFT_TRIGGER`, estabilizador acalmando fendas), blocos de
   taint e espalhamento, criaturas do taint, garrafa de taint, condensador, sifão do vazio, sabão da
   sanidade, sais de banho, colapsador de causalidade. Também trocar os stubs da infusão
   (`InfusionMatrixBlockEntity`: gosma de fluxo nos eventos 1/3, flux taint e vis exhaust em `inEvHarm`).
2. **Marco 11 — Golems**: entidade golem e partes, selos (todos + avançados) e GUI, logística/tarefas,
   prensa de golem (multibloco MINDCLOCKWORK), sino do golemante, placeholders.
3. **Marco 13 — Mobs e mundo**: cultistas (drops das armaduras/lâmina já registradas), pech (+ troca),
   wisps, morcegos de fogo, zumbis cérebro, slime táumico, aranha mental; perfuradora arcana e torretas;
   sacos de loot, pérola primordial, guloseima tripla de carne, pedaços de carne; biomas Floresta Mágica e
   Sinistro, montes, estruturas de cultistas; curiosidades no drop de âmbar.
4. **Marco 14 — Eldritch**: dimensão Outer Lands e labirintos, eventos de distorção (usar
   `GearEvents.getWarpFromGear`), mobs eldritch e chefes, pedra porosa, bloco "hole" (foco de fenda).
5. **Fechamento**: resolver as pendências abaixo, teste visual completo no cliente, teste com o modpack
   (Create, Aeronautics, MineColonies) e jar final.

## Pendências dentro dos marcos prontos

### Essentia / Infusão (7–8)
- Fluxo de essentia sem fio desenhado com partículas, não com o tubo GLE do original.
- Bicos do alambique (ModelBoreBase) não renderizados.
- Eventos de instabilidade da infusão usam stubs (fluxo na aura, veneno/fraqueza) até o Marco 12.

### Manoplas e focos (9)
- Foco de fenda só toca o som de falha (precisa do bloco "hole", Marco 14).
- Maldição sem a imunidade dos mobs eldritch (Marco 14).
- Tecla G para orientação da pá elemental não ligada.
- Seleção radial mostra só um dos dois anéis giratórios; moldura do foco de plano parece elevada (verificar).
- Raio desenhado como fita plana, não tubo 3D.

### Ferramentas / armaduras / acessórios (10)
- Lâmina carmesim e armaduras de cultista só registradas (dependem dos cultistas, Marco 13).
- Efeitos flux taint / vis exhaust procurados por id: só funcionam quando o Marco 12 registrá-los.
- Receitas esperando itens: máscara do diabo sorridente (`brain` já existe agora — conferir se ativou),
  amuleto verdejante de sustento (`triple_meat_treat`, Marco 13).
- HUD do medidor de recarga não portado (método vazio no original); receitas "fantasma" rúnicas do livro.

### Dispositivos
- Spa só com fluido comum até existirem `purifying_fluid` e `bath_salts` (Marco 12; manter esses ids).
- Bônus da Fornalha Infernal para pepitas de estanho/prata/chumbo e pedaços de carne ausentes.
- Caixas/urnas de loot sem pérola primordial e sem o sorteio de equipamento.
- Efeito do espelho usa o shader do portal do End.
- Nada dos dispositivos foi visto no cliente ainda (modelos, renderizadores, GUIs) — conferir no teste visual.

### Geral
- `README.md` desatualizado de propósito (pedido do usuário para não mexer).
- Teste de fumaça do cliente: mundo `run/saves/tc_test`, jogador deve começar em (-6.5, -60, 0.5) olhando a
  parede de ouro; um teste que trava deixa o jogador teleportado (corrigir o level.dat).
- Limite prático: ~3 builds Gradle simultâneos (22 GB de RAM).
