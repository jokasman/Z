# Setor Zero

Jogo de estratégia em tempo real para Android, em português. Não há recursos para recolher: o mapa está dividido em setores, e quem toca na bandeira de um setor fica com ele, com as fábricas e os canhões que lá estão. Quantos mais setores tiveres, mais depressa as fábricas produzem. Ganha quem destruir o forte inimigo.

Requer Android 8.0 ou superior. Joga-se com o telemóvel deitado e não tem anúncios nem ligação à internet.

## Como se joga

- **Objetivo**: destruir o forte ciano, no canto superior direito, antes que o inimigo destrua o teu, no canto inferior esquerdo.
- **Bandeiras**: basta uma unidade (robô ou veículo) tocar na bandeira para capturar o setor. Se houver unidades das duas equipas na bandeira, ninguém a captura. A bandeira de cada base só pode ser tomada depois de o forte dessa base cair.
- **Fábricas**: produzem sem parar o tipo de unidade escolhido. A velocidade depende do número de setores (×0,8 com 1 setor, ×1,0 com 3, ×1,5 com 8). Fábricas de robôs: Batedor, Atirador e Bazuca. Fábricas de veículos: Jipe e Tanque.
- **Veículos abandonados**: estão espalhados pelo mapa. Um robô que entre num fica a conduzi-lo. Quando um veículo é destruído, o condutor pode sair com vida.
- **Canhões**: pertencem a quem tem o setor e disparam sozinhos. Podem ser destruídos.

### Controlos

| Gesto | Ação |
|---|---|
| Arrastar um dedo | Caixa de seleção |
| Tocar numa unidade tua | Selecioná-la (dois toques: todas do mesmo tipo no ecrã) |
| Tocar no chão | Mover as unidades selecionadas |
| Tocar num inimigo, no forte ou num canhão | Atacar |
| Tocar num veículo abandonado | O robô selecionado mais próximo entra nele |
| Tocar numa fábrica tua | Escolher o que fabrica |
| Tocar numa fábrica que não é tua | Mandar as unidades capturar a bandeira desse setor |
| Dois dedos | Mover a câmara e aproximar ou afastar |
| Minimapa | Saltar para esse ponto do mapa |
| Botão "voltar" | Pausa |

### Unidades

| Unidade | Papel |
|---|---|
| Batedor | Rápido e barato, bom em grupo |
| Atirador | Alcance longo, muito eficaz contra robôs |
| Bazuca | Rockets contra veículos e edifícios |
| Jipe | O mais rápido, ideal para capturar bandeiras longe |
| Tanque | Lento, muito resistente, canhão com dano em área |

### Campanha

São 11 níveis, todos disponíveis desde o início, com três dificuldades. Cada nível vencido ganha uma estrela por dificuldade.

| # | Nível | Cenário | Particularidade |
|---|---|---|---|
| 1 | Vale das Bandeiras | Prados | Mapa de introdução |
| 2 | Dunas de Ferro | Deserto | Mapa aberto, tanques abandonados |
| 3 | Rio Partido | Prados | Rio com três pontes |
| 4 | Passo Gelado | Neve | Montanhas com poucas passagens |
| 5 | Arquipélago | Prados | Ilhas ligadas por pontes estreitas |
| 6 | Cratera | Vulcão | Rios de lava e uma cratera central |
| 7 | Cidade Fantasma | Cidade | Ruas entre ruínas e um canal |
| 8 | Floresta Negra | Prados | Floresta cerrada com trilhos |
| 9 | Mar de Areia | Deserto | Mapa grande, inimigo com fábrica extra |
| 10 | Fortaleza Gelada | Neve | Base inimiga cheia de canhões |
| 11 | Coração do Vulcão | Vulcão | Batalha final, inimigo reforçado |

O adversário controlado pelo computador escolhe o que produz conforme o teu exército, defende as fábricas, ocupa veículos abandonados, disputa as bandeiras e ataca o forte quando junta tropas suficientes.

### 2 jogadores

Todos os níveis podem ser jogados por duas pessoas, cada uma no seu telemóvel, na mesma rede Wi-Fi ou com um telemóvel ligado ao ponto de acesso (hotspot) do outro. Não é preciso internet.

