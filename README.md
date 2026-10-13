# Thaumcraft 6 → NeoForge 1.21.1 (port pessoal)

Port não oficial, só para estudo/uso pessoal. Thaumcraft é de Azanor ("All Rights Reserved") — não redistribuir.

## Pastas
- `mod/` — projeto NeoForge 1.21.1 (o port)
- `reference/` — código do 1.12.2 decompilado e com nomes legíveis (MCP), para consulta
- `decompiled/` — decompilação crua + assets originais
- `tools/` — scripts (deobf.py, convert_aspects.py, gen_marco2.py, make_structure.py, texgen/), logs, decompilador e fontes do 1.21.1 (`src121/`)

## Comandos (dentro de `mod/`, com `export JAVA_HOME=$(mise where java@temurin-21)`)
- `./gradlew build` — gera `build/libs/thaumcraft-*.jar`
- `./gradlew runClient` — abre o jogo com o mod
- `./gradlew runGameTestServer` — roda os 15 testes automatizados do servidor
- `./gradlew runClient -PtcQuickPlay=tc_test -PtcSmoke` — teste de fumaça do cliente (screenshots em `run/screenshots`)
- `./gradlew runServer` — servidor; grava `run/thaumcraft-aspects.txt` com os aspectos de todos os itens
- `./gradlew runSurvey` — gera um mundo novo em `run-survey/` e conta o que o worldgen do mod colocou (perto do spawn e na floresta mais próxima)
- `python3 tools/gen_marco2.py` — regera os JSON do Marco 2 (blockstates, modelos, loot, receitas, tags, worldgen, aspectos); edite o script, não os arquivos gerados

## Marco 1 (pronto)
- 37 aspectos com ícones e cores originais
- Aspectos de itens/entidades em JSON de datapack (`data/<mod>/thaumcraft/aspects/{items,entities}/*.json`),
  convertidos dos 1.12 (`tools/convert_aspects.py`) + `vanilla_1_21.json` para conteúdo novo
- Cálculo automático por receitas (crafting, como o original; + smithing e fornalha como adição),
  poções, encantamentos e bônus de armas/ferramentas/armaduras
- Thaumômetro (modelo 3D original) escaneia blocos, entidades, itens no chão e baús
- Conhecimento do jogador (pesquisas "!item" e pontos de observação por categoria), salvo e sincronizado
- Tooltip com aspectos (Shift), tradução pt_br
- Comandos: `/thaumcraft aspects`, `/thaumcraft knowledge`, `/thaumcraft knowledge reset`

## Marco 2 (pronto)
- Minérios: âmbar, cinábrio e quartzo (+ variantes de ardósia de cinábrio e quartzo, novidade do port: no 1.21 a maior parte cai abaixo de y=0)
- Cristais de vis dos 6 primais + vitium: modelo OBJ original, cor do aspecto, crescem de qualquer face de rocha, quebram sem apoio
  e dropam tamanho+1 cristais de vis (item com componente `thaumcraft:essentia`, um por aspecto no criativo)
- Greatwood e Silverwood: troncos, folhas (Greatwood com cor de folhagem do bioma), mudas (Greatwood só cresce 2x2), tábuas, escadas, lajes;
  geradores portados do original (Greatwood com ninho de aranhas 1 em 8 no worldgen; Silverwood com folhas-cintilantes em volta)
- Plantas: folha-cintilante, pérola-de-brasa e viscogumelo (náusea), com partículas
- Materiais: âmbar, mercúrio, lingotes/pepitas de taumium, metal do vazio e latão, gotas de mercúrio, lascas de quartzo,
  aglomerados nativos (ferro, ouro, cobre, cinábrio, quartzo); blocos de metal e de âmbar
- Receitas do original (compactação, tábuas/escadas/lajes, folha-cintilante → mercúrio, pérola-de-brasa → pó de blaze) + fornalha/alto-forno
- Worldgen por datapack (`data/thaumcraft/worldgen` + `neoforge/biome_modifier`); afinidade de bioma via tags `c:` (BiomeHandler)
- Aspectos dos blocos/itens novos; arquivos de aspecto aceitam `"priority"` (o de Thaumcraft é aplicado depois do vanilla)

