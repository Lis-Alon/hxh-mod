# Hunter x Hunter Mod

Fan mod de Hunter x Hunter para Minecraft **26.3** (Fabric).

![Painel do Nen - aba Nível](docs/painel-nivel.png)

## Como funciona o Nen

1. **Despertar:** fique parado no chão agachado (ou com o painel do Nen aberto) para meditar. Depois de 60 segundos sua aura desperta e o Ten é liberado.
2. **Painel do Nen:** aperte **N** (dá para trocar em Opções > Controles > Hunter x Hunter).
3. **Proficiência (nível 1 a 100):** cada segundo com uma técnica ativa dá XP de Nen. Cada nível dá 2 pontos, e a cada 10 níveis você ganha +3 de bônus.
4. **Pontos:** gaste em **atributos** (vida, dano, velocidade de ataque, proteção, agilidade, capacidade e fluxo de aura) ou para **desbloquear técnicas** e as **funcionalidades** de cada uma.
5. **Vidência (Copo d'Água com Folha):** com o Ren liberado, coloque o copo no chão e clique nele com a mão vazia: o personagem senta e se concentra por 5 segundos (não ande nem se afaste). Também dá para segurar o botão direito com o copo na mão. Cada sessão gasta aura e avança a leitura até revelar seu tipo de Nen.
6. **Meditação:** com o painel aberto, meditando para despertar ou concentrado no copo, o personagem senta de pernas cruzadas.

Todos os itens do mod ficam na aba **Hunter x Hunter** do inventário do criativo.

| Meditação | Copo no chão | Aba do criativo |
|---|---|---|
| ![Pose de meditação](docs/meditacao.png) | ![Copo d'água com folha](docs/copo-no-chao.png) | ![Aba Hunter x Hunter](docs/aba-criativo.png) |

### O painel

| Aba | O que tem |
|---|---|
| **Nível** | Nível, pontos, XP para o próximo nível, distribuição de atributos (atual → novo) e status base |
| **Técnicas** | Uma aba para cada técnica, com o que ela é, o que faz no jogo, requisitos, botão de ativar e funcionalidades para desbloquear |
| **Status** | Registro do treino: tipo, vidência, XP total, pontos e tempo treinado em cada técnica |

Mouse: clique seleciona e aperta os botões, a rodinha em cima de um atributo põe pontos (Shift = 5), o botão direito tira 1 ponto e clique duplo numa técnica desbloqueia ou liga/desliga.

Teclado: ↑↓ seleciona, ←→ ajusta (Shift = 5 por vez), Enter confirma, Tab troca de aba.

![Painel do Nen - aba Técnicas](docs/painel-tecnicas.png)

### Técnicas

Os quatro princípios e as aplicações avançadas, seguindo a [wiki de Hunter x Hunter](https://hunter-x-hunter.fandom.com/pt/wiki/Nen):

| Técnica | Vem de | Efeito no jogo |
|---|---|---|
| **Ten** (Envoltura) | princípio | Reduz o dano de 10% a 25% conforme o domínio |
| **Ken** (Fortificar) | Ten + Ren | Resistência II |
| **Zetsu** (Corte) | Ten | Mobs percebem menos você, aura volta mais rápido (mais ainda parado), mas sem defesa de Nen |
| **In** (Ocultar) | Zetsu + Ren | Invisibilidade e Força I |
| **Ren** (Prática) | Ten | Força I |
| **Gyo** (Coagular) | Ren | Visão noturna e revela invisíveis |
| **Ko** (Endurecimento) | Gyo + Zetsu | Força III, mas fica lento |
| **Ryu** (Fluxo) | Ko + Ken | Força II, Resistência II e Velocidade I |
| **Shu** (Ciclo) | Ren | Pressa II |
| **En** (Território) | Ten + Ren | Revela criaturas ao redor |
| **Hatsu** (Liberação) | Ren + tipo revelado | Habilidade do seu tipo (em breve) |

### Comandos

`/nen` mostra seu status. Os outros precisam de operador (no mundo single player, ative "Permitir comandos"). Aperte Tab para ver os nomes das técnicas e funcionalidades.

| Comando | O que faz |
|---|---|
| `/nen unlock <técnica\|funcionalidade\|all>` | Libera de graça (junto com o que vem antes) |
| `/nen lock <técnica\|funcionalidade\|all>` | Bloqueia de novo; se você tinha pago, os pontos voltam |
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
