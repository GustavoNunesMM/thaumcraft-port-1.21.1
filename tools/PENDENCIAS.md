# Pendências do port Thaumcraft 6 → NeoForge 1.21.1

Atualizado em 2026-10-14. Repositório git só local (sem remote). Branch principal: `main`.

## Estado atual

- Todos os marcos estão no `main`: 1–6 (aspectos, worldgen, aura, pesquisa, Bancada Arcana, Crisol), 7 Essentia,
  8 Infusão, 9 Manoplas/Focos, 10 Ferramentas/Armaduras/Acessórios (Curios), Dispositivos, 11 Golems,
  12 Fluxo/Taint, 13 Mobs e mundo, 14 Eldritch, mais a correção da auditoria (branch `worktree-agent-a3fa665000778d4cb`).
- Build ok, `runGameTestServer`: 153 testes passando.
- Curios 9.5.1 é dependência obrigatória.
- Scripts de geração, em ordem: `gen_marco4.py` … `gen_marco10.py`, `gen_devices.py`, `gen_marco12.py`,
  `gen_golems.py`, `gen_marco13.py` (roda `gen_marco13_world.py`), `gen_marco14.py`, `gen_fixes.py` (sempre por último).
  `gen_marco2.py` sobrescreve tags/aspectos compartilhados: não rodar (ou rodar todos os seguintes depois).
  `mound.nbt` é reescrito (não determinístico) a cada execução do `gen_marco13`: dar `git checkout` se for a única mudança.
  As pesquisas vêm de `tools/convert_research.py` (mapeia ids de ícones/itens do 1.12).
- Compatibilidade com shader packs (Iris/Oculus): `client/compat/ShaderCompat` (detecção reflexiva, sem dependência);
  `./gradlew runClient -PtcIris` coloca Sodium + Iris no runtime de desenvolvimento.
- Regras para agentes em paralelo: `tools/AGENT_BRIEF.md`.

## Feito na correção da auditoria (2026-10-14)

- Gatilhos de escaneamento que faltavam: `f_VOIDSEED` (destrava UNLOCKELDRITCH e toda a categoria Eldritch), `f_BRAIN`,
  `PRIMPEARL`, `!Pechwand`, `!FluxRift`, `f_toomuchflux`, criaturas do taint, `f_BAT`/`f_FLY` dos mobs do mod,
  `f_golem` para golems; popup de pesquisa FLUX do taumômetro com fluxo alto.
- Receitas: 16 tinturas de nitor (`nitorgroup`), purificação de cobre (estanho/prata/chumbo quando algum mod adiciona
  o minério), interruptor da orelha arcana, bloco de carne; grupos `brass_stuff`, `thaumium_stuff`, `void_stuff`,
  `tallowcandles`, `jarlabels`, `inkwell` (o livro resolve um id de receita como receita, grupo ou receita "fake").
- Itens novos: gancho arcano (arma, ponta, carretel, entidade e renderizador), trilho ativador arcano, esponja de
  fluxo criativa, item do taumatório, pepitas e aglomerados de estanho/prata/chumbo.
- Thaumonomicon: páginas de infusão (não existiam), receitas "fake" (`ie*fake`, `runicarmorfake0-2`), páginas de
  multibloco com vista isométrica e botão de camadas (fornalha infernal, três altares, taumatório, prensa de golem);
  ícones de pesquisa corrigidos (torretas, perfuradora, amuleto de vis).
- Stubs ligados: fenda de fluxo gera wisp, gosma de fluxo gera slimes táumicos e eles crescem comendo a gosma, slime
  táumico é ITaintedMob, aranha mental é IEldritchMob, runas do taumômetro, partículas da folha-brilhante, bicos do
  alambique.
- Campeões: os 14 modificadores, sorteio no spawn (config `allowChampionMobs`), nomes/títulos (inclusive dos chefes),
  sacos de loot e XP extra, campeões corrompidos (taint) com IA de caça, tentáculo gigante sempre campeão.
- `/summon thaumcraft:golem` sem NBT agora cria um golem padrão (madeira, partes básicas) em vez de sumir.

## Marco 14 — Eldritch (pendências)

- As Terras Exteriores (Outer Lands) são um design original do port: o TC6 nunca implementou a dimensão nem os
  labirintos. Precisam de teste de jogo (dificuldade, loot, chefes) e provavelmente de ajuste.
- Nada da dimensão foi visto com calma no cliente ainda (geração, salas, portal, obeliscos).

## Pendências por área

### Essentia / Infusão
- Fluxo de essentia sem fio desenhado com partículas, não com o tubo GLE do original.

### Manoplas e focos
- Seleção radial mostra só um dos dois anéis giratórios; moldura do foco de plano parece elevada (verificar).
- Raio desenhado como fita plana, não tubo 3D.

### Ferramentas / armaduras / acessórios
- HUD do medidor de recarga não portado (o método era vazio no original).

### Campeões
- As cores que alguns modificadores davam ao mob (glColor antes de renderizar: flamejante, sinistro, venenoso,
  vampírico) não são reproduzidas; as partículas e a camada de fibras do taint sim.
- A lista de mobs que podem ser campeões é fixa em `ChampionModifier.WHITELIST` (o original aceitava IMC de outros mods).

### Construtos / mobs
- Perfuradora arcana sem o PacketFXBoreDig (o fluxo de partículas do bloco escavado); só o feixe.
- Gancho arcano em primeira pessoa usa o modelo sem transformação de mão (igual ao 1.12, fica grande na tela).

### Livro
- Ícones das páginas de multibloco usam o item resultante (o original desenhava a estrutura em miniatura).
- Ids que o JSON de pesquisa do 1.12 citava sem receita (`arcane_stone`, `arcane_brick`, `nitorcolor`,
  `jarlabelessence`) apontam para as receitas óbvias (`RecipeCatalog.ALIAS`).

### Dispositivos
- Efeito do espelho usa o shader do portal do End.
- Dispositivos vistos no cliente só em parte (smoke test e capturas); falta um teste visual completo.

### Geral
- `README.md` desatualizado de propósito (pedido do usuário para não mexer).
- Teste de fumaça do cliente: `./gradlew runClient -PtcQuickPlay=tc_test -PtcSmoke`, mundo `run/saves/tc_test`; o
  jogador deve começar em (-6.5, -60, 0.5) olhando a parede de ouro; um teste que trava deixa o jogador teleportado
  (corrigir o level.dat).
- Fechamento: teste visual completo no cliente, teste com o modpack (Create, Aeronautics, MineColonies) e jar final.
- Limite prático: ~3 builds Gradle simultâneos (22 GB de RAM).
