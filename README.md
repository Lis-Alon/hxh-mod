# Hunter x Hunter Mod

Fan mod de Hunter x Hunter para Minecraft **26.3** (Fabric).

![Painel do Nen - aba Nível](docs/painel-nivel.png)

## Como funciona o Nen

1. **Despertar:** fique parado no chão agachado (ou com o painel do Nen aberto) para meditar. Depois de 60 segundos sua aura desperta e você aprende os quatro princípios: **Ten, Zetsu, Ren e Hatsu**.
2. **Painel do Nen:** aperte **N** (dá para trocar em Opções > Controles > Hunter x Hunter).
3. **Nível de Nen (1 a 100):** cada segundo com uma técnica ativa dá XP de Nen. Cada nível dá 2 pontos para as funcionalidades, e a cada 10 níveis você ganha +3 de bônus.
4. **Atributos e pontos:** os **atributos** (vitalidade, capacidade e fluxo de aura, ataque, proteção, agilidade e percepção) não recebem pontos: sobem sozinhos com o treino (veja [Atributos](#atributos)). Os pontos servem só para as **funcionalidades** de cada técnica. As técnicas não custam pontos: são aprendidas pela proficiência (veja abaixo). Os atributos e os bônus das técnicas são **status internos do personagem**: não mexem na armadura, na vida máxima nem em outros atributos do Minecraft (a vida extra aparece normalmente na barra de vida da HUD).
5. **Vidência (Copo d'Água com Folha):** com o Ren, coloque o copo no chão e clique nele com a mão vazia: o personagem senta e se concentra por 5 segundos (não ande nem se afaste). Também dá para segurar o botão direito com o copo na mão. Cada sessão gasta aura e avança a leitura até revelar seu tipo de Nen.
6. **Meditação:** com o painel aberto, meditando para despertar ou concentrado no copo, o personagem senta de pernas cruzadas.

Todos os itens do mod ficam na aba **Hunter x Hunter** do inventário do criativo.

| Meditação | Copo no chão | Aba do criativo |
|---|---|---|
| ![Pose de meditação](docs/meditacao.png) | ![Copo d'água com folha](docs/copo-no-chao.png) | ![Aba Hunter x Hunter](docs/aba-criativo.png) |

### HUD de combate

No canto superior esquerdo, no lugar dos corações e da armadura do Minecraft (a comida e o ar continuam onde sempre estiveram):

- **Retrato:** o rosto da skin, com a cor do seu tipo de Nen em volta (branco enquanto o tipo não foi revelado). Embaixo ficam o nível de Nen e a barra de XP.
- **Vida:** vai do vermelho ao amarelo conforme enche, com o número em cima. Ao apanhar, ela cai na hora e um rastro amarelo fica para trás, espera 0,45 s e esvazia devagar; outro golpe nesse meio-tempo segura o rastro de novo. Fica verde com veneno e cinza com wither, e a vida extra da absorção aparece como uma faixa dourada em cima.
- **Nen:** a barra azul elétrica logo abaixo, com a aura atual e a máxima. Só aparece depois do despertar.
- **Armadura:** um escudo com o número ao lado da barra de Nen, só quando você usa alguma.
- **Técnicas:** ícones redondos de 14 pixels colados embaixo da barra de Nen, só das técnicas já aprendidas. A técnica ligada acende e ganha um brilho pulsando na cor dela; a desligada fica apagada.

A carga do Ko, a postura do Ryu e as recargas do Gyo continuam em texto embaixo da HUD. Antes do despertar sobram só o retrato e a vida, e no modo criativo a HUD só aparece para quem já despertou.

### O painel

| Aba | O que tem |
|---|---|
| **Nível** | Nível, pontos, XP para o próximo nível, nível e barra de progresso de cada atributo, o que treina o atributo selecionado e status base |
| **Aura** | Escolhe as duas cores da aura: 12 cores prontas ou ajuste de matiz, saturação e brilho, com o personagem mostrando o resultado. |
| **Técnicas** | Não é uma aba: abre pelo botão Técnicas da Árvore ou clicando numa técnica, e Tab volta para a Árvore. Mostra cada técnica, com o que ela é, o que faz no jogo, proficiência ou requisitos, botão de ativar e funcionalidades para desbloquear |
| **Árvore** | Ocupa a tela toda: emblema do Nen, técnicas e sub-habilidades ligadas pelos requisitos, com a proficiência de cada técnica (nas que faltam aprender, quanto dos requisitos já foi cumprido). Passe o mouse para ver os detalhes, clique numa sub-habilidade para comprar e numa técnica para abrir. Tem "Mostrar nomes" e "Resetar", que devolve os pontos das sub-habilidades compradas |
| **Status** | Registro do treino: tipo, vidência, XP total, pontos e tempo treinado em cada técnica |

Mouse: clique seleciona e aperta os botões, a rodinha troca o atributo selecionado e clique duplo numa técnica liga/desliga.

Teclado: ↑↓ seleciona, ←→ ajusta (Shift = 5 por vez), Enter confirma, Tab troca de aba.

![Painel do Nen - aba Técnicas](docs/painel-tecnicas.png)

![Painel do Nen - aba Árvore](docs/painel-arvore.png)

### Atributos

Cada atributo tem a própria XP e vai do nível 0 ao 100 (nível = raiz de XP ÷ 5; o 100 pede 50 000 XP, e 1 XP vale mais ou menos 1 segundo de treino simples). Ao subir de nível aparece um aviso na barra de ação, como "★ Seu Fluxo de Aura aumentou pelo treinamento!".

| Atributo | Como treinar | XP | No nível 100 |
|---|---|---|---|
| **Vitalidade** | Ten ligado e recuperar vida | 1/s com Ten, 3 por ponto de vida recuperado | +80 de vida máxima e recuperação do Corpo Jovem e do Zetsu 2x mais rápida |
| **Capacidade de Aura** | Recuperar aura em Zetsu e esgotar a aura com Ren ou Ko | 25 por barra inteira recuperada em Zetsu, 60 ao esgotar (uma vez por minuto) | +500 de aura máxima |
| **Fluxo de Aura** | Lutar com a aura correndo, trocar rápido entre Zetsu, Ten e Ren, Fluidez da Aura | 1 a 2/s em combate (sobe em 1 minuto sem interromper), 3 por troca em até 3 s (uma a cada 2 s), 0,25/s com Ten e Fluidez da Aura | +20 de aura/s e técnicas gastam 30% menos |
| **Poder de Ataque** | Acertar golpes reforçados por Ren, Ko, Shu ou super soco | 0,6 por ponto de dano (Ko 0,9), até 30 por golpe | +10 de dano, +100% de dano físico e +50% de impacto no Ko e no Ren |
| **Proteção** | Absorver golpes com Ten, Ken ou super defesa, bloquear com escudo com o Nen ligado | 4 por ponto absorvido, 2 por ponto bloqueado, até 30 por golpe | Redução de dano do Ten e do Ken +75% (o piso de 20% do dano continua valendo) |
| **Agilidade** | Super pulo, super dash, mover-se, atacar e trocar de postura com o Ryu | 8 por pulo ou dash, 0,75/s correndo com Ryu, 1,5 por golpe com Ryu, 2 por troca de postura (uma a cada 2 s) | +45% de movimento e +25% de velocidade de ataque |
| **Percepção** | En aberto rastreando criaturas, achar alvos ocultos com En ou Gyo | 1/s com En, +0,1 por criatura (até +1), 1,5/s por alvo oculto (até 3) | Alcance do En 2x e 40% menos aura por bloco |

**Alvo oculto** é uma criatura invisível ou um jogador em Zetsu ou In. O Gyo enxerga até o raio de revelação, em linha reta; o En, em toda a esfera.

**Combate e descanso:** bater ou apanhar de uma criatura conta como combate por 8 segundos e multiplica a XP por 1,5, subindo até 2 em 30 segundos de luta; contra outro jogador já vale 2. Sem combate, ficar a menos de 2 blocos do mesmo lugar sem mexer em blocos nem criaturas não perde nada no primeiro minuto e depois cai até 0,2x em mais 3 minutos. Recuperar aura em Zetsu e recuperar vida são treinos parados de propósito, então não perdem XP por isso. Bonecos de armadura não contam, e o dano além da vida do alvo também não.

**Mundos antigos:** cada atributo começa no nível equivalente aos pontos que já tinha (a mesma fração do limite antigo), e esses pontos voltam a ficar livres para as funcionalidades. A antiga Velocidade de Ataque agora faz parte da Agilidade.

### Técnicas

Várias técnicas podem ficar ligadas ao mesmo tempo (o gasto de aura soma). A exceção é o **Zetsu**: ligar o Zetsu desliga as outras, e ligar qualquer outra encerra o Zetsu.

Cada técnica tem sua tecla de atalho (dá para trocar em Opções > Controles > Hunter x Hunter):

| Ten | Zetsu | Ren | Hatsu | Ken | Gyo | In | Ko | Ryu | Shu | En |
|---|---|---|---|---|---|---|---|---|---|---|
| Z | V | R | U | K | G | I | B | Y | H | J |

**Proficiência (0% a 100%):** cada técnica tem a sua, que sobe com o tempo em que ela fica ligada (mais rápido usando do jeito certo: Ten levando golpes, Zetsu parado). Os quatro princípios vêm ao despertar; as outras técnicas são aprendidas **sozinhas e de graça** quando a proficiência nas técnicas base chega no mínimo pedido.

Os quatro princípios e as aplicações avançadas, seguindo a [wiki de Hunter x Hunter](https://hunter-x-hunter.fandom.com/pt/wiki/Nen):

| Técnica | Aprende com | Efeito no jogo |
|---|---|---|
| **Ten** (Envoltura) | despertar | Reduz o dano de 10% a 25% conforme a proficiência |
| **Zetsu** (Corte) | despertar | Mobs percebem menos você, aura volta mais rápido (mais ainda parado), mas sem defesa de Nen |
| **Ren** (Prática) | despertar | Três intensidades: +15/25/40% de dano e -3/5/8% de dano recebido, gastando 2/3/5 de aura por segundo |
| **Hatsu** (Liberação) | despertar | Treina a aura do seu tipo. Eficiência de Aura é a passiva geral. Quem revelou o Reforço tem também duas passivas exclusivas; as dos outros tipos seguem em pausa, como no documento |
| **Gyo** (Coagular) | Ten 20% | A tecla liga o Gyo; golpe, botão direito, pulo e Ctrl viram super soco, super defesa, super pulo e super dash, com recarga (veja abaixo) |
| **In** (Ocultar) | Zetsu 50% | Esconde dos outros jogadores a aura das suas técnicas. Quem usa Gyo por perto ainda vê; liberar além da capacidade deixa parte à mostra |
| **En** (Território) | Ten 40% + Ren 40% | Detecta toda criatura numa esfera de 5 (1%) até 20 blocos de diâmetro (100%), através de paredes e mesmo invisível; só você vê o contorno. Dá para escolher uma área menor, que gasta menos |
| **Shu** (Ciclo) | Ten 50% | Reveste o objeto na mão, com benefício conforme o que ele é (veja abaixo) |
| **Ken** (Fortificar) | Ten 60% + Ren 60% | Três intensidades: -20/30/40% de dano recebido, gastando 2/3/5 de aura por segundo. Não soma com Ten e Ren: vale a maior defesa |
| **Ko** (Endurecimento) | Gyo, Ten, Zetsu, Ren e Hatsu 70% | Segure a tecla para carregar um golpe de +100% a +300% (veja abaixo) |
| **Ryu** (Fluxo) | Gyo 40% + Ken 40% | Liga junto com o Ken e move a aura dele entre ataque e defesa (veja abaixo) |

![En detectando um porco invisível](docs/en-invisivel.png)

### Intensidade

Ren, Ken e Shu têm três intensidades, e o En tem quatro tamanhos de área. Aperte a tecla da técnica **agachado** para trocar, ou use o botão **Trocar** na página da técnica no painel.

### Aura visível

A aura é uma chama lenta em volta do que a técnica está reforçando. As cores são escolhidas na aba **Aura** do painel, e todos os jogadores veem a sua escolha:

- **Ten:** um halo macio que acompanha o corpo, os braços e, de leve, o objeto segurado. Em primeira pessoa, os dois braços ficam à mostra, com poucos fios de aura e bem menos partículas do que os outros jogadores veem, para não atrapalhar a visão.
- **Ren e Ken:** chamas nas duas cores, atrás e em volta do corpo, com um anel girando no antebraço. As línguas sobem mais a cada intensidade do Ren; na máxima, passam bem acima da cabeça.
- **Shu:** só o objeto segurado brilha na cor principal, envolto na chama.
- **Ko:** toda a aura vai para uma esfera branca e colorida no punho, que cresce com a carga e ilumina o chão. O braço principal vai à frente e o resto do corpo fica sem aura.
- **Gyo:** em primeira pessoa, chamas fracas só na beirada da tela e o que estava escondido ganha contorno; visto de fora, uma chama pequena em volta da cabeça.
- **En:** uma esfera translúcida do tamanho da área de detecção, com labaredas perto do chão. Quem você detecta com Gyo ou En também ganha uma chama fina em volta.
- **Ryu:** a aura muda de lugar com a postura. No ataque, enfraquece no corpo e cresce nos braços e no objeto; na defesa, é o contrário.

O domínio da técnica muda a aparência:

- **Domínio de 0 a 25:** aura fina, instável e colada ao corpo, com poucas partículas.
- **Domínio de 26 a 75:** aura densa e constante, com contorno bem definido.
- **Domínio de 76 a 100:** mais volume, partículas mais numerosas e rápidas, centro mais transparente e chama mais larga. No En, a superfície passa a respirar devagar.

O tamanho acompanha cada ponto de proficiência: com 100%, a aura tem cerca de 2,9 vezes a largura e 2,2 vezes a altura que tem com 0%. No En, o tamanho é a própria área de detecção.

A opacidade também sobe com a intensidade liberada.

Em Zetsu não aparece nada. Com In, os outros jogadores não veem a aura, a não ser que estejam com Gyo preparado, a até 16 blocos e na linha de visão. Se você liberar mais aura por segundo do que o In consegue esconder (3 a 10, conforme a proficiência), o que passar fica à mostra, mais fraco.

### Ko

Segure a tecla (**B**) para concentrar a aura no punho; uma barra mostra a carga, que enche em 3 segundos. Atacar libera de +100% a +300% de dano e gasta de 10 a 40 de aura, conforme a carga, mesmo se o golpe for no ar. Minerar não conta como erro. Soltar a tecla antes de atacar cancela. Enquanto carrega, você perde a defesa de Nen (Ten, Ren e Ken) e recebe +50% de dano.

### Ryu

A tecla (**Y**) liga o Ryu junto com o Ken, e desligar o Ken desliga o Ryu. O dano recebido e causado muda com o lugar da aura:

| Ação | Aura no ataque | Dano causado | Dano recebido |
|---|---|---|---|
| Sem atacar nem defender | 50% | normal | normal |
| Atacar (fica por 1,5 s depois do clique) | 70% | +30% | +30% |
| Segurar o botão direito | 30% | -30% | -30% |

A aura leva 6 ticks para chegar ao destino, então trocar tarde deixa uma janela vulnerável. Mandar a aura para um lado gasta 3 de aura; voltar ao equilíbrio é de graça.

### Shu

A tecla (**H**) reveste o objeto da mão principal, ou o escudo da outra mão se a principal estiver vazia. O revestimento acompanha o que estiver na mão: trocar de arma ou ferramenta não desliga o Shu, e o objeto novo recebe o benefício na hora. De mão vazia ele fica ligado e gastando aura, só que sem efeito; só não dá para ligar com a mão vazia. Ferramentas e armas são reconhecidas pelo que fazem, então a regra vale para espada, machado, picareta, pá, enxada, tesoura, arco, besta, tridente, maça, vara de pesca e ferramentas de outros mods.

| Objeto | Benefício por intensidade (1/2/3) |
|---|---|
| Qualquer ferramenta ou arma | +15/25/40% de dano, quebra blocos 30/60/100% mais rápido, ataca 5/10/15% mais rápido e gasta 30/40/50% menos durabilidade |
| Escudo | -10/15/20% de dano recebido, além do bloqueio normal, e 30/40/50% menos desgaste |
| Outros objetos | +8/12/16% de dano |

A proficiência multiplica esses valores, de metade com 0% até o dobro com 100%. Na intensidade 3 com 100%, uma picareta dá +80% de dano, quebra 200% mais rápido, ataca 30% mais rápido e gasta 85% menos durabilidade (o limite). Flechas de um arco ou besta revestidos também recebem o bônus de dano; um tridente arremessado sai da mão e não leva o bônus. Usar o objeto, como minerar ou atirar, também treina o Shu.

### Aplicações do Gyo

Aperte a tecla do Gyo (**G**) para ligar: gasta 1 de aura por segundo e você vê o contorno de quem está invisível por perto (até 16 blocos, na linha de visão). Só você vê esse contorno. Cada ação abaixo gasta 10 de aura e usa a aplicação, e o Gyo continua ligado até você apertar **G** de novo. Andar, olhar em volta e minerar não usam aplicação.

Cada aplicação tem a própria recarga, de 20 s com proficiência 0% até 5 s com 100%. O tempo restante aparece no canto da tela. Durante a recarga, a ação funciona normalmente e não gasta aura.

| Ação | Aplicação | Efeito (proficiência 0% → 100%) |
|---|---|---|
| Acertar um golpe | Super Soco | +50% → +100% de dano |
| Botão direito | Super Defesa | -40% → -60% de dano recebido por 1,5 s, com 25% menos dano causado. Com a defesa pronta, o botão direito não usa itens nem blocos |
| Pular | Super Pulo | Impulso bem maior, só a partir do chão; a queda desse pulo não machuca |
| Ctrl | Super Dash | Arrancada para onde você está andando, ou para onde olha se estiver parado; funciona no ar |

Depois do soco, do pulo ou do dash, o resto do corpo fica com menos aura: você recebe +25% de dano por 1 segundo.

### Sub-habilidades

Compradas com pontos na aba Árvore, com os nomes do documento de design. "Em breve" aparece na árvore, mas ainda não pode ser comprada.

| Técnica | Sub-habilidades |
|---|---|
| **Ten** | Fluidez da Aura, Corpo Jovem |
| **Zetsu** | Supressão Refinada, Descanso Profundo, Passos Silenciosos (passos sem som) |
| **Ren** | Controle de Liberação (menos gasto), Potência Ofensiva (mais dano) |
| **Hatsu** | Eficiência de Aura (-5% no gasto de aura). Só para o Reforço já revelado: Vitalidade Reforçada (+10% a +30% de vida máxima) e Impacto Natural (+10% a +30% de dano corpo a corpo), conforme o domínio do Hatsu e sem precisar ligá-lo. Os outros tipos: em breve |
| **Gyo** | Concentração Precisa (aplicações 25% a 50% mais fortes), Transferência Eficiente (custam 25% a 50% menos aura) |
| **In** | Controle de Ocultação (menos gasto), Ocultação Sustentada (esconde mais aura) |
| **En** | Expansão Territorial (esfera maior), Controle Territorial (menos gasto) |
| **Ken** | Fortificação Duradoura (menos gasto), Densidade Protetora (mais defesa) |
| **Shu** | Revestimento Eficiente (menos gasto), Reforço Estrutural (mais benefício e menos desgaste) |
| **Ko** | Concentração Absoluta (golpe mais forte), Preparação Refinada (carga mais rápida) |
| **Ryu** | Transferência Rápida (troca mais rápida), Fluxo Eficiente (troca mais barata) |

### Comandos

`/nen` mostra seu status. Os outros precisam de operador (no mundo single player, ative "Permitir comandos"). Aperte Tab para ver os nomes das técnicas e funcionalidades.

| Comando | O que faz |
|---|---|
| `/nen unlock <técnica\|funcionalidade\|all>` | Libera na hora, mesmo sem a proficiência (junto com as técnicas base) |
| `/nen lock <técnica\|funcionalidade\|all>` | Bloqueia de novo e não deixa ser aprendida sozinha até usar o unlock; funcionalidades pagas devolvem os pontos |
| `/nen level <1-100>` | Vai direto para o nível |
| `/nen mastery <técnica\|all> <0-100>` | Define a proficiência da técnica ou de todas, e zera a recarga do Gyo quando ele é afetado. As técnicas que a nova proficiência libera são aprendidas na hora; baixar não bloqueia o que já foi aprendido |
| `/nen xp <quantidade>` | Dá XP de Nen |
| `/nen points add <quantidade>` | Pontos extras para gastar no painel (negativo tira) |
| `/nen attribute <atributo\|all> <0-100>` | Define o nível do atributo ou de todos, para testar (`vitality`, `aura_capacity`, `aura_flow`, `attack_power`, `protection`, `agility`, `perception`) |
| `/nen awaken [tipo]` | Desperta na hora; com o tipo, troca o tipo de Nen |
| `/nen reveal` | Revela o tipo sem o copo |
| `/nen aura` | Enche a aura |
| `/nen reset` | Apaga todo o Nen e recomeça do zero |

Exemplos: `/nen unlock ken`, `/nen lock zetsu`, `/nen unlock ryu_master`, `/nen awaken emission`, `/nen mastery gyo 50`, `/nen mastery all 100`.

Os números de balanceamento ficam em `src/main/java/com/hxhmod/nen/NenRules.java`, `NenStat.java`, `Technique.java` e `NenUpgrade.java`.

## Compilar

Precisa do Java 25.

```bash
./gradlew build
```

O `.jar` sai em `build/libs/`. O comando `./gradlew installMod` compila e copia direto para a pasta `mods` da instância.

`./gradlew runClientGameTest` abre o jogo sozinho, testa o painel e salva prints em `build/run/clientGameTest/screenshots`.

## Aviso

Projeto de fã, sem fins lucrativos. Hunter x Hunter pertence a Yoshihiro Togashi e Shueisha. Todas as texturas do mod são originais.

## Fonte

As texturas da árvore são geradas por `tools/gen_tree_textures.py` e depois `tools/gen_tree_art.py`, que usa as silhuetas de `tools/art/tree_reference.png` e desenha os kanjis com a Yu Gothic do Windows. As peças da HUD são geradas por `tools/gen_hud_art.py`. As imagens de títulos, abas e botões são geradas por `tools/gen_panel_art.py` e a página de Nível também usa a [Cinzel](https://github.com/NDISCOVER/Cinzel), sob a SIL Open Font License 1.1. A licença vai junto em `assets/hxh/font/cinzel_ofl.txt`.
