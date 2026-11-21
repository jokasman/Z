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

Há três níveis de dificuldade. O adversário controlado pelo computador escolhe o que produz conforme o teu exército, defende as fábricas, ocupa veículos abandonados, disputa as bandeiras e ataca o forte quando junta tropas suficientes.

Os gráficos e os sons são todos gerados por código, sem ficheiros de imagem nem de áudio.

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
  MapData.java              mapa de 9 setores, simétrico
  PathFinder.java           A* com alisamento do caminho
  Ai.java                   adversário controlado pelo computador
  Draw.java                 arte (terreno, edifícios, unidades, efeitos)
  Game.java                 ecrãs, câmara, toque e interface
  SoundSynth.java           síntese dos efeitos sonoros (WAV)
src/pt/setorzero/android/   atividade, superfície de desenho, Canvas e SoundPool
test/SimTest.java           testes da lógica, incluindo partidas computador contra computador
test/Shots.java             capturas de ecrã no computador (Java2D)
```

Testes (só precisam de Java):

```bash
mkdir -p build/test
javac -encoding UTF-8 -d build/test src/pt/setorzero/core/*.java test/*.java
java -cp build/test pt.setorzero.core.SimTest
java -Djava.awt.headless=true -cp build/test pt.setorzero.core.Shots build/shots
```