Pendências conscientes (dependem de marcos futuros):
curiosidades no drop de âmbar (pesquisa), aspectos de lingotes (receitas do Crucible), biomas Magical Forest/Eerie, montes e portais de cultistas.

## Marco 3 (pronto) — Aura
- Cada chunk tem base/vis/fluxo (attachment salvo com o chunk); a base vem dos biomas do chunk e vizinhos (BiomeHandler)
- Simulação do AuraThread a cada segundo no servidor: vis e fluxo se espalham para chunks vizinhos, vis regenera conforme a fase da lua,
  vis estagnado vira fluxo (o gatilho de fenda de fluxo é calculado; a entidade vem no marco de fluxo/taint)
- API: getVis/getFlux/getAuraBase/addVis/addFlux/drainVis/drainFlux/shouldPreserveAura (`AuraHandler`)
- Taumômetro na mão sincroniza a aura e mostra o medidor original no HUD (Shift mostra os números)
- Cristais crescem/espalham com vis (vitium com fluxo) e encolhem devolvendo vis; folhas de Silverwood naturais repõem vis
- `/thaumcraft aura` e `/thaumcraft aura set vis|flux|base <valor>`

## Marco 4 (pronto) — Pesquisa
- Pesquisas, categorias e estágios do original em JSON de datapack (`data/<mod>/thaumcraft/research/*.json`, `tools/convert_research.py`),
  sincronizadas ao cliente; estágios exigem itens/crafts/conhecimento/pesquisas, adendos, flags (novo/atualizado/popup)
- Thaumonomicon: obtido com sal mundus numa estante (após o sonho "!gotdream"); navegador de categorias com fundo e
  linhas de dependência, páginas com texto formatado, receitas (crafting/fornalha) e botão de completar estágio; versão "cheat" opcional
- Ganho de conhecimento (escaneamento, curiosidades, notas celestes, marcos como pegar fogo, levar flecha, dormir…) com HUD
- Mesa de pesquisa (ferramentas de escrita numa mesa de madeira) e teoria completa: inspiração, cartas (todas as do original),
  auxílios ao redor (estantes, etc.), tinta/papel; completar a teoria dá pontos de teoria com a penalidade de 2/3 do original
- Distorção (warp) permanente/temporária guardada no jogador
- `/thaumcraft research list | <jogador> list|all|reset|<chave>|revoke <chave>`, `/thaumcraft warp <jogador> add|set <n> [PERM|TEMP]`

## Marco 5 (pronto) — Bancada Arcana
- Sal mundus numa bancada de trabalho (com FIRSTSTEPS@1) cria a Bancada Arcana; o Carregador de Bancada em cima dela
  faz a bancada puxar vis dos 3x3 chunks em volta
- Receitas arcanas (`thaumcraft:arcane_shaped` / `arcane_shapeless`): grade 3x3 + cristais nos 6 encaixes + vis da aura + pesquisa;
  sem receita arcana a grade funciona como bancada comum. Desconto de vis por equipamento (`IVisDiscountGear`) já previsto
- `tools/convert_recipes.py` interpreta o `ConfigRecipes.java` original (variáveis, laços de cores/aspectos, ore dictionary → tags `c:`)
  e gera as receitas em JSON; cada uma tem condição `neoforge:item_exists` e passa a valer sozinha quando os itens forem portados
  (hoje carregam as que usam itens existentes, ex.: Taumômetro). Crucible/infusão já são convertidos, mas só são ativados nos seus marcos
- Thaumonomicon mostra receitas arcanas (grade, cristais e custo de vis); receitas arcanas entram no cálculo de aspectos

## Próximos marcos sugeridos
Arcane Workbench → Crucible/Alquimia → Essentia (jarras, tubos) → Infusão → Focos/Cajados → Ferramentas/Armaduras → Golems → Fluxo/Taint → Eldritch
