# Hunter x Hunter Mod

Fan mod de Hunter x Hunter para Minecraft **26.3** (Fabric).

![Painel do Nen - aba Nível](docs/painel-nivel.png)

## Como funciona o Nen

1. **Despertar:** fique parado no chão agachado (ou com o painel do Nen aberto) para meditar. Depois de 60 segundos sua aura desperta e você aprende os quatro princípios: **Ten, Zetsu, Ren e Hatsu**.
2. **Painel do Nen:** aperte **N** (dá para trocar em Opções > Controles > Hunter x Hunter).
3. **Nível de Nen (1 a 100):** cada segundo com uma técnica ativa dá XP de Nen. Cada nível dá 2 pontos, e a cada 10 níveis você ganha +3 de bônus.
4. **Pontos:** gaste em **atributos** (vida, dano, velocidade de ataque, proteção, agilidade, capacidade e fluxo de aura) ou nas **funcionalidades** de cada técnica. As técnicas não custam pontos: são aprendidas pela proficiência (veja abaixo). Os atributos e os bônus das técnicas são **status internos do personagem**: não mexem na armadura, na vida máxima nem em outros atributos do Minecraft (os corações extras aparecem normalmente na barra de vida).
5. **Vidência (Copo d'Água com Folha):** com o Ren, coloque o copo no chão e clique nele com a mão vazia: o personagem senta e se concentra por 5 segundos (não ande nem se afaste). Também dá para segurar o botão direito com o copo na mão. Cada sessão gasta aura e avança a leitura até revelar seu tipo de Nen.
6. **Meditação:** com o painel aberto, meditando para despertar ou concentrado no copo, o personagem senta de pernas cruzadas.

Todos os itens do mod ficam na aba **Hunter x Hunter** do inventário do criativo.

| Meditação | Copo no chão | Aba do criativo |
|---|---|---|
| ![Pose de meditação](docs/meditacao.png) | ![Copo d'água com folha](docs/copo-no-chao.png) | ![Aba Hunter x Hunter](docs/aba-criativo.png) |

### O painel

| Aba | O que tem |
|---|---|
| **Nível** | Nível, pontos, XP para o próximo nível, distribuição de atributos (atual → novo) e status base |
| **Aura** | Escolhe as duas cores da aura: 12 cores prontas ou ajuste de matiz, saturação e brilho, com o personagem mostrando o resultado. |
| **Técnicas** | Não é uma aba: abre pelo botão Técnicas da Árvore ou clicando numa técnica, e Tab volta para a Árvore. Mostra cada técnica, com o que ela é, o que faz no jogo, proficiência ou requisitos, botão de ativar e funcionalidades para desbloquear |
| **Árvore** | Ocupa a tela toda: emblema do Nen, técnicas e sub-habilidades ligadas pelos requisitos, com a proficiência de cada técnica (nas que faltam aprender, quanto dos requisitos já foi cumprido). Passe o mouse para ver os detalhes, clique numa sub-habilidade para comprar e numa técnica para abrir. Tem "Mostrar nomes" e "Resetar", que devolve os pontos das sub-habilidades compradas |
| **Status** | Registro do treino: tipo, vidência, XP total, pontos e tempo treinado em cada técnica |

Mouse: clique seleciona e aperta os botões, a rodinha em cima de um atributo põe pontos (Shift = 5), o botão direito tira 1 ponto e clique duplo numa técnica liga/desliga.

Teclado: ↑↓ seleciona, ←→ ajusta (Shift = 5 por vez), Enter confirma, Tab troca de aba.

![Painel do Nen - aba Técnicas](docs/painel-tecnicas.png)

![Painel do Nen - aba Árvore](docs/painel-arvore.png)

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
| **Gyo** (Coagular) | Ten 20% | A tecla prepara o Gyo; a próxima ação vira super soco, super defesa, super pulo ou super dash (veja abaixo) |
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

A aura aparece em faixas de energia, com quadradinhos de luz subindo. As cores são escolhidas na aba **Aura** do painel, e todos os jogadores veem a sua escolha:

- **Ten, Ren na intensidade 1 e outras técnicas:** faixas suaves em volta do corpo e dos braços. Em primeira pessoa, os dois braços ficam à mostra enquanto a aura está ligada.
- **Ren na intensidade 2 e Ken:** chamas em volta do corpo.
- **Ren na intensidade máxima e Ko:** chamas maiores e mais altas.
- **En:** uma esfera translúcida do tamanho da área de detecção, com labaredas perto do chão.
- **Shu:** o objeto segurado brilha na cor principal e fica envolto pelas faixas.

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

A tecla (**H**) reveste o objeto da mão principal, ou o escudo da outra mão se a principal estiver vazia. Guardar, soltar ou trocar o objeto desfaz o revestimento. Todo objeto revestido desgasta 30/40/50% menos, conforme a intensidade.

| Objeto | Benefício por intensidade |
|---|---|
| Espadas, machados, tridente e maça | +15/25/40% de dano (machados também quebram blocos mais rápido) |
| Picaretas, pás e enxadas | Quebram blocos 30/60/100% mais rápido |
| Escudo | -10/15/20% de dano recebido, além do bloqueio normal |
| Outros objetos | +8/12/16% de dano |

### Aplicações do Gyo

Aperte a tecla do Gyo (**G**) para preparar: gasta 1 de aura por segundo e você vê o contorno de quem está invisível por perto (até 16 blocos, na linha de visão). Só você vê esse contorno. A próxima ação gasta 10 de aura, usa a aplicação e termina a preparação; apertar **G** de novo cancela. Andar, olhar em volta e minerar não gastam a preparação.

| Ação | Aplicação | Efeito (proficiência 0% → 100%) |
|---|---|---|
| Acertar um golpe | Super Soco | +50% → +100% de dano |
| Botão direito | Super Defesa | -40% → -60% de dano recebido por 1,5 s, com 25% menos dano causado. Enquanto prepara, o botão direito não usa itens nem blocos |
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
| `/nen xp <quantidade>` | Dá XP de Nen |
| `/nen points add <quantidade>` | Pontos extras para gastar no painel (negativo tira) |
| `/nen stats reset` | Zera os atributos e devolve os pontos |
| `/nen awaken [tipo]` | Desperta na hora; com o tipo, troca o tipo de Nen |
| `/nen reveal` | Revela o tipo sem o copo |
| `/nen aura` | Enche a aura |
| `/nen reset` | Apaga todo o Nen e recomeça do zero |

Exemplos: `/nen unlock ken`, `/nen lock zetsu`, `/nen unlock ryu_master`, `/nen awaken emission`.

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

As texturas da árvore são geradas por `tools/gen_tree_textures.py` e depois `tools/gen_tree_art.py`, que usa as silhuetas de `tools/art/tree_reference.png` e desenha os kanjis com a Yu Gothic do Windows. As imagens de títulos, abas e botões são geradas por `tools/gen_panel_art.py` e a página de Nível também usa a [Cinzel](https://github.com/NDISCOVER/Cinzel), sob a SIL Open Font License 1.1. A licença vai junto em `assets/hxh/font/cinzel_ofl.txt`.