1. No primeiro telemóvel: **2 JOGADORES › CRIAR JOGO** e escolher o mapa. O ecrã mostra o endereço do telemóvel.
2. No segundo: **2 JOGADORES › ENTRAR NUM JOGO**. O jogo aparece na lista ao fim de um ou dois segundos. Se não aparecer, toca em **ESCREVER ENDEREÇO** e escreve o endereço mostrado no primeiro telemóvel.

Quem cria o jogo joga com o laranja (em baixo à esquerda) e quem entra joga com o ciano (em cima à direita). Não há reforços nem bónus de nenhum dos lados. Alguns mapas (Mar de Areia, Fortaleza Gelada e Coração do Vulcão) têm edifícios a mais do lado ciano. O telemóvel que criou o jogo faz a simulação e envia o estado ao outro cerca de 15 vezes por segundo. Se esse telemóvel for para segundo plano, o jogo para nos dois. Se a ligação cair, os dois veem "LIGAÇÃO PERDIDA".

### Aspeto e som

O jogo tem o aspeto dos jogos de estratégia dos anos 90: é desenhado em baixa resolução (cerca de 270 píxeis de altura) e ampliado sem suavizar. Tem letra pixelizada, painéis metálicos em relevo, sprites em 16 direções e um efeito opcional de monitor CRT. Os sons são de 8 bits e há duas músicas chiptune, uma para os menus e outra para a batalha. Som, música e efeito CRT ligam-se e desligam-se no ecrã inicial.

Os gráficos, os sons e a música são todos gerados por código, sem ficheiros de imagem nem de áudio.

## Obter o APK

**Descarregar:** https://github.com/jokasman/Z/releases/latest/download/SetorZero.apk

Cada envio (push) para o GitHub corre o workflow **Compilar APK** (`.github/workflows/build.yml`). O workflow testa a lógica do jogo, compila o APK e, no ramo `main`, publica-o na Release da versão atual (`versionName` do `AndroidManifest.xml`). O link acima aponta sempre para a Release mais recente e funciona sem conta no GitHub. O APK também fica como artefacto `SetorZero-apk` na página da execução, em **Actions**, mas aí só se descarrega com sessão iniciada no GitHub.

Assinatura:

- Se os Repository secrets `KEYSTORE_BASE64` (o ficheiro `.jks` em Base64) e `KS_PASS` existirem, o APK é assinado sempre com essa chave e cada versão instala-se por cima da anterior.
- Se não existirem, o APK é assinado com uma chave temporária, diferente em cada compilação. Para instalar uma versão nova é preciso desinstalar a anterior. Como o jogo não guarda dados, não se perde nada.

Para criar a chave e o secret:

```bash
keytool -genkeypair -keystore release.jks -alias setorzero -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=Setor Zero, C=PT"
base64 -w0 release.jks   # copiar o resultado para o secret KEYSTORE_BASE64
```

Para compilar localmente, com o Android SDK em `ANDROID_HOME`: `KS_PASS=... bash build.sh`. O APK fica em `build/SetorZero.apk`.

## Estrutura

```
src/pt/setorzero/core/      lógica e desenho, sem dependências do Android
  World.java                simulação: movimento, combate, captura, produção
  Level.java                os 11 níveis da campanha
  MapData.java              ferramentas para construir mapas simétricos
  TerrainArt.java           terreno em pixel art, com 5 cenários
  PixelFont.java            letra pixelizada com acentos
  PathFinder.java           A* com alisamento do caminho
  Ai.java                   adversário controlado pelo computador
  Draw.java                 arte (terreno, edifícios, unidades, efeitos)
  Game.java                 ecrãs, câmara, toque e interface
  SoundSynth.java           síntese dos efeitos sonoros e da música (WAV)
  Net.java                  ligação na rede local (procura por UDP, jogo por TCP)
  Versus.java               jogo a dois: imagens de estado e ordens
src/pt/setorzero/android/   atividade, superfície de desenho, Canvas e SoundPool
test/SimTest.java           testes da lógica, incluindo partidas computador contra computador
test/NetTest.java           dois jogos ligados pela rede no mesmo computador
test/Shots.java             capturas de ecrã no computador (Java2D)
```

Testes (só precisam de Java):

```bash
mkdir -p build/test
javac -encoding UTF-8 -d build/test src/pt/setorzero/core/*.java test/*.java
java -cp build/test pt.setorzero.core.SimTest
java -Djava.awt.headless=true -cp build/test pt.setorzero.core.NetTest
java -Djava.awt.headless=true -cp build/test pt.setorzero.core.Shots build/shots
```
