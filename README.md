# Hunter x Hunter Mod

Fan mod de Hunter x Hunter para Minecraft **26.3** (Fabric).

![Painel do Nen - aba Nível](docs/painel-nivel.png)

## Como funciona o Nen

1. **Despertar:** agache parado no chão para meditar. Depois de 60 segundos sua aura desperta e o Ten é liberado.
2. **Painel do Nen:** aperte **N** (dá para trocar em Opções > Controles > Hunter x Hunter).
3. **Proficiência (nível 1 a 100):** cada segundo com uma técnica ativa dá XP de Nen. Cada nível dá 2 pontos, e a cada 10 níveis você ganha +3 de bônus.
4. **Pontos:** gaste em **atributos** (vida, dano, velocidade de ataque, proteção, agilidade, capacidade e fluxo de aura) ou para **desbloquear técnicas** e as **funcionalidades** de cada uma.
5. **Vidência (Copo d'Água com Folha):** com o Ren liberado, segure o botão direito com o copo. Cada sessão gasta aura e avança a leitura até revelar seu tipo de Nen.

### O painel

| Aba | O que tem |
|---|---|
| **Nível** | Nível, pontos, XP para o próximo nível, distribuição de atributos (atual → novo) e status base |
| **Técnicas** | Uma aba para cada técnica, com o que ela é, o que faz no jogo, requisitos, botão de ativar e funcionalidades para desbloquear |
| **Status** | Registro do treino: tipo, vidência, XP total, pontos e tempo treinado em cada técnica |

Teclado: ↑↓ seleciona, ←→ ajusta (Shift = 5 por vez), Enter confirma, Tab troca de aba.

![Painel do Nen - aba Técnicas](docs/painel-tecnicas.png)

### Técnicas

Os quatro princípios e as aplicações avançadas, seguindo a [wiki de Hunter x Hunter](https://hunter-x-hunter.fandom.com/pt/wiki/Nen):

| Técnica | Vem de | Efeito no jogo |
|---|---|---|
| **Ten** (Envoltura) | princípio | Resistência I |
| **Ken** (Fortificar) | Ten + Ren | Resistência II |
| **Zetsu** (Corte) | Ten | Invisibilidade e aura 3x mais rápida |
| **In** (Ocultar) | Zetsu + Ren | Invisibilidade e Força I |
| **Ren** (Prática) | Ten | Força I |
| **Gyo** (Coagular) | Ren | Visão noturna e revela invisíveis |
| **Ko** (Endurecimento) | Gyo + Zetsu | Força III, mas fica lento |
| **Ryu** (Fluxo) | Ko + Ken | Força II, Resistência II e Velocidade I |
| **Shu** (Ciclo) | Ren | Pressa II |
| **En** (Território) | Ten + Ren | Revela criaturas ao redor |
| **Hatsu** (Liberação) | Ren + tipo revelado | Habilidade do seu tipo (em breve) |

### Comandos

- `/nen` mostra seu status.
- `/nen reset` (operador) recomeça do zero.
- `/nen xp <quantidade>` (operador) dá XP de Nen, útil para testar.

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
