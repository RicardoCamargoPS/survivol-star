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
- Esc: fechar o jogo.

As opções de novo jogo e carregar jogo exibem uma mensagem de preparação; as respectivas
telas e funcionalidades de jogo ainda não foram implementadas.

## Configurações

Mesmo modelo do Racing Stellar: as seções (Jogo, Vídeo, Áudio, Controles, Sobre) ficam numa
lista à esquerda e as opções da seção sob o cursor num painel à direita.

- Enter ou seta para a direita entram no painel; Esc volta à lista; Esc na lista (ou
  "Voltar") retorna à tela de título.
- Esquerda/direita ajustam a opção; o mouse seleciona, clica nas setas e a roda ajusta.
- **Jogo**: dificuldade. **Vídeo**: tela cheia, resolução, VSync e contador de FPS, aplicados
  na hora. **Áudio**: volumes geral, de música e de efeitos (salvos para quando houver som).
- Cada alteração é gravada em `~/.survivolstar/settings.properties`.
