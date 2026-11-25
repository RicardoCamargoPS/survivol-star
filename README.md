# Survivol Star

Projeto inicial do jogo Survivol Star em Java 21, Maven e LWJGL 3. A abertura apresenta
o estúdio Mamutes Games e depois mostra um menu inicial com navegação por teclado ou mouse.

## Executar

Requisitos: JDK 21 e Maven.

```powershell
mvn clean compile exec:java
```

O Maven usa os binários nativos do Windows por padrão. Para executar em Linux ou macOS,
substitua o classificador nativo, por exemplo:

```powershell
mvn -Dlwjgl.natives=natives-linux clean compile exec:java
```

## Imagem da tela de título

A tela de título já carrega uma imagem de fundo opcional do recurso
`src/main/resources/images/title-background.png`. Enquanto esse arquivo não existir,
o fundo permanece preto. Adicione uma imagem PNG nesse caminho para usá-la como fundo.

## Controles

- Setas para cima/baixo: navegar pelo menu.
- Enter: selecionar a opção.
- Mouse: passar sobre uma opção e clicar.
- Esc: no título, fecha o jogo; na partida, pausa.

## Partida

Por enquanto a partida é um protótipo: um cubo girando sobre um campo de estrelas, com o
tempo de jogo e a dificuldade no canto. A velocidade do giro depende da dificuldade.

## Pausa

Esc durante a partida (ou a janela perder o foco) congela o jogo e abre o painel de pausa:

- **Continuar** (ou Esc) volta à partida.
- **Salvar jogo** abre a lista de slots; um slot ocupado pede confirmação antes de ser
  sobrescrito.
- **Configurações** abre as configurações e volta para a pausa ao sair.
- **Menu principal** e **Sair do jogo** perguntam antes, pois o progresso não salvo se perde.

## Carregar jogo

Um painel com a lista dos 5 slots de jogo salvo, um por linha, com a data à direita. Slots
vazios ficam em cinza e são pulados. Abaixo da lista aparece o progresso do slot selecionado
(dificuldade, tempo de jogo e resumo).

- Enter ou clique carregam o jogo e continuam a partida; Delete ou botão direito apagam
  (pedindo confirmação); Esc volta à tela de título.
- Os saves ficam em `~/.survivolstar/saves/slot1.properties` a `slot5.properties`.

## Configurações

Mesmo modelo do Racing Stellar: as seções (Jogo, Vídeo, Áudio, Controles, Sobre) ficam numa
lista à esquerda e as opções da seção sob o cursor num painel à direita.

- Enter ou seta para a direita entram no painel; Esc volta à lista; Esc na lista (ou
  "Voltar") retorna à tela de título.
- Esquerda/direita ajustam a opção; o mouse seleciona, clica nas setas e a roda ajusta.
- **Jogo**: dificuldade. **Vídeo**: tela cheia, resolução, VSync e contador de FPS, aplicados
  na hora. **Áudio**: volumes geral, de música e de efeitos (salvos para quando houver som).
- Cada alteração é gravada em `~/.survivolstar/settings.properties`.
