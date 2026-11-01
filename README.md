# Hunter x Hunter Mod

vai tomando

## Como funciona o Nen

1. **Despertar:** agache parado no chão para meditar. Depois de 60 segundos sua aura desperta.
2. **Painel do Nen:** aperte **N** (dá para trocar em Opções > Controles > Hunter x Hunter) para ver seu tipo, nível, aura e treino.
3. **Técnicas:** ative pelo painel. Cada segundo ativo conta como treino.
   - **Ten:** reduz o dano recebido. 300s de treino liberam o Zetsu.
   - **Zetsu:** esconde sua presença e recupera aura mais rápido. 300s liberam o Ren.
   - **Ren:** aumenta o dano, mas gasta aura.
4. **Vidência (Copo d'Água com Folha):** com o Ren liberado, segure o botão direito com o copo. Cada sessão gasta aura e avança a leitura até revelar seu tipo de Nen.

Comandos: `/nen` mostra seu status, `/nen reset` (operador) recomeça do zero.

Os números de balanceamento ficam em `src/main/java/com/hxhmod/nen/NenRules.java`.

## Compilar

Precisa do Java 25.

```bash
./gradlew build
```

O `.jar` sai em `build/libs/`. O comando `./gradlew installMod` compila e copia direto para a pasta `mods` da instância.

## Aviso

Projeto de fã, sem fins lucrativos. Hunter x Hunter pertence a Yoshihiro Togashi e Shueisha. Todas as texturas do mod são originais.
